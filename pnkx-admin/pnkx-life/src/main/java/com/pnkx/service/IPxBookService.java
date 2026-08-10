package com.pnkx.service;

import com.pnkx.domain.po.PxBook;
import com.pnkx.domain.po.PxBookChapter;
import com.pnkx.domain.po.PxBookTxtPreview;

import java.util.List;
import java.util.Map;

public interface IPxBookService {
    List<PxBook> selectBookList(PxBook book);
    PxBook selectBookById(Long id, String userId);
    int insertBook(PxBook book);
    int updateBook(PxBook book);
    int deleteBooks(Long[] ids, String userId);
    List<PxBookChapter> selectChapterList(PxBookChapter chapter);
    PxBookChapter selectChapterById(Long id, String userId);
    Map<String, Object> selectReaderData(Long id, String userId);
    int insertChapter(PxBookChapter chapter);
    int insertChapters(List<PxBookChapter> chapters);
    int updateChapter(PxBookChapter chapter);
    int deleteChapters(Long[] ids, String userId);
    int updateReadingProgress(Long chapterId, String userId);
    PxBookTxtPreview previewTxt(byte[] bytes, String fileName);
    PxBook importTxt(byte[] bytes, String fileName, PxBook book);
    List<PxBook> importTxtBatch(List<byte[]> files, List<String> fileNames, List<PxBook> books);
    String exportTxt(Long bookId, String userId);
}
