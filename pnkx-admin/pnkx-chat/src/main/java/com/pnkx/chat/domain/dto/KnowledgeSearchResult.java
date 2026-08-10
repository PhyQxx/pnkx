package com.pnkx.chat.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class KnowledgeSearchResult {
    private String sourceType; // custom_reply, blog, message
    private Long sourceId;
    private String title;
    private String content;
    private BigDecimal score;
}
