package com.pnkx.service.impl;

import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.domain.po.PxBook;
import com.pnkx.domain.po.PxBookChapter;
import com.pnkx.domain.po.PxBookTxtPreview;
import com.pnkx.mapper.PxBookChapterMapper;
import com.pnkx.mapper.PxBookMapper;
import com.pnkx.service.IPxBookService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PxBookServiceImpl implements IPxBookService {
    private static final int MAX_TXT_SIZE = 20 * 1024 * 1024;
    private static final int MAX_IMPORT_CHAPTERS = 5000;
    private static final int MAX_PREVIEW_CHAPTERS = 200;
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "(第[零〇一二三四五六七八九十百千万两0-9０-９]{1,16}章(?:[\\s　:：、.．-]+[^\\r\\n]{0,120})?)");
    private static final Pattern CHAPTER_KEY_PATTERN = Pattern.compile(
            "第[零〇一二三四五六七八九十百千万两0-9０-９]{1,16}章");
    private static final Pattern SPECIAL_CHAPTER_PATTERN = Pattern.compile(
            "^\\s*(序章|楔子|前言|引子|后记|尾声)\\s*$");
    private static final Pattern EXPORTED_CHAPTER_PATTERN = Pattern.compile("^\\s*【章节】\\s*(.+?)\\s*$");
    @Resource
    private PxBookMapper bookMapper;
    @Resource
    private PxBookChapterMapper chapterMapper;

    @Override
    public List<PxBook> selectBookList(PxBook book) {
        return bookMapper.selectBookList(book);
    }

    @Override
    public PxBook selectBookById(Long id, String userId) {
        return bookMapper.selectBookById(id, userId);
    }

    @Override
    public int insertBook(PxBook book) {
        book.setCreateTime(DateUtils.getNowDate());
        if (book.getStatus() == null) {
            book.setStatus("reading");
        }
        return bookMapper.insertBook(book);
    }

    @Override
    public int updateBook(PxBook book) {
        book.setUpdateTime(DateUtils.getNowDate());
        return bookMapper.updateBook(book);
    }

    @Override
    @Transactional
    public int deleteBooks(Long[] ids, String userId) {
        chapterMapper.deleteByBookIds(ids, userId);
        return bookMapper.deleteBooks(ids, userId);
    }

    @Override
    public List<PxBookChapter> selectChapterList(PxBookChapter chapter) {
        return chapterMapper.selectChapterList(chapter);
    }

    @Override
    public PxBookChapter selectChapterById(Long id, String userId) {
        return chapterMapper.selectChapterById(id, userId);
    }

    @Override
    public Map<String, Object> selectReaderData(Long id, String userId) {
        PxBookChapter chapter = chapterMapper.selectChapterById(id, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("chapter", chapter);
        if (chapter != null) {
            result.put("previous", chapterMapper.selectPreviousChapter(chapter, userId));
            result.put("next", chapterMapper.selectNextChapter(chapter, userId));
        }
        return result;
    }

    @Override
    public int insertChapter(PxBookChapter chapter) {
        normalizeChapterContent(chapter);
        chapter.setCreateTime(DateUtils.getNowDate());
        return chapterMapper.insertChapter(chapter);
    }

    @Override
    @Transactional
    public int insertChapters(List<PxBookChapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            throw new ServiceException("请至少添加一个章节");
        }
        int rows = 0;
        for (PxBookChapter chapter : chapters) {
            if (chapter == null || chapter.getBookId() == null || chapter.getCreateBy() == null) {
                throw new ServiceException("批量章节数据不完整");
            }
            int inserted = insertChapter(chapter);
            if (inserted != 1) {
                throw new ServiceException("书籍不存在或无权新增章节");
            }
            rows += inserted;
        }
        return rows;
    }

    @Override
    public int updateChapter(PxBookChapter chapter) {
        normalizeChapterContent(chapter);
        chapter.setUpdateTime(DateUtils.getNowDate());
        return chapterMapper.updateChapter(chapter);
    }

    @Override
    public int deleteChapters(Long[] ids, String userId) {
        return chapterMapper.deleteChapters(ids, userId);
    }

    @Override
    public int updateReadingProgress(Long chapterId, String userId) {
        return bookMapper.updateReadingProgress(chapterId, userId);
    }

    @Override
    public PxBookTxtPreview previewTxt(byte[] bytes, String fileName) {
        ParsedTxt parsed = parseTxt(bytes, fileName);
        PxBookTxtPreview preview = new PxBookTxtPreview();
        preview.setFileName(fileName);
        preview.setSuggestedTitle(suggestTitle(fileName));
        preview.setEncoding(parsed.encoding);
        preview.setChapterCount(parsed.chapters.size());
        int totalWordCount = 0;
        List<PxBookTxtPreview.Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < parsed.chapters.size(); i++) {
            ParsedChapter chapter = parsed.chapters.get(i);
            int wordCount = countWords(chapter.content);
            totalWordCount += wordCount;
            if (i < MAX_PREVIEW_CHAPTERS) {
                chapters.add(new PxBookTxtPreview.Chapter(i + 1, chapter.name, wordCount));
            }
        }
        preview.setTotalWordCount(totalWordCount);
        preview.setChapters(chapters);
        return preview;
    }

    @Override
    @Transactional
    public PxBook importTxt(byte[] bytes, String fileName, PxBook book) {
        ParsedTxt parsed = parseTxt(bytes, fileName);
        if (book == null || book.getCreateBy() == null) {
            throw new ServiceException("导入信息不完整");
        }
        String title = trimToNull(book.getTitle());
        if (title == null) {
            title = suggestTitle(fileName);
        }
        if (title.length() > 200) {
            throw new ServiceException("书名不能超过200个字符");
        }
        if (book.getAuthor() != null && book.getAuthor().length() > 100) {
            throw new ServiceException("作者不能超过100个字符");
        }
        book.setTitle(title);
        book.setAuthor(trimToNull(book.getAuthor()));
        book.setStatus(normalizeStatus(book.getStatus()));
        String importRemark = "由TXT文件导入：" + safeFileName(fileName);
        book.setRemark(importRemark.length() > 255 ? importRemark.substring(0, 255) : importRemark);
        if (insertBook(book) != 1 || book.getId() == null) {
            throw new ServiceException("创建书籍失败");
        }
        for (int i = 0; i < parsed.chapters.size(); i++) {
            ParsedChapter source = parsed.chapters.get(i);
            PxBookChapter chapter = new PxBookChapter();
            chapter.setBookId(book.getId());
            chapter.setChapterNo(i + 1);
            chapter.setChapterName(source.name);
            chapter.setContent(source.content);
            chapter.setCreateBy(book.getCreateBy());
            if (insertChapter(chapter) != 1) {
                throw new ServiceException("导入第" + (i + 1) + "章失败");
            }
        }
        book.setChapterCount(parsed.chapters.size());
        return book;
    }

    @Override
    @Transactional
    public List<PxBook> importTxtBatch(List<byte[]> files, List<String> fileNames, List<PxBook> books) {
        if (files == null || fileNames == null || books == null
                || files.isEmpty() || files.size() != fileNames.size() || files.size() != books.size()) {
            throw new ServiceException("批量导入数据不完整");
        }
        if (files.size() > 20) {
            throw new ServiceException("每次最多导入20本书");
        }
        long totalSize = 0;
        for (byte[] file : files) {
            totalSize += file == null ? 0 : file.length;
        }
        if (totalSize > 80L * 1024 * 1024) {
            throw new ServiceException("批量导入文件总大小不能超过80MB");
        }
        List<PxBook> imported = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            imported.add(importTxt(files.get(i), fileNames.get(i), books.get(i)));
        }
        return imported;
    }

    @Override
    public String exportTxt(Long bookId, String userId) {
        PxBook book = bookMapper.selectBookById(bookId, userId);
        if (book == null) {
            throw new ServiceException("书籍不存在或无权导出");
        }
        PxBookChapter query = new PxBookChapter();
        query.setBookId(bookId);
        query.setCreateBy(userId);
        List<PxBookChapter> chapters = chapterMapper.selectChapterList(query);
        StringBuilder output = new StringBuilder();
        output.append("【书名】").append(book.getTitle()).append('\n');
        if (trimToNull(book.getAuthor()) != null) {
            output.append("【作者】").append(book.getAuthor().trim()).append('\n');
        }
        output.append('\n');
        for (PxBookChapter chapter : chapters) {
            output.append("【章节】").append(chapter.getChapterName().trim()).append("\n\n");
            if (chapter.getContent() != null) {
                output.append(chapter.getContent().trim());
            }
            output.append("\n\n");
        }
        return output.toString();
    }

    private ParsedTxt parseTxt(byte[] bytes, String fileName) {
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
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) return false;
        }
        return true;
    }

    private String suggestTitle(String fileName) {
        String name = safeFileName(fileName);
        int dot = name.lastIndexOf('.');
        String title = dot > 0 ? name.substring(0, dot) : name;
        title = title.trim();
        if (title.isEmpty()) title = "TXT导入书籍";
        return title.length() > 200 ? title.substring(0, 200) : title;
    }

    private String safeFileName(String fileName) {
        if (fileName == null) return "book.txt";
        return fileName.replace('\\', '/').substring(fileName.replace('\\', '/').lastIndexOf('/') + 1);
    }

    private String normalizeStatus(String status) {
        return "finished".equals(status) || "shelved".equals(status) ? status : "reading";
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private String limitChapterName(String value) {
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    private int countWords(String content) {
        if (content == null) return 0;
        String compact = content.replaceAll("[\\s　]", "");
        return compact.codePointCount(0, compact.length());
    }

    private static class ParsedTxt {
        private final String encoding;
        private final List<ParsedChapter> chapters;

        private ParsedTxt(String encoding, List<ParsedChapter> chapters) {
            this.encoding = encoding;
            this.chapters = chapters;
        }
    }

    private static class ParsedChapter {
        private final String name;
        private final String content;

        private ParsedChapter(String name, String content) {
            this.name = name;
            this.content = content;
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

    private void normalizeChapterContent(PxBookChapter chapter) {
        if (!Boolean.TRUE.equals(chapter.getConvertHtml()) || chapter.getContent() == null) {
            return;
        }
        String html = chapter.getContent();
        if (!html.matches("(?s).*</?[a-zA-Z][^>]*>.*")
                && html.matches("(?is).*&lt;/?[a-z][^&]*&gt;.*")) {
            html = Jsoup.parse(html).text();
        }
        StringBuilder output = new StringBuilder();
        appendPlainText(Jsoup.parse(html).body(), output);
        String plainText = output.toString()
                .replace('\u00a0', ' ')
                .replaceAll("[\\t\\f ]+", " ")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n[ \\t]+", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        chapter.setContent(plainText);
    }

    /**
     * 按 DOM 块级结构提取文本，避免 Jsoup.text() 将连续 p 标签合并成同一行。
     */
    private void appendPlainText(Node node, StringBuilder output) {
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

    private boolean isBlockTag(String tag) {
        return "p".equals(tag) || "div".equals(tag) || "li".equals(tag)
                || "blockquote".equals(tag) || "tr".equals(tag)
                || "section".equals(tag) || "article".equals(tag)
                || tag.matches("h[1-6]");
    }

    private void appendLineBreak(StringBuilder output) {
        if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
            output.append('\n');
        }
    }
}
