package com.pnkx.life.service.impl;

import com.pnkx.life.service.workspace.ResourceTagService;
import org.springframework.transaction.annotation.Transactional;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.life.domain.po.PxWebArticle;
import com.pnkx.life.mapper.PxWebArticleMapper;
import com.pnkx.life.service.IPxWebArticleService;
import com.pnkx.life.service.webarticle.PxWebArticleTaskService;
import com.pnkx.life.service.webarticle.WebArticleUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author PHY
 * @classname PxWebArticleServiceImpl
 * @description 网址收藏Service业务层处理
 * <p>
 * 网址收藏为用户私有数据（「我的应用」），所有读写均按 create_by 隔离。
 * 收藏归属必须显式传入（Web 请求取当前登录用户，RSS 定时任务取订阅创建者），
 * 不依赖 SecurityUtils 线程上下文——定时线程无登录态，取到的会是空字符串。
 */
@Service
public class PxWebArticleServiceImpl implements IPxWebArticleService {

    @Resource
    private PxWebArticleMapper pxWebArticleMapper;

    @Resource
    private com.pnkx.life.mapper.ResourceLinkMapper resourceLinkMapper;
    @Resource private ResourceTagService resourceTags;

    @Resource
    private PxWebArticleTaskService taskService;

    @Override
    public PxWebArticle selectPxWebArticleById(Long id) {
        return pxWebArticleMapper.selectPxWebArticleById(id, currentUserId());
    }

    @Override
    public List<PxWebArticle> selectPxWebArticleList(PxWebArticle pxWebArticle) {
        pxWebArticle.setCreateBy(currentUserId());
        return pxWebArticleMapper.selectPxWebArticleList(pxWebArticle);
    }

    @Override
    public PxWebArticle addArticleByUrl(String url, String remark) {
        return addArticleByUrl(url, remark, "1", currentUserId());
    }

    @Override
    public PxWebArticle addArticleByUrl(String url, String remark, String source) {
        return addArticleByUrl(url, remark, source, currentUserId());
    }

