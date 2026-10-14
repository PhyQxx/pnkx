package com.pnkx.life.mapper;
import com.pnkx.life.service.workspace.WorkspaceQaRun;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface WorkspaceQaMapper {
 WorkspaceQaRun get(@Param("owner") String owner,@Param("id") String id);
 List<WorkspaceQaRun> list(String owner);
 void expire(String owner);
 int running(String owner);
 void insert(WorkspaceQaRun row);
 int finish(WorkspaceQaRun row);
 void cancel(@Param("owner") String owner,@Param("id") String id);
 void applied(WorkspaceQaRun row);
}
