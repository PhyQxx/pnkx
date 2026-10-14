package com.pnkx.life.service.webarticle;

import com.pnkx.life.service.workspace.ResourceTagService;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.spring.SpringUtils;
import com.pnkx.life.domain.po.PxWebArticle;
import com.pnkx.life.mapper.PxWebArticleMapper;
import com.pnkx.life.service.txt.PxBookHtmlText;
import cn.hutool.core.date.DateUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.Set;

/**
 * 网址收藏异步任务：网页抓取、正文提取、AI 分析
 * <p>
 * 两级线程池：抓取在 webClipExecutor（快，10s 超时），AI 分析经代理投递到
 * webArticleAiExecutor（单线程串行，LLM 有速率限额且单次耗时长，RSS 批量场景
 * 下既不挤占抓取线程，也不会并发轰炸模型 API）。同类内直接调用 analyzeArticle
 * 不走 @Async 代理，因此跨池投递必须取代理对象。
 *
 * @author PHY
 */
@Component
public class PxWebArticleTaskService {

    private static final Logger log = LoggerFactory.getLogger(PxWebArticleTaskService.class);

    /**
     * 抓取超时（毫秒）
     */
    private static final int FETCH_TIMEOUT_MS = 10_000;

    /**
     * 单页 HTML 大小上限（字符）
     */
    private static final int MAX_HTML_LENGTH = 2 * 1024 * 1024;

    /**
     * 正文纯文本上限（字符），超出截断
     */
    private static final int MAX_TEXT_LENGTH = 512 * 1024;

    /**
     * AI 分析输入的正文上限（字符），控制 token 成本；
     * 超出部分走分段摘要→汇总的长文流程，而非直接丢弃
     */
    private static final int AI_INPUT_LIMIT = 6000;

    /**
     * 长文分段分析的每段长度与最大段数（上限约 2 万字）
     */
    private static final int SEGMENT_SIZE = 5000;
    private static final int MAX_SEGMENTS = 4;

    /**
     * 正文最少长度，低于此值认为页面无可提取正文（如 SPA 壳页面）
     */
    private static final int MIN_TEXT_LENGTH = 100;

    /**
     * AI 分类白名单
     */
    private static final Set<String> CATEGORIES = Set.of("技术", "资讯", "工具", "生活", "学习", "其他");

    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final String AI_SYSTEM_PROMPT = "你是网页内容整理助手。分析用户提供的网页内容，"
            + "严格按以下 JSON 格式输出，不要输出 JSON 以外的任何内容：\n"
            + "{\"summary\":\"不超过200字的中文摘要\",\"tags\":[\"3到5个简短中文标签\"],\"category\":\"分类\"}\n"
            + "category 只能从以下选项中选择一个：技术、资讯、工具、生活、学习、其他。";

    /**
     * 干扰节点选择器：脚本样式 + 导航/页脚/侧栏/评论/推荐等非正文区块
     */
    private static final String JUNK_SELECTOR = "script,style,noscript,iframe,form,button,svg,nav,header,footer,aside,"
            + "ins,ad,[class~=nav],[class~=menu],[class~=sidebar],[class~=comment],[class~=share],"
            + "[class~=related],[class~=recommend],[class~=breadcrumb],[class~=copyright],[id~=comment]";

    /**
     * 清理白名单：基础排版 + 图片/链接；链接强制新窗口打开，避免站内阅读时点击跳走
     */
    private static final Safelist CONTENT_SAFELIST = Safelist.basicWithImages()
            .addEnforcedAttribute("a", "target", "_blank")
            .addEnforcedAttribute("a", "rel", "noopener");

    @Resource
    private PxWebArticleMapper pxWebArticleMapper;
    @Resource private ResourceTagService resourceTags;

    @Resource
    private AiClient aiClient;

