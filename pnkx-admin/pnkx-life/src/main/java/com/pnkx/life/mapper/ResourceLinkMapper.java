package com.pnkx.life.mapper;
import com.pnkx.life.service.workspace.ResourceLink;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;
public interface ResourceLinkMapper {
    void add(ResourceLink link);
    void remove(ResourceLink link);
    List<ResourceLink> outgoing(@Param("type") String type, @Param("id") Long id, @Param("owner") String owner);
    List<ResourceLink> incoming(@Param("type") String type, @Param("id") Long id, @Param("owner") String owner);
    void clearBody(@Param("type") String type, @Param("id") Long id, @Param("owner") String owner);
    void visit(@Param("type") String type, @Param("id") Long id, @Param("owner") String owner);
    void inbox(@Param("type") String type, @Param("id") Long id, @Param("owner") String owner, @Param("inbox") boolean inbox);
    void starState(@Param("id") Long id, @Param("owner") String owner, @Param("starred") boolean starred);
    void openState(@Param("id") Long id, @Param("owner") String owner, @Param("opened") boolean opened);
    Map<String,Object> action(@Param("owner") String owner, @Param("requestId") String requestId);
    void actionSave(@Param("owner") String owner, @Param("requestId") String requestId, @Param("sourceId") Long sourceId,
                    @Param("targetType") String targetType, @Param("targetId") Long targetId);
}
