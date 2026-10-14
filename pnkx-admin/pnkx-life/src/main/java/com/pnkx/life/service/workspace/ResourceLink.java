package com.pnkx.life.service.workspace;
import lombok.Data;
@Data
public class ResourceLink {
    private String ownerId;
    private String sourceType;
    private Long sourceId;
    private String targetType;
    private Long targetId;
    private String relationType;
    private String title;
    private boolean unavailable;
}
