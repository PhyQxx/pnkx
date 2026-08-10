package com.pnkx.mapper;

import com.pnkx.domain.po.PxBook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PxBookMapper {
    List<PxBook> selectBookList(PxBook book);
    PxBook selectBookById(@Param("id") Long id, @Param("userId") String userId);
    int insertBook(PxBook book);
    int updateBook(PxBook book);
    int deleteBooks(@Param("ids") Long[] ids, @Param("userId") String userId);
    int updateReadingProgress(@Param("chapterId") Long chapterId, @Param("userId") String userId);
}
