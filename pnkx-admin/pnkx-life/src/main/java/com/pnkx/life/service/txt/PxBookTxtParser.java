package com.pnkx.life.service.txt;

import com.pnkx.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 书籍 TXT 文件解析器
 * <p>
 * 从 PxBookServiceImpl 拆出的纯解析职责：文件校验、字符集探测（BOM/UTF-8/GB18030）、
 * 章节标题识别（第X章/序章楔子等/导出格式回读）、正文规整与字数统计。
 * 无状态，不含任何数据库访问。
 *
 * @author phy
 */
@Component
public class PxBookTxtParser {

    private static final int MAX_TXT_SIZE = 20 * 1024 * 1024;
    private static final int MAX_IMPORT_CHAPTERS = 5000;
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "(第[零〇一二三四五六七八九十百千万两0-9０-９]{1,16}章(?:[\\s　:：、.．-]+[^\\r\\n]{0,120})?)");
    private static final Pattern CHAPTER_KEY_PATTERN = Pattern.compile(
            "第[零〇一二三四五六七八九十百千万两0-9０-９]{1,16}章");
    private static final Pattern SPECIAL_CHAPTER_PATTERN = Pattern.compile(
            "^\\s*(序章|楔子|前言|引子|后记|尾声)\\s*$");
    private static final Pattern EXPORTED_CHAPTER_PATTERN = Pattern.compile("^\\s*【章节】\\s*(.+?)\\s*$");

    /**
     * 解析结果：探测到的编码 + 章节列表
     */
    public static class ParsedTxt {
        private final String encoding;
        private final List<ParsedChapter> chapters;

        public ParsedTxt(String encoding, List<ParsedChapter> chapters) {
            this.encoding = encoding;
            this.chapters = chapters;
        }

        public String getEncoding() {
            return encoding;
        }

        public List<ParsedChapter> getChapters() {
            return chapters;
        }
    }

    /**
     * 单个章节：标题 + 规整后的正文
     */
    public static class ParsedChapter {
        private final String name;
        private final String content;

        public ParsedChapter(String name, String content) {
            this.name = name;
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public String getContent() {
            return content;
        }
    }

    private static class Heading {
        private final int lineIndex;
        private final String key;
        private final String title;

        private Heading(int lineIndex, String key, String title) {
            this.lineIndex = lineIndex;
            this.key = key;
            this.title = title;
        }
    }

    private static class DecodedTxt {
        private final String text;
        private final String encoding;

        private DecodedTxt(String text, String encoding) {
            this.text = text;
            this.encoding = encoding;
        }
    }

    /**
     * 解析 TXT：校验 → 解码 → 章节切分 → 正文规整
     */
    public ParsedTxt parse(byte[] bytes, String fileName) {
        validateTxt(bytes, fileName);
        DecodedTxt decoded = decodeTxt(bytes);
        String text = decoded.text.replace("\r\n", "\n").replace('\r', '\n').replace("\uFEFF", "");
        String[] lines = text.split("\n", -1);
        List<Heading> headings = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            Heading heading = parseHeading(lines[i], i);
            if (heading == null) {
                continue;
            }
            if (!headings.isEmpty() && headings.get(headings.size() - 1).key.equals(heading.key)) {
                // 网页复制文本常在标题、面包屑、正文标题中重复同一章，以最后一次为正文起点。
                headings.set(headings.size() - 1, heading);
            } else {
                headings.add(heading);
            }
        }

        List<ParsedChapter> chapters = new ArrayList<>();
        if (headings.isEmpty()) {
            String content = normalizeTxtContent(text);
            if (content.isEmpty()) {
                throw new ServiceException("TXT文件没有可导入的正文");
            }
            chapters.add(new ParsedChapter("正文", content));
        } else {
            for (int i = 0; i < headings.size(); i++) {
                Heading heading = headings.get(i);
                int end = i + 1 < headings.size() ? headings.get(i + 1).lineIndex : lines.length;
                StringBuilder content = new StringBuilder();
                for (int line = heading.lineIndex + 1; line < end; line++) {
                    content.append(lines[line]);
                    if (line + 1 < end) {
                        content.append('\n');
                    }
                }
                chapters.add(new ParsedChapter(limitChapterName(heading.title), normalizeTxtContent(content.toString())));
            }
        }
        if (chapters.size() > MAX_IMPORT_CHAPTERS) {
            throw new ServiceException("识别到的章节超过" + MAX_IMPORT_CHAPTERS + "章，请拆分文件后导入");
        }
        return new ParsedTxt(decoded.encoding, chapters);
    }

    /**
     * 由文件名推导书名（去扩展名，兜底默认名，截断200字）
     */
    public String suggestTitle(String fileName) {
        String name = safeFileName(fileName);
        int dot = name.lastIndexOf('.');
        String title = dot > 0 ? name.substring(0, dot) : name;
        title = title.trim();
        if (title.isEmpty()) {
            title = "TXT导入书籍";
        }
        return title.length() > 200 ? title.substring(0, 200) : title;
    }

    /**
     * 文件名去路径后的安全展示名
     */
    public String safeFileName(String fileName) {
        if (fileName == null) {
            return "book.txt";
        }
        return fileName.replace('\\', '/').substring(fileName.replace('\\', '/').lastIndexOf('/') + 1);
    }

    /**
     * 正文字数（去除空白后按码点计数）
     */
    public int countWords(String content) {
        if (content == null) {
            return 0;
        }
        String compact = content.replaceAll("[\\s　]", "");
        return compact.codePointCount(0, compact.length());
    }

    private Heading parseHeading(String line, int lineIndex) {
        String value = line == null ? "" : line.trim();
        if (value.isEmpty() || value.length() > 300) {
            return null;
        }
        Matcher exported = EXPORTED_CHAPTER_PATTERN.matcher(value);
        if (exported.matches()) {
            String title = exported.group(1).trim();
            return title.isEmpty() ? null : new Heading(lineIndex, "export-" + lineIndex, title);
        }
        Matcher matcher = CHAPTER_PATTERN.matcher(value);
        if (matcher.find()) {
            String title = matcher.group(1).trim().replaceAll("[\\s　]+", " ");
            Matcher keyMatcher = CHAPTER_KEY_PATTERN.matcher(title);
            String key = keyMatcher.find() ? keyMatcher.group() : title;
            return new Heading(lineIndex, key, title);
        }
        Matcher special = SPECIAL_CHAPTER_PATTERN.matcher(value);
        return special.matches() ? new Heading(lineIndex, special.group(1), special.group(1)) : null;
    }

    private String normalizeTxtContent(String content) {
        String normalized = content == null ? "" : content;
        // 常见中文小说TXT用两个全角空格标记新段落，即使整章只有一行也能恢复段落。
        normalized = normalized.replaceAll("　{2,}", "\n");
        normalized = normalized.replace('\u00a0', ' ')
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n[ \\t]+", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return normalized;
    }

    private void validateTxt(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException("请选择非空TXT文件");
        }
        if (bytes.length > MAX_TXT_SIZE) {
            throw new ServiceException("TXT文件不能超过20MB");
        }
        if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
            throw new ServiceException("仅支持.txt格式文件");
        }
    }

    private DecodedTxt decodeTxt(byte[] bytes) {
        if (startsWith(bytes, (byte) 0xEF, (byte) 0xBB, (byte) 0xBF)) {
            return new DecodedTxt(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8), "UTF-8");
        }
        if (startsWith(bytes, (byte) 0xFF, (byte) 0xFE)) {
            return new DecodedTxt(new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE), "UTF-16LE");
        }
        if (startsWith(bytes, (byte) 0xFE, (byte) 0xFF)) {
            return new DecodedTxt(new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE), "UTF-16BE");
        }
        try {
            CharBuffer chars = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return new DecodedTxt(chars.toString(), "UTF-8");
        } catch (CharacterCodingException ignored) {
            return new DecodedTxt(new String(bytes, Charset.forName("GB18030")), "GB18030");
        }
    }

    private boolean startsWith(byte[] bytes, byte... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String limitChapterName(String value) {
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}
