package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class PxAutomationRule extends BaseEntity {
    private Long id;
    private String clientUuid;
    private String name;
    private String templateCode;
    private String triggerType;
    private String triggerConfig;
    private String conditionJson;
    private String actionType;
    private String actionConfig;
    private Boolean enabled;
    private String version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date lastRunTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date nextRunTime;
    private String delFlag;
}
