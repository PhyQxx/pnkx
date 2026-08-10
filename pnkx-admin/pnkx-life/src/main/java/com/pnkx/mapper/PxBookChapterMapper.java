package com.pnkx.mapper;

import com.pnkx.domain.po.PxBookChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PxBookChapterMapper {
    List<PxBookChapter> selectChapterList(PxBookChapter chapter);
    PxBookChapter selectChapterById(@Param("id") Long id, @Param("userId") String userId);
    PxBookChapter selectPreviousChapter(@Param("chapter") PxBookChapter chapter, @Param("userId") String userId);
    PxBookChapter selectNextChapter(@Param("chapter") PxBookChapter chapter, @Param("userId") String userId);
    int insertChapter(PxBookChapter chapter);
    int updateChapter(PxBookChapter chapter);
    int deleteChapters(@Param("ids") Long[] ids, @Param("userId") String userId);
    int deleteByBookIds(@Param("bookIds") Long[] bookIds, @Param("userId") String userId);
}
