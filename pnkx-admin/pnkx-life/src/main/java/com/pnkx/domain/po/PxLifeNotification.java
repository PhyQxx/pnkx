package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * @author PHY
 * @classname PxLifeNotification
 * @date 2026/07/02
 * @description 提醒投递日志（防重发 + 历史记录）
 */
public class PxLifeNotification extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 关联提醒配置ID
     */
    private Long reminderId;

    /**
     * 接收用户ID
     */
    @Excel(name = "接收用户ID")
    private String userId;

    /**
     * 投递渠道（websocket / email）
     */
    @Excel(name = "投递渠道")
    private String channel;

    /**
     * 来源类型（冗余便于查询）
     */
    @Excel(name = "来源类型")
    private String sourceType;

    /**
     * 来源实体ID（冗余便于查询）
     */
    private Long sourceId;

    /**
     * 通知标题
     */
    @Excel(name = "通知标题")
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发送时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /**
     * 状态（0 已发送 1 发送失败 2 已读）
     */
    @Excel(name = "状态")
    private String status;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setReminderId(Long reminderId) {
        this.reminderId = reminderId;
    }

    public Long getReminderId() {
        return reminderId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
