package com.pnkx.life.mapper;
import com.pnkx.life.service.workspace.*;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface WorkspaceMapper {
    List<ResourceRecord> auditRows(@Param("owner") String owner,@Param("type") String type,@Param("after") long after);
    void ensureLock(String owner);
    String lockOwner(String owner);
    ResourceRecord resource(@Param("type") String type, @Param("id") Long id, @Param("owners") List<Long> owners,
                            @Param("all") boolean all, @Param("lock") boolean lock);
    List<ResourceTag> tags(String owner);
    void insertTag(ResourceTag tag);
    int updateTag(ResourceTag tag);
    void deleteTag(@Param("owner") String owner, @Param("id") Long id);
    List<Long> resourceTags(@Param("owner") String owner, @Param("type") String type, @Param("id") Long id);
    void clearResourceTags(@Param("owner") String owner, @Param("type") String type, @Param("id") Long id);
    void addRelation(@Param("owner") String owner, @Param("type") String type, @Param("id") Long id, @Param("tagId") Long tagId);
    void clearTagRelations(@Param("owner") String owner, @Param("tagId") Long tagId);
    List<Map<String,Object>> taggedResources(@Param("owner") String owner, @Param("tagId") Long tagId);
    void projectTags(@Param("owner") String owner, @Param("type") String type, @Param("id") Long id, @Param("tags") String tags);
    List<ResourceRecord> backfillRows(@Param("owner") String owner, @Param("type") String type, @Param("after") long after);
    Long checkpoint(@Param("owner") String owner, @Param("type") String type);
    void checkpointSave(@Param("owner") String owner, @Param("type") String type, @Param("after") long after, @Param("done") boolean done);
}
