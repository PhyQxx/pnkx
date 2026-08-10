package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class PxLifeReportHistory {
    private Long id;
    private String userId;
    private String period;
    private String reportType;
    private String source;
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
