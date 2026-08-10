package com.pnkx.domain.po;

import lombok.Data;

@Data
public class WebhookEvent {
    /**
     * 消息创建时间戳
     */
    private Long created_at;

    /**
     * 消息详情
     */
    private EventDetail detail;

    /**
     * 发送者用户ID
     */
    private Integer from_uid;

    /**
     * 消息ID
     */
    private Integer mid;

    /**
     * 目标（接收者）
     */
    private Target target;

    /**
     * 事件类型
     */
    private String type;

    /**
     * 用户ID
     */
    private Integer uid;

    /**
     * 用户名称（新用户事件使用）
     */
    private String name;

    /**
     * 用户语言（新用户事件使用）
     */
    private String language;

    /**
     * 用户性别（新用户事件使用）
     */
    private Integer gender;

    /**
     * 用户生日（新用户事件使用）
     */
    private String birthday;

    /**
     * 用户域名（新用户事件使用）
     */
    private String domain;
}
