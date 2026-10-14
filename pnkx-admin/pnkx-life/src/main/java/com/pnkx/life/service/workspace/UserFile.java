package com.pnkx.life.service.workspace;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Data
public class UserFile {
 private Long id;
 private String ownerId;
 private Long parentId=0L;
 private String displayName;
 private Boolean isFolder=false;
 @JsonIgnore private String objectKey;
 private String mime;
 private Long byteSize=0L;
 private String contentHash;
 private Long revision=1L;
 private Integer delFlag=0;
}
