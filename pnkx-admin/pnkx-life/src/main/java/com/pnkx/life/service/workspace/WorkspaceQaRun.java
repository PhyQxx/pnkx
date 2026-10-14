package com.pnkx.life.service.workspace;
import lombok.Data;
@Data
public class WorkspaceQaRun {
 private String requestId,ownerId,status,answer,sourcesJson,coverage,targetType;
 private Long targetId,elapsedMs;
 private Integer inputChars,outputChars;
 private java.util.Date createTime;
}
