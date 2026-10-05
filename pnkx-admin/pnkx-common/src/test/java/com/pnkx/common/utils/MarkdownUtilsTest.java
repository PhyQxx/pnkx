package com.pnkx.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Markdown 渲染工具单元测试
 *
 * @author phy
 */
class MarkdownUtilsTest {

    @Test
    void 标题加粗列表转换为HTML标签() {
        String md = "### 📅 周报\n\n- 本周支出：**358.90 元**（共 12 笔）\n- 完成待办：**3** 项\n";
        String html = MarkdownUtils.toHtml(md);

        assertTrue(html.contains("<h3>📅 周报</h3>"));
        assertTrue(html.contains("<ul>"));
        assertTrue(html.contains("<li>本周支出：<strong>358.90 元</strong>（共 12 笔）</li>"));
        // 原始标记不应残留
        assertFalse(html.contains("###"));
        assertFalse(html.contains("**"));
    }

    @Test
    void 引用分隔线有序列表转换() {
        String md = "1. **消费方面**：餐饮支出较高\n2. **待办方面**：还有未完成项\n\n---\n\n> 由 Pnkx 自动生成\n";
        String html = MarkdownUtils.toHtml(md);

        assertTrue(html.contains("<ol>"));
        assertTrue(html.contains("<li><strong>消费方面</strong>：餐饮支出较高</li>"));
        assertTrue(html.contains("<hr />"));
        assertTrue(html.contains("<blockquote>"));
        assertTrue(html.contains("由 Pnkx 自动生成"));
    }

    @Test
    void 空输入返回空串() {
        assertEquals("", MarkdownUtils.toHtml(null));
        assertEquals("", MarkdownUtils.toHtml(""));
    }

    @Test
    void 邮件文档包含容器样式与转义标题() {
        String html = MarkdownUtils.toEmailHtml("- 项目一", "周报<测试>");
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<div class=\"md-card\""));
        assertTrue(html.contains("Pei你看雪 自动发送"));
        // 标题中的尖括号必须转义，防止注入邮件文档结构
        assertTrue(html.contains("<title>周报&lt;测试&gt;</title>"));
    }
}
