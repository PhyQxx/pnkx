package com.pnkx.life.service.workspace;
import lombok.Data;
@Data
public class OrganizeSuggestion {
    private Long id;
    private String ownerId;
    private String resourceType;
    private Long resourceId;
    private String ruleCode;
    private String fingerprint;
    private String title;
    private String reason;
    private String proposedTitle;
    private String status;
}
