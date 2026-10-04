package com.pnkx.life.service.txt;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.service.txt.PxBookTxtParser.ParsedChapter;
import com.pnkx.life.service.txt.PxBookTxtParser.ParsedTxt;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TXT 解析器单元测试：章节切分、字符集探测、正文规整与标题推导
 *
 * @author phy
 */
class PxBookTxtParserTest {

    private final PxBookTxtParser parser = new PxBookTxtParser();

    private List<String> chapterNames(ParsedTxt parsed) {
        return parsed.getChapters().stream().map(ParsedChapter::getName).collect(Collectors.toList());
    }

    @Test
    void 中文章节标题切分() {
        String txt = "前言废话\n\n第一章 初见\n　　正文一。\n　　第二段。\n\n第二章 重逢\n正文二。";
        ParsedTxt parsed = parser.parse(txt.getBytes(StandardCharsets.UTF_8), "书.txt");

        assertEquals(2, parsed.getChapters().size());
        assertEquals("第一章 初见", parsed.getChapters().get(0).getName());
        assertEquals("第二章 重逢", parsed.getChapters().get(1).getName());
        // 全角双空格恢复为段落（每个缩进转一个换行，与原标题行换行合并为空行）
        assertTrue(parsed.getChapters().get(0).getContent().contains("正文一。\n\n第二段。"));
        // 标题前的杂项内容不属于任何章节，被丢弃
        assertEquals("正文二。", parsed.getChapters().get(1).getContent());
    }

    @Test
    void 数字与汉字混用章节号() {
        String txt = "第12章 测试\nA\n第两百章 测试二\nB";
        ParsedTxt parsed = parser.parse(txt.getBytes(StandardCharsets.UTF_8), "x.txt");
        assertEquals(List.of("第12章 测试", "第两百章 测试二"), chapterNames(parsed));
    }

    @Test
    void 特殊章节名序章楔子() {
        String txt = "序章\na\n楔子\nb\n第一章 开始\nc";
        ParsedTxt parsed = parser.parse(txt.getBytes(StandardCharsets.UTF_8), "x.txt");
        assertEquals(List.of("序章", "楔子", "第一章 开始"), chapterNames(parsed));
    }

    @Test
    void 无章节标题时整体作为正文() {
        String txt = "只是一段没有章节标记的文本。";
        ParsedTxt parsed = parser.parse(txt.getBytes(StandardCharsets.UTF_8), "x.txt");
        assertEquals(1, parsed.getChapters().size());
        assertEquals("正文", parsed.getChapters().get(0).getName());
        assertEquals("只是一段没有章节标记的文本。", parsed.getChapters().get(0).getContent());
    }

    @Test
    void 同名连续标题去重取最后一次() {
        String txt = "第一章 开始\n目录里的简介\n第一章 开始\n真正正文。";
        ParsedTxt parsed = parser.parse(txt.getBytes(StandardCharsets.UTF_8), "x.txt");
        assertEquals(1, parsed.getChapters().size());
        assertEquals("真正正文。", parsed.getChapters().get(0).getContent());
    }

    @Test
    void 导出格式回读识别章节() {
        String txt = "【书名】某书\n\n【章节】第一章 起\n内容一\n\n【章节】第二章 承\n内容二";
        ParsedTxt parsed = parser.parse(txt.getBytes(StandardCharsets.UTF_8), "x.txt");
        assertEquals(List.of("第一章 起", "第二章 承"), chapterNames(parsed));
    }

    @Test
    void UTF8带BOM解码并去BOM() {
        byte[] bytes = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "第一章 A\nhi".getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[3 + body.length];
        System.arraycopy(bytes, 0, all, 0, 3);
        System.arraycopy(body, 0, all, 3, body.length);

        ParsedTxt parsed = parser.parse(all, "x.txt");
        assertEquals("UTF-8", parsed.getEncoding());
        assertEquals("hi", parsed.getChapters().get(0).getContent());
    }

    @Test
    void GB18030自动回退解码() {
        // "第一章 起" 的 GB18030 字节不是合法 UTF-8，应回退 GB18030
        byte[] gbBytes = "第一章 起\n你好".getBytes(java.nio.charset.Charset.forName("GB18030"));
        ParsedTxt parsed = parser.parse(gbBytes, "x.txt");
        assertEquals("GB18030", parsed.getEncoding());
        assertEquals("你好", parsed.getChapters().get(0).getContent());
    }

    @Test
    void 空文件与非TXT后缀被拒绝() {
        assertThrows(ServiceException.class, () -> parser.parse(new byte[0], "a.txt"));
        assertThrows(ServiceException.class, () -> parser.parse("内容".getBytes(), "a.epub"));
    }

    @Test
    void 规整后无正文报错() {
        // 只有章节标题没有内容
        assertThrows(ServiceException.class,
                () -> parser.parse("   \n\t\n".getBytes(StandardCharsets.UTF_8), "a.txt"));
    }

    @Test
    void 标题推导与字数统计() {
        assertEquals("我的书", parser.suggestTitle("我的书.txt"));
        assertEquals("书", parser.suggestTitle("d:/dir/书.txt"));
        // 现行为：仅扩展名的文件名保留原值 ".txt"（截点在第0位不剥扩展名）
        assertEquals(".txt", parser.suggestTitle(".txt"));
        assertEquals("book", parser.suggestTitle(null));

        assertEquals(4, parser.countWords("你 好　世 界\n\t"));
        assertEquals(0, parser.countWords(null));
        // emoji 按码点计 1
        assertEquals(3, parser.countWords("a🌟b"));
    }
}
