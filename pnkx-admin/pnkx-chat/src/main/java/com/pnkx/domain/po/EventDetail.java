package com.pnkx.domain.po;

import lombok.Data;

@Data
public class EventDetail {
    /**
     * 一级消息类型
     * - normal: 新消息
     * - reaction: 对消息的响应（编辑、删除等）
     */
    private String type;
    
    /**
     * 消息内容（对于普通消息）
     */
    private String content;
    
    /**
     * 消息类型
     * - text/plain: 纯文本消息
     * - text/markdown: markdown消息
     * - vocechat/file: 文件类消息
     */
    private String content_type;
    
    /**
     * 消息过期时长，如果有大于0数字，说明该消息是个限时消息
     */
    private Integer expires_in;
    
    /**
     * 消息属性（元数据）
     */
    private Object properties;
    
    /**
     * 二级消息类型（对于reaction类型）
     * - edit: 编辑消息
     * - delete: 删除消息
     * - reply: 回复消息
     */
    private String subType;
    
    /**
     * 被操作的消息ID（对于reaction类型）
     */
    private Integer mid;
    
    /**
     * 详细内容（对于reaction类型）
     */
    private EventDetail detail;
}
