package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class PxAutomationExecution extends BaseEntity {
    private Long id;
    private Long ruleId;
    private String idempotencyKey;
    private String status;
    private Boolean dryRun;
    private String inputJson;
    private String planJson;
    private String resultJson;
    private String errorMsg;
    private Long retryOf;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date finishTime;
}
