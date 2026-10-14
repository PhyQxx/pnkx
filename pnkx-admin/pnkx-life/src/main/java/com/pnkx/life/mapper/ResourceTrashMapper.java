package com.pnkx.life.mapper;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.service.search.SearchResult;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface ResourceTrashMapper {
    List<SearchResult> list(@Param("owner") String owner,@Param("offset") int offset,@Param("limit") int limit);
    long count(@Param("owner") String owner);
    Long lockDeleted(@Param("type") String type,@Param("id") Long id,@Param("owner") String owner);
    int restore(@Param("type") String type,@Param("id") Long id,@Param("owner") String owner);
}
