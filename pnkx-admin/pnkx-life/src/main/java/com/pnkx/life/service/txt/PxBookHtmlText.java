package com.pnkx.life.service.txt;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * 章节富文本 → 纯文本转换
 * <p>
 * 从 PxBookServiceImpl 拆出的 HTML 清洗职责：按 DOM 块级结构提取文本
 *（避免 Jsoup.text() 把连续 p 标签并成一行），并做空白规整。
 *
 * @author phy
 */
public final class PxBookHtmlText {

    private PxBookHtmlText() {
    }

    /**
     * 将 HTML 章节内容转为规整后的纯文本
     */
    public static String toPlainText(String html) {
        String source = html;
        if (!source.matches("(?s).*</?[a-zA-Z][^>]*>.*")
                && source.matches("(?is).*&lt;/?[a-z][^&]*&gt;.*")) {
            source = Jsoup.parse(source).text();
        }
        StringBuilder output = new StringBuilder();
        appendPlainText(Jsoup.parse(source).body(), output);
        return output.toString()
                .replace('\u00a0', ' ')
                .replaceAll("[\\t\\f ]+", " ")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n[ \\t]+", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 按 DOM 块级结构提取文本，避免 Jsoup.text() 将连续 p 标签合并成同一行。
     */
    private static void appendPlainText(Node node, StringBuilder output) {
        if (node instanceof TextNode) {
            output.append(((TextNode) node).getWholeText());
            return;
        }
        String tag = node.nodeName().toLowerCase();
        if ("script".equals(tag) || "style".equals(tag)) {
            return;
        }
        if ("br".equals(tag)) {
            appendLineBreak(output);
            return;
        }
        for (Node child : node.childNodes()) {
            appendPlainText(child, output);
        }
        if (isBlockTag(tag)) {
            appendLineBreak(output);
        }
    }

    private static boolean isBlockTag(String tag) {
        return "p".equals(tag) || "div".equals(tag) || "li".equals(tag)
                || "blockquote".equals(tag) || "tr".equals(tag)
                || "section".equals(tag) || "article".equals(tag)
                || tag.matches("h[1-6]");
    }

    private static void appendLineBreak(StringBuilder output) {
        if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
            output.append('\n');
        }
    }
}
