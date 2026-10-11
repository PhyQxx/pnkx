package com.pnkx.life.service.webarticle;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 网址收藏纯函数工具：URL 归一化/去重键、链接提取、AI 输出解析、RSS 条目解析
 * <p>
 * 抽为无状态工具类便于单元测试（PxWebArticleServiceImpl / PxWebArticleTaskService /
 * WebClipHandler / PxWebRssFeedServiceImpl 共用）。
 *
 * @author PHY
 */
public final class WebArticleUtils {

    private WebArticleUtils() {
    }

    /**
     * 归一化时剔除的跟踪参数（utm_* 前缀单独判断）
     */
    private static final Set<String> TRACKING_PARAMS = Set.of(
            "spm", "fbclid", "gclid", "ysclid", "srsltid", "mc_cid", "mc_eid",
            "igshid", "_hsenc", "_hsmi", "share_token", "scene", "from");

    /**
     * URL 归一化后需去除的尾部标点（对话文本中链接常以句读结尾）
     */
    private static final String TRAILING_PUNCTUATION = ".,;:!?\"'”』」》）)]}】。，、";

    /**
     * 从自然语言中提取链接：http(s):// 或 www. 开头，遇空白/引号/中文即止
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b((?:https?://|www\\.)[^\\s<>\"'\\u4e00-\\u9fff（）【】\\[\\]]+)");

    /**
     * URL 归一化：补协议、host 小写、去尾部标点、去 fragment 与跟踪参数。
     * 归一化结果作为收藏展示链接与去重基础。
     */
    public static String normalizeUrl(String raw) {
        String url = trimTrailingPunctuation(raw.trim());
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase();
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath() == null ? "" : uri.getPath();
            StringBuilder normalized = new StringBuilder(scheme).append("://").append(host).append(path);
            String query = uri.getQuery();
            if (query != null && !query.isEmpty()) {
                String kept = Arrays.stream(query.split("&"))
                        .filter(pair -> !isTrackingParam(pair))
                        .collect(Collectors.joining("&"));
                if (!kept.isEmpty()) {
                    normalized.append('?').append(kept);
                }
            }
            return normalized.toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    /**
     * URL 去重键（MD5），解决超过唯一索引前缀长度（255）的长链接碰撞问题
     */
    public static String urlHash(String url) {
        return SecureUtil.md5(url == null ? "" : url);
    }

    public static boolean isTrackingParam(String pair) {
        int eq = pair.indexOf('=');
        String key = (eq < 0 ? pair : pair.substring(0, eq)).toLowerCase();
        return key.startsWith("utm_") || TRACKING_PARAMS.contains(key);
    }

    public static String hostOf(String url) {
        try {
            String host = new URI(url).getHost();
            return host == null ? null : host.toLowerCase();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * 内网/保留地址判断（SSRF 防护，字面判断不做 DNS 解析）：
     * 环回、链路本地、私有网段、回送域名、云元数据地址。
     * 服务器抓取入口（收藏正文 / RSS 拉取）拒绝此类目标。
     */
    public static boolean isInternalUrl(String url) {
        String host = hostOf(url);
        if (host == null) {
            // 解析不出 host 的地址一律视为不可信
            return true;
        }
        if ("localhost".equals(host) || host.endsWith(".localhost") || host.endsWith(".local")
                || host.endsWith(".internal") || host.equals("metadata.google.internal")) {
            return true;
        }
        // 纯 IP 字面量按网段判断（IPv4）
        if (host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            String[] parts = host.split("\\.");
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            if (first == 0 || first == 10 || first == 127) {
                return true;
            }
            if (first == 169 && second == 254) {
                return true;
            }
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            if (first == 192 && second == 168) {
                return true;
            }
            if (first >= 224) {
                // 组播/保留段
                return true;
            }
        }
        // IPv6 环回/本地/唯一本地
        String bare = host.replace("[", "").replace("]", "");
        return bare.equals("::1") || bare.equals("::") || bare.toLowerCase().startsWith("fe80:")
                || bare.toLowerCase().startsWith("fc") || bare.toLowerCase().startsWith("fd");
    }

    /**
     * 从用户输入中提取第一个网址，www. 开头的自动补 https 协议；
     * 提取不到返回 null
     */
    public static String extractUrl(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = trimTrailingPunctuation(matcher.group(1).trim());
            if (url.startsWith("www.")) {
                url = "https://" + url;
            }
            return url;
        }
        return null;
    }

    /**
     * 从 AI 输出中容错提取 JSON（剥代码围栏、截取首尾大括号），失败返回 null
     */
    public static JSONObject extractJson(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String text = content.trim()
                .replaceAll("(?s)^```(json)?\\s*", "")
                .replaceAll("(?s)\\s*```$", "");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return JSON.parseObject(text.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 兼容 RSS 2.0（item > link 文本）与 Atom（entry > link[href]）解析条目链接
     */
    public static List<String> parseRssLinks(Document doc) {
        List<String> links = new ArrayList<>();
        for (Element item : doc.select("item")) {
            Element link = item.selectFirst("link");
            if (link != null && !link.text().isBlank()) {
                links.add(link.text().trim());
            }
        }
        if (links.isEmpty()) {
            for (Element entry : doc.select("entry")) {
                Element link = entry.selectFirst("link[href]");
                if (link != null && !link.attr("href").isBlank()) {
                    links.add(link.attr("href").trim());
                }
            }
        }
        return links;
    }

    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String trimTrailingPunctuation(String url) {
        String result = url;
        while (!result.isEmpty() && TRAILING_PUNCTUATION.indexOf(result.charAt(result.length() - 1)) >= 0) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
