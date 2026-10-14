package com.pnkx.life.service.workspace;
import lombok.Data;
@Data
public class ResourceRecord {
    private String fetchStatus;
    private Long id;
    private String ownerId;
    private String title;
    private String content;
    private String richText;
    private String tags;
    private Long revision;
}
