package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.life.domain.po.PxBook;
import com.pnkx.life.domain.po.PxBookChapter;
import com.pnkx.life.domain.po.PxBookTxtPreview;
import com.pnkx.life.mapper.PxBookChapterMapper;
import com.pnkx.life.mapper.PxBookMapper;
import com.pnkx.life.service.IPxBookService;
import com.pnkx.life.service.txt.PxBookHtmlText;
import com.pnkx.life.service.txt.PxBookTxtExporter;
import com.pnkx.life.service.txt.PxBookTxtParser;
import com.pnkx.life.service.txt.PxBookTxtParser.ParsedChapter;
import com.pnkx.life.service.txt.PxBookTxtParser.ParsedTxt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读书 Service：书籍/章节 CRUD 与阅读进度。
 * TXT 解析见 {@link PxBookTxtParser}，富文本转纯文本见 {@link PxBookHtmlText}，
 * 导出拼装见 {@link PxBookTxtExporter}。
 *
 * @author phy
 */
@Service
public class PxBookServiceImpl implements IPxBookService {
    private static final int MAX_PREVIEW_CHAPTERS = 200;

    @Resource
    private PxBookMapper bookMapper;
    @Resource
    private PxBookChapterMapper chapterMapper;
    @Resource
    private PxBookTxtParser txtParser;
    @Resource
    private PxBookTxtExporter txtExporter;

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
        if (StringUtils.isNotEmpty(book.getClientUuid())) {
            PxBook existing = bookMapper.selectByClientUuid(book.getClientUuid());
            if (existing != null) {
                book.setId(existing.getId());
                return 1;
            }
        }
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
        ParsedTxt parsed = txtParser.parse(bytes, fileName);
        PxBookTxtPreview preview = new PxBookTxtPreview();
        preview.setFileName(fileName);
        preview.setSuggestedTitle(txtParser.suggestTitle(fileName));
        preview.setEncoding(parsed.getEncoding());
        preview.setChapterCount(parsed.getChapters().size());
        int totalWordCount = 0;
        List<PxBookTxtPreview.Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < parsed.getChapters().size(); i++) {
            ParsedChapter chapter = parsed.getChapters().get(i);
            int wordCount = txtParser.countWords(chapter.getContent());
            totalWordCount += wordCount;
            if (i < MAX_PREVIEW_CHAPTERS) {
                chapters.add(new PxBookTxtPreview.Chapter(i + 1, chapter.getName(), wordCount));
            }
        }
        preview.setTotalWordCount(totalWordCount);
        preview.setChapters(chapters);
        return preview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PxBook importTxt(byte[] bytes, String fileName, PxBook book) {
        ParsedTxt parsed = txtParser.parse(bytes, fileName);
        if (book == null || book.getCreateBy() == null) {
            throw new ServiceException("导入信息不完整");
        }
        String title = trimToNull(book.getTitle());
        if (title == null) {
            title = txtParser.suggestTitle(fileName);
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
        String importRemark = "由TXT文件导入：" + txtParser.safeFileName(fileName);
        book.setRemark(importRemark.length() > 255 ? importRemark.substring(0, 255) : importRemark);
        if (insertBook(book) != 1 || book.getId() == null) {
            throw new ServiceException("创建书籍失败");
        }
        for (int i = 0; i < parsed.getChapters().size(); i++) {
            ParsedChapter source = parsed.getChapters().get(i);
            PxBookChapter chapter = new PxBookChapter();
            chapter.setBookId(book.getId());
            chapter.setChapterNo(i + 1);
            chapter.setChapterName(source.getName());
            chapter.setContent(source.getContent());
            chapter.setCreateBy(book.getCreateBy());
            if (insertChapter(chapter) != 1) {
                throw new ServiceException("导入第" + (i + 1) + "章失败");
            }
        }
        book.setChapterCount(parsed.getChapters().size());
        return book;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        return txtExporter.export(book, chapterMapper.selectChapterList(query));
    }

    private void normalizeChapterContent(PxBookChapter chapter) {
        if (!Boolean.TRUE.equals(chapter.getConvertHtml()) || chapter.getContent() == null) {
            return;
        }
        chapter.setContent(PxBookHtmlText.toPlainText(chapter.getContent()));
    }

    private String normalizeStatus(String status) {
        return "finished".equals(status) || "shelved".equals(status) ? status : "reading";
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