    @Override
    public PxWebArticle addArticleByUrl(String url, String remark, String source, String userId) {
        if (StringUtils.isEmpty(url)) {
            throw new ServiceException("链接不能为空");
        }
        if (StringUtils.isEmpty(userId)) {
            throw new ServiceException("收藏归属用户缺失（未登录或调用方未指定）");
        }
        String normalized = WebArticleUtils.normalizeUrl(url);
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            throw new ServiceException("链接格式不正确");
        }
        String urlHash = WebArticleUtils.urlHash(normalized);
        PxWebArticle existing = pxWebArticleMapper.selectPxWebArticleByUrl(urlHash, userId);
        if (existing != null) {
            // 之前抓取失败的文章再次收藏时自动重试
            if ("2".equals(existing.getFetchStatus())) {
                retryFetch(existing.getId());
            }
            return existing;
        }
        PxWebArticle article = new PxWebArticle();
        article.setUrl(normalized);
        article.setUrlHash(urlHash);
        article.setDomain(WebArticleUtils.hostOf(normalized));
        article.setRemark(remark);
        article.setFetchStatus("0");
        article.setAiStatus("0");
        article.setSource(source);
        article.setCreateBy(userId);
        article.setCreateTime(DateUtils.getNowDate());
        pxWebArticleMapper.insertPxWebArticle(article);
        taskService.fetchAndAnalyze(article.getId());
        return article;
    }

    @Override
    @Transactional
    public PxWebArticle updatePxWebArticle(PxWebArticle pxWebArticle) {
        pxWebArticle.setUpdateBy(currentUserId());
        pxWebArticle.setUpdateTime(DateUtils.getNowDate());
        int rows = pxWebArticleMapper.updatePxWebArticle(pxWebArticle);
        if (rows == 0) {
            throw new ServiceException("记录不存在或无权修改");
        }
        if (pxWebArticle.getTags() != null) resourceTags.syncLegacy(currentUserId(), "webArticle", pxWebArticle.getId(), pxWebArticle.getTags());
        return pxWebArticleMapper.selectPxWebArticleById(pxWebArticle.getId(), currentUserId());
    }

    @Override
    public int deletePxWebArticleByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return pxWebArticleMapper.deletePxWebArticleByIds(ids, currentUserId());
    }

    @Override
    public boolean updateReadFlag(Long id, String readFlag) {
        // 状态收一（§3.1）：已读状态落 px_resource_state.opened，不再写 px_web_article.read_flag
        resourceLinkMapper.openState(id, currentUserId(), "1".equals(readFlag));
        return true;
    }

    @Override
    public boolean updateStarFlag(Long id, String starFlag) {
        // 状态收一（§3.1）：收藏状态落 px_resource_state.starred，不再写 px_web_article.star_flag
        resourceLinkMapper.starState(id, currentUserId(), "1".equals(starFlag));
        return true;
    }

    @Override
    public boolean retryFetch(Long id) {
        boolean reset = pxWebArticleMapper.resetFetchStatus(id, currentUserId()) > 0;
        if (reset) {
            taskService.fetchAndAnalyze(id);
        }
        return reset;
    }

    @Override
    public boolean retryAnalyze(Long id) {
        boolean reset = pxWebArticleMapper.resetAiStatus(id, currentUserId()) > 0;
        if (reset) {
            taskService.analyzeAsync(id);
        }
        return reset;
    }

    @Override
    public List<String> selectTags() {
        List<String> tagStrings = pxWebArticleMapper.selectPxWebArticleTags(currentUserId());
        Set<String> tags = new LinkedHashSet<>();
        for (String tagString : tagStrings) {
            tags.addAll(Arrays.stream(tagString.split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList()));
        }
        return new ArrayList<>(tags);
    }

    @Override
    public String exportMarkdown() {
        List<PxWebArticle> articles = pxWebArticleMapper.selectPxWebArticleExport(currentUserId());
        StringBuilder md = new StringBuilder();
        md.append("# 网址收藏导出\n\n")
                .append("> 共 ").append(articles.size()).append(" 条，导出时间：")
                .append(DateUtils.dateTimeNow("yyyy-MM-dd HH:mm:ss")).append("\n\n");
        for (PxWebArticle article : articles) {
            md.append("## ").append(StringUtils.isNotEmpty(article.getTitle()) ? article.getTitle() : article.getUrl())
                    .append("\n\n");
            md.append("- 链接：").append(article.getUrl()).append('\n');
            if (StringUtils.isNotEmpty(article.getDomain())) {
                md.append("- 域名：").append(article.getDomain()).append('\n');
            }
            if (StringUtils.isNotEmpty(article.getAuthor())) {
                md.append("- 作者：").append(article.getAuthor()).append('\n');
            }
            if (article.getCategory() != null) {
                md.append("- 分类：").append(article.getCategory()).append('\n');
            }
            if (StringUtils.isNotEmpty(article.getTags())) {
                md.append("- 标签：").append(article.getTags().replace(",", "、")).append('\n');
            }
            if (article.getCreateTime() != null) {
                md.append("- 收藏时间：").append(DateUtils.parseDateToStr("yyyy-MM-dd", article.getCreateTime())).append('\n');
            }
            if (StringUtils.isNotEmpty(article.getSummary())) {
                md.append("\n**AI 摘要**：").append(article.getSummary()).append('\n');
            }
            if (StringUtils.isNotEmpty(article.getContentText())) {
                md.append("\n").append(article.getContentText()).append('\n');
            }
            md.append("\n---\n\n");
        }
        return md.toString();
    }

    @Override
    public Map<String, Object> selectStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("overview", pxWebArticleMapper.selectStatsOverview(currentUserId()));
        stats.put("categories", pxWebArticleMapper.selectCategoryStats(currentUserId()));
        stats.put("trend", pxWebArticleMapper.selectDailyTrend(currentUserId(), 30));
        return stats;
    }

    @Override
    public List<Map<String, Object>> selectTagStats() {
        // tags 为逗号分隔字段，分布统计在 Java 侧拆分聚合
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (String tagString : pxWebArticleMapper.selectPxWebArticleTags(currentUserId())) {
            for (String tag : tagString.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    counter.merge(trimmed, 1, Integer::sum);
                }
            }
        }
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("tag", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int renameTag(String oldTag, String newTag) {
        if (StringUtils.isEmpty(oldTag) || StringUtils.isEmpty(newTag)) {
            throw new ServiceException("标签名不能为空");
        }
        String trimmed = newTag.trim();
        int affected = 0;
        for (PxWebArticle row : pxWebArticleMapper.selectRowsByTag(currentUserId(), oldTag.trim())) {
            LinkedHashSet<String> tags = new LinkedHashSet<>();
            for (String tag : row.getTags().split(",")) {
                String current = tag.trim();
                if (current.isEmpty()) {
                    continue;
                }
                // 旧名替换为新名，与已有同名标签自然合并（Set 去重）
                tags.add(current.equals(oldTag.trim()) ? trimmed : current);
            }
            String joined = String.join(",", tags);
            if (!joined.equals(row.getTags())) {
                resourceTags.syncLegacy(currentUserId(), "webArticle", row.getId(), joined);
                affected++;
            }
        }
        return affected;
    }

    @Override
    @Transactional
    public int deleteTag(String tag) {
        if (StringUtils.isEmpty(tag)) {
            throw new ServiceException("标签名不能为空");
        }
        String target = tag.trim();
        int affected = 0;
        for (PxWebArticle row : pxWebArticleMapper.selectRowsByTag(currentUserId(), target)) {
            String joined = Arrays.stream(row.getTags().split(","))
                    .map(String::trim)
                    .filter(current -> !current.isEmpty() && !current.equals(target))
                    .collect(Collectors.joining(","));
            resourceTags.syncLegacy(currentUserId(), "webArticle", row.getId(), joined);
            affected++;
        }
        return affected;
    }

    private String currentUserId() {
        return SecurityUtils.getUserId();
    }
}
