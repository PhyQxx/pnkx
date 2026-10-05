package com.pnkx.common.utils;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Markdown 渲染工具
 * <p>
 * 基于 commonmark-java（CommonMark 规范）。用于把 AI 生成的 Markdown 文本
 * 转成 HTML——邮件客户端不渲染 Markdown 原文（### ** 等标记会原样显示），
 * 发送前必须经 {@link #toEmailHtml} 转换。
 *
 * @author phy
 */
public class MarkdownUtils {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    /**
     * Markdown → HTML 片段（无外层文档/样式）
     */
    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        Node document = PARSER.parse(markdown);
        return RENDERER.render(document);
    }

    /**
     * Markdown → 可直接作为 HTML 邮件正文的完整文档。
     * 样式以内联 style + head style 双保险（QQ邮箱/163/Gmail 对两者支持度不同）。
     *
     * @param markdown Markdown 文本
     * @param title    邮件标题（同时作为页面标题展示）
     */
    public static String toEmailHtml(String markdown, String title) {
        String body = toHtml(markdown);
        String safeTitle = title == null ? "" : title.replace("<", "&lt;").replace(">", "&gt;");
        return "<!DOCTYPE html>\n"
                + "<html lang=\"zh-CN\">\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\"/>\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n"
                + "<title>" + safeTitle + "</title>\n"
                + "<style>\n"
                + "  body{margin:0;padding:24px;background:#f4f6f9;font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;color:#2c3e50;}\n"
                + "  .md-card{max-width:640px;margin:0 auto;background:#ffffff;border-radius:12px;padding:28px 32px;box-shadow:0 2px 8px rgba(0,0,0,0.06);}\n"
                + "  .md-card h1{font-size:22px;margin:20px 0 12px;color:#1f2d3d;}\n"
                + "  .md-card h2{font-size:19px;margin:18px 0 10px;color:#1f2d3d;border-left:4px solid #409eff;padding-left:10px;}\n"
                + "  .md-card h3{font-size:17px;margin:16px 0 8px;color:#2c3e50;}\n"
                + "  .md-card h4{font-size:15px;margin:14px 0 8px;color:#2c3e50;}\n"
                + "  .md-card p{font-size:14px;line-height:1.8;margin:8px 0;}\n"
                + "  .md-card strong{color:#409eff;}\n"
                + "  .md-card ul,.md-card ol{font-size:14px;line-height:1.9;padding-left:22px;margin:8px 0;}\n"
                + "  .md-card li{margin:4px 0;}\n"
                + "  .md-card blockquote{margin:14px 0;padding:10px 14px;background:#ecf5ff;border-left:4px solid #409eff;border-radius:0 8px 8px 0;color:#5e6d82;font-size:13px;}\n"
                + "  .md-card hr{border:none;border-top:1px dashed #dcdfe6;margin:20px 0;}\n"
                + "  .md-card code{background:#ecf5ff;color:#409eff;padding:2px 6px;border-radius:4px;font-size:13px;font-family:Monaco,Menlo,monospace;}\n"
                + "  .md-card pre{background:#f8f9fa;border-radius:8px;padding:12px;overflow-x:auto;}\n"
                + "  .md-card pre code{background:none;padding:0;color:#2c3e50;}\n"
                + "  .md-card table{border-collapse:collapse;width:100%;margin:12px 0;font-size:13px;}\n"
                + "  .md-card th,.md-card td{border:1px solid #ebeef5;padding:8px 12px;text-align:left;}\n"
                + "  .md-card th{background:#ecf5ff;color:#409eff;}\n"
                + "  .md-card tr:nth-child(even){background:#fafbfc;}\n"
                + "  .md-footer{max-width:640px;margin:12px auto 0;text-align:center;font-size:12px;color:#909399;}\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "<div class=\"md-card\" style=\"max-width:640px;margin:0 auto;background:#ffffff;border-radius:12px;padding:28px 32px;\">\n"
                + body + "\n"
                + "</div>\n"
                + "<div class=\"md-footer\" style=\"max-width:640px;margin:12px auto 0;text-align:center;font-size:12px;color:#909399;\">此邮件由 Pei你看雪 自动发送 · 请勿直接回复</div>\n"
                + "</body>\n"
                + "</html>";
    }
}