    /**
     * 抓取 + AI 分析（入口，抓取池）
     */
    @Async("webClipExecutor")
    public void fetchAndAnalyze(Long id) {
        try {
            fetchArticle(id);
        } catch (Exception e) {
            log.error("网址抓取异常, id: {}", id, e);
        }
        // 经代理投递到 AI 单线程池，与抓取解耦
        if (isFetchSuccess(id)) {
            try {
                SpringUtils.getBean(PxWebArticleTaskService.class).analyzeAsync(id);
            } catch (Exception e) {
                log.error("网址AI分析投递异常, id: {}", id, e);
            }
        }
    }

    /**
     * 仅重新 AI 分析（AI 池）
     */
    @Async("webArticleAiExecutor")
    public void analyzeAsync(Long id) {
        try {
            analyzeArticle(id);
        } catch (Exception e) {
            log.error("网址AI分析异常, id: {}", id, e);
        }
    }

    /**
     * 抓取网页并提取正文/元数据，回写快照字段与 fetch_status
     */
    public void fetchArticle(Long id) {
        PxWebArticle article = pxWebArticleMapper.selectPxWebArticleById(id, null);
        if (article == null || article.getUrl() == null) {
            return;
        }
        PxWebArticle result = new PxWebArticle();
        result.setId(id);
        result.setUpdateTime(DateUtils.getNowDate());
        if (WebArticleUtils.isInternalUrl(article.getUrl())) {
            log.warn("网址抓取被拒绝（内网/保留地址）, id: {}, url: {}", id, article.getUrl());
            result.setFetchStatus("2");
            result.setFetchError("不允许抓取内网或保留地址");
            pxWebArticleMapper.updateFetchResult(result);
            return;
        }
        try {
            Document doc = Jsoup.connect(article.getUrl())
                    .userAgent(UA)
                    .referrer("https://www.google.com/")
                    .timeout(FETCH_TIMEOUT_MS)
                    .maxBodySize(MAX_HTML_LENGTH)
                    .followRedirects(true)
                    .ignoreContentType(true)
                    .get();

            Element content = extractContent(doc);
            // 链接与图片绝对化，避免快照中的相对地址失效
            content.select("a[href]").forEach(a -> {
                String abs = a.absUrl("href");
                if (!abs.isEmpty()) {
                    a.attr("href", abs);
                }
            });
            content.select("img[src]").forEach(img -> {
                String abs = img.absUrl("src");
                if (!abs.isEmpty()) {
                    img.attr("src", abs);
                }
            });
            String safeHtml = Jsoup.clean(content.html(), CONTENT_SAFELIST);
            String text = PxBookHtmlText.toPlainText(safeHtml);

            result.setDomain(WebArticleUtils.hostOf(article.getUrl()));
            result.setTitle(truncate(firstNonBlank(metaContent(doc, "meta[property=og:title]"),
                    metaContent(doc, "meta[name=twitter:title]"), doc.title()), 500));
            result.setAuthor(truncate(firstNonBlank(metaContent(doc, "meta[property=article:author]"),
                    metaContent(doc, "meta[name=author]")), 250));
            result.setPublishTime(parseDate(firstNonBlank(
                    metaContent(doc, "meta[property=article:published_time]"),
                    metaContent(doc, "meta[name=pubdate]"),
                    metaContent(doc, "meta[name=publishdate]"),
                    attr(doc.selectFirst("time[datetime]"), "datetime"))));
            result.setCoverImage(truncate(firstNonBlank(metaContent(doc, "meta[property=og:image]"),
                    metaContent(doc, "meta[name=twitter:image]"),
                    attr(content.selectFirst("img[src]"), "src")), 1000));
            result.setFavicon(truncate(firstNonBlank(attr(doc.selectFirst("link[rel~=icon]"), "abs:href"),
                    faviconGuess(article.getUrl())), 1000));
            result.setContentHtml(truncate(safeHtml, MAX_HTML_LENGTH));
            result.setContentText(truncate(text, MAX_TEXT_LENGTH));
            result.setFetchStatus("1");
            result.setFetchError(null);
        } catch (Exception e) {
            log.warn("网址抓取失败, id: {}, url: {}, 原因: {}", id, article.getUrl(), e.getMessage());
            result.setFetchStatus("2");
            result.setFetchError(truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), 1000));
        }
        pxWebArticleMapper.updateFetchResult(result);
    }

    /**
     * 调用 AI 生成摘要/标签/分类，回写 ai_status
     * <p>
     * prompt 携带时间戳 nonce：AiClient 对相同 prompt 有 1 小时缓存，
     * 不加 nonce 的话"重新分析"会命中缓存返回旧结果。
     */
    public void analyzeArticle(Long id) {
        PxWebArticle article = pxWebArticleMapper.selectPxWebArticleById(id, null);
        PxWebArticle result = new PxWebArticle();
        result.setId(id);
        result.setUpdateTime(DateUtils.getNowDate());
        if (article == null || !"1".equals(article.getFetchStatus())) {
            result.setAiStatus("2");
            result.setAiError("正文未就绪，无法分析");
            resourceTags.applyArticleAnalysis(result);
            return;
        }
        String text = article.getContentText() == null ? "" : article.getContentText().trim();
        if (text.length() < MIN_TEXT_LENGTH) {
            result.setAiStatus("2");
            result.setAiError("正文过短，无法有效分析");
            resourceTags.applyArticleAnalysis(result);
            return;
        }

        JSONObject parsed;
        String model;
        if (text.length() <= AI_INPUT_LIMIT) {
            JSONObject aiResult = aiClient.chat(AI_SYSTEM_PROMPT, buildAnalyzePrompt(article, text));
            parsed = WebArticleUtils.extractJson(aiResult == null ? null : aiResult.getString("content"));
            model = aiResult == null ? null : aiResult.getString("model");
        } else {
            // 长文：先分段摘要，再汇总生成最终结果
            parsed = analyzeLongArticle(article, text);
            model = parsed.getString("__model");
            parsed.remove("__model");
        }
        if (parsed == null) {
            result.setAiStatus("2");
            result.setAiError("AI 调用或输出解析失败");
            resourceTags.applyArticleAnalysis(result);
            return;
        }
        String category = parsed.getString("category");
        result.setSummary(truncate(parsed.getString("summary"), 2000));
        result.setTags(truncate(joinTags(parsed.getJSONArray("tags")), 500));
        result.setCategory(category != null && CATEGORIES.contains(category) ? category : "其他");
        result.setAiStatus("1");
        result.setAiError(null);
        result.setAiModel(model);
        resourceTags.applyArticleAnalysis(result);
        log.info("网址AI分析完成, id: {}, 模型: {}, 长文分段: {}", id, model, text.length() > AI_INPUT_LIMIT);
    }

    /**
     * 长文分段分析：正文按 {@link #SEGMENT_SIZE} 分段（最多 {@link #MAX_SEGMENTS} 段），
     * 每段生成简短摘要，再将分段摘要汇总为最终 摘要/标签/分类。
     * 单段失败跳过（不中断整体），全部段失败则整体失败。
     */
    private JSONObject analyzeLongArticle(PxWebArticle article, String text) {
        java.util.List<String> sectionSummaries = new java.util.ArrayList<>();
        for (int i = 0; i < MAX_SEGMENTS; i++) {
            int from = i * SEGMENT_SIZE;
            if (from >= text.length()) {
                break;
            }
            int to = Math.min(from + SEGMENT_SIZE, text.length());
            String sectionPrompt = "标题：" + safe(article.getTitle())
                    + "\n（第" + (i + 1) + "部分）用不超过150字概括以下内容的核心信息，直接输出概括，不要多余说明："
                    + "\n" + text.substring(from, to);
            JSONObject sectionResult = aiClient.chat("你是内容摘要助手，输出简明中文概括。", sectionPrompt);
            String summary = sectionResult == null ? null : sectionResult.getString("content");
            if (summary != null && !summary.isBlank()) {
                sectionSummaries.add("第" + (i + 1) + "部分：" + summary.trim());
            }
        }
        if (sectionSummaries.isEmpty()) {
            return null;
        }
        String tailNote = text.length() > MAX_SEGMENTS * SEGMENT_SIZE
                ? "\n（注：原文超过" + MAX_SEGMENTS * SEGMENT_SIZE + "字，以上为前" + MAX_SEGMENTS + "部分的概括）" : "";
        String mergePrompt = "标题：" + safe(article.getTitle())
                + "\n链接：" + article.getUrl()
                + "\n分析批次：" + System.currentTimeMillis()
                + "\n以下是这篇长文各部分的概括：\n" + String.join("\n", sectionSummaries) + tailNote;
        JSONObject aiResult = aiClient.chat(AI_SYSTEM_PROMPT, mergePrompt);
        JSONObject parsed = WebArticleUtils.extractJson(aiResult == null ? null : aiResult.getString("content"));
        if (parsed != null) {
            parsed.put("__model", aiResult == null ? null : aiResult.getString("model"));
        }
        return parsed;
    }

    /**
     * 构建分析 prompt。时间戳 nonce 用于绕过 AiClient 的 1 小时响应缓存，
     * 否则"重新分析"会命中缓存返回旧结果。
     */
    private String buildAnalyzePrompt(PxWebArticle article, String text) {
        return "标题：" + safe(article.getTitle())
                + "\n链接：" + article.getUrl()
                + "\n分析批次：" + System.currentTimeMillis()
                + "\n正文：" + text.substring(0, Math.min(text.length(), AI_INPUT_LIMIT));
    }

    private boolean isFetchSuccess(Long id) {
        PxWebArticle article = pxWebArticleMapper.selectPxWebArticleById(id, null);
        return article != null && "1".equals(article.getFetchStatus());
    }

    /**
     * 正文提取：article > main > 评分最高的容器（文本量 × 链接密度惩罚）> body
     * <p>
     * 链接文本占比高的容器（导航/列表页/聚合页）会被降权，
     * 避免把"文章列表"当成正文整体抓下来。
     */
    private Element extractContent(Document doc) {
        doc.select(JUNK_SELECTOR).remove();
        Element candidate = doc.selectFirst("article");
        if (candidate != null && candidate.text().length() >= MIN_TEXT_LENGTH) {
            return candidate;
        }
        candidate = doc.selectFirst("main");
        if (candidate != null && candidate.text().length() >= MIN_TEXT_LENGTH) {
            return candidate;
        }
        Element best = doc.body();
        double bestScore = -1;
        for (Element el : doc.select("div,section,article")) {
            double score = contentScore(el);
            if (score > bestScore) {
                bestScore = score;
                best = el;
            }
        }
        return best;
    }

    /**
     * 内容评分：文本长度 × (1 - 链接文本密度)，纯链接容器得分趋近 0
     */
    private double contentScore(Element el) {
        int textLength = el.text().length();
        if (textLength < MIN_TEXT_LENGTH) {
            return 0;
        }
        int linkLength = 0;
        for (Element a : el.select("a")) {
            linkLength += a.text().length();
        }
        double linkDensity = textLength == 0 ? 1 : (double) linkLength / textLength;
        return textLength * (1 - linkDensity);
    }

    private String joinTags(java.util.List<Object> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object tag : tags) {
            String value = tag == null ? "" : String.valueOf(tag).trim();
            if (value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(value);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String metaContent(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        return el == null ? null : el.attr("content");
    }

    private String attr(Element el, String attr) {
        return el == null || el.attr(attr) == null ? null : el.attr(attr).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Date parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DateUtil.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String faviconGuess(String url) {
        String host = WebArticleUtils.hostOf(url);
        return host == null ? null : "https://" + host + "/favicon.ico";
    }

    private String truncate(String value, int max) {
        return WebArticleUtils.truncate(value, max);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
