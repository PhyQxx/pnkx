package com.pnkx.life.service.workspace;
import lombok.Data;
@Data
public class ResourceTag {
    private Long id;
    private String ownerId;
    private String name;
    private String normalizedName;
    private Long parentId;
    private String color;
    private Long revision;
}
