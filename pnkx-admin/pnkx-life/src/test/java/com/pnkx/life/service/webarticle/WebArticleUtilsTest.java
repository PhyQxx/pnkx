package com.pnkx.life.service.webarticle;

import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 网址收藏纯函数工具单元测试
 */
class WebArticleUtilsTest {

    // ──────────── normalizeUrl ────────────

    @Test
    void normalizeUrlStripsTrackingParams() {
        String normalized = WebArticleUtils.normalizeUrl(
                "https://Example.com/a?utm_source=x&utm_medium=y&spm=1.2&id=42");
        assertEquals("https://example.com/a?id=42", normalized);
    }

    @Test
    void normalizeUrlAddsSchemeAndLowercasesHost() {
        assertEquals("https://www.test.com/path", WebArticleUtils.normalizeUrl("www.TEST.com/path"));
    }

    @Test
    void normalizeUrlTrimsTrailingPunctuation() {
        assertEquals("https://example.com/a", WebArticleUtils.normalizeUrl("https://example.com/a。"));
        assertEquals("https://example.com/a", WebArticleUtils.normalizeUrl("https://example.com/a."));
        assertEquals("https://example.com/a", WebArticleUtils.normalizeUrl("https://example.com/a，"));
    }

    @Test
    void normalizeUrlKeepsPathTrailingSlash() {
        assertEquals("https://example.com/a/", WebArticleUtils.normalizeUrl("https://example.com/a/"));
    }

    @Test
    void normalizeUrlDropsFragment() {
        assertEquals("https://example.com/a", WebArticleUtils.normalizeUrl("https://example.com/a#section"));
    }

    // ──────────── urlHash / hostOf ────────────

    @Test
    void urlHashIsStableMd5() {
        assertEquals(WebArticleUtils.urlHash("https://a.com"), WebArticleUtils.urlHash("https://a.com"));
        assertEquals(32, WebArticleUtils.urlHash("https://a.com").length());
        assertFalse(WebArticleUtils.urlHash("https://a.com").equals(WebArticleUtils.urlHash("https://b.com")));
    }

    @Test
    void hostOfExtractsLowercaseHost() {
        assertEquals("example.com", WebArticleUtils.hostOf("https://Example.COM/a"));
        assertNull(WebArticleUtils.hostOf("not a url"));
    }

    // ──────────── isInternalUrl（SSRF 防护）────────────

    @Test
    void internalUrlsAreRejected() {
        assertTrue(WebArticleUtils.isInternalUrl("http://localhost/admin"));
        assertTrue(WebArticleUtils.isInternalUrl("http://127.0.0.1:8080/actuator"));
        assertTrue(WebArticleUtils.isInternalUrl("http://10.1.2.3/x"));
        assertTrue(WebArticleUtils.isInternalUrl("http://192.168.1.1/x"));
        assertTrue(WebArticleUtils.isInternalUrl("http://172.16.0.1/x"));
        assertTrue(WebArticleUtils.isInternalUrl("http://172.31.255.255/x"));
        assertTrue(WebArticleUtils.isInternalUrl("http://169.254.169.254/latest/meta-data"));
        assertTrue(WebArticleUtils.isInternalUrl("http://[::1]/x"));
        assertTrue(WebArticleUtils.isInternalUrl("http://foo.internal/x"));
        assertTrue(WebArticleUtils.isInternalUrl("::::not a url"));
    }

    @Test
    void publicUrlsAreAllowed() {
        assertFalse(WebArticleUtils.isInternalUrl("https://example.com/a"));
        assertFalse(WebArticleUtils.isInternalUrl("https://172.32.0.1/a"));
        assertFalse(WebArticleUtils.isInternalUrl("http://8.8.8.8/dns"));
    }

    // ──────────── extractUrl（对话链接提取）────────────

    @Test
    void extractUrlFromChineseSentence() {
        assertEquals("https://example.com/a",
                WebArticleUtils.extractUrl("推荐看这篇 https://example.com/a 很不错"));
    }

    @Test
    void extractUrlAddsSchemeToWww() {
        assertEquals("https://www.example.com/a",
                WebArticleUtils.extractUrl("www.example.com/a"));
    }

    @Test
    void extractUrlTrimsTrailingPunctuation() {
        assertEquals("https://example.com/a",
                WebArticleUtils.extractUrl("看这个https://example.com/a。"));
    }

    @Test
    void extractUrlReturnsNullWhenAbsent() {
        assertNull(WebArticleUtils.extractUrl("今天天气不错"));
        assertNull(WebArticleUtils.extractUrl(null));
    }

    // ──────────── extractJson（AI 输出解析）────────────

    @Test
    void extractJsonFromFencedOutput() {
        JSONObject json = WebArticleUtils.extractJson("```json\n{\"summary\":\"摘要\",\"tags\":[\"a\"],\"category\":\"技术\"}\n```");
        assertNotNull(json);
        assertEquals("摘要", json.getString("summary"));
        assertEquals("技术", json.getString("category"));
    }

    @Test
    void extractJsonFromMixedText() {
        JSONObject json = WebArticleUtils.extractJson("好的，结果如下：{\"summary\":\"s\"} 以上。");
        assertNotNull(json);
        assertEquals("s", json.getString("summary"));
    }

    @Test
    void extractJsonReturnsNullOnGarbage() {
        assertNull(WebArticleUtils.extractJson("没有任何 JSON 内容"));
        assertNull(WebArticleUtils.extractJson(null));
    }

    // ──────────── parseRssLinks（RSS/Atom 解析）────────────

    @Test
    void parseRss2Links() {
        String xml = "<rss><channel><item><link>https://a.com/1</link></item>"
                + "<item><link>https://a.com/2</link></item></channel></rss>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        List<String> links = WebArticleUtils.parseRssLinks(doc);
        assertEquals(2, links.size());
        assertEquals("https://a.com/1", links.get(0));
    }

    @Test
    void parseAtomLinks() {
        String xml = "<feed><entry><link href=\"https://a.com/1\"/></entry>"
                + "<entry><link href=\"https://a.com/2\"/></entry></feed>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        List<String> links = WebArticleUtils.parseRssLinks(doc);
        assertEquals(2, links.size());
        assertEquals("https://a.com/1", links.get(0));
    }

    @Test
    void parseEmptyFeedReturnsEmpty() {
        Document doc = Jsoup.parse("<html><body>empty</body></html>", "", Parser.xmlParser());
        assertTrue(WebArticleUtils.parseRssLinks(doc).isEmpty());
    }

    // ──────────── truncate ────────────

    @Test
    void truncateHandlesNullAndBoundaries() {
        assertNull(WebArticleUtils.truncate(null, 10));
        assertEquals("abc", WebArticleUtils.truncate("abc", 10));
        assertEquals("ab", WebArticleUtils.truncate("abc", 2));
    }
}
