package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * @author PHY
 * @classname PxLifeReminder
 * @date 2026/07/02
 * @description 统一提醒配置对象（按实体挂载，支持 todo/commemoration/menstruation/subscription）
 */
public class PxLifeReminder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 来源类型（todo 待办 / commemoration 纪念日 / menstruation 经期 / subscription 订阅）
     */
    @Excel(name = "来源类型")
    private String sourceType;

    /**
     * 来源实体ID
     */
    @Excel(name = "来源实体ID")
    private Long sourceId;

    /**
     * 接收提醒的用户ID（userId）
     */
    @Excel(name = "接收用户ID")
    private String userId;

    /**
     * 提醒触发时间（已计算提前量后的绝对时间）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "提醒时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date remindTime;

    /**
     * 提前量（分钟），相对来源事件时间的提前分钟数
     */
    @Excel(name = "提前量(分钟)")
    private Integer leadMinutes;

    /**
     * 是否启用（1启用 0停用）
     */
    @Excel(name = "是否启用")
    private Boolean enabled;

    /**
     * 上次触发时间（防重发）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastTriggeredTime;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 客户端唯一标识（离线幂等去重）
     */
    private String clientUuid;

    /**
     * 删除标志（0存在 1删除）
     */
    private String delFlag;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setRemindTime(Date remindTime) {
        this.remindTime = remindTime;
    }

    public Date getRemindTime() {
        return remindTime;
    }

    public void setLeadMinutes(Integer leadMinutes) {
        this.leadMinutes = leadMinutes;
    }

    public Integer getLeadMinutes() {
        return leadMinutes;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setLastTriggeredTime(Date lastTriggeredTime) {
        this.lastTriggeredTime = lastTriggeredTime;
    }

    public Date getLastTriggeredTime() {
        return lastTriggeredTime;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDelFlag() {
        return delFlag;
    }
}
