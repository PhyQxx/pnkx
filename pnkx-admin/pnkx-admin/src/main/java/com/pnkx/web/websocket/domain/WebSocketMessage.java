package com.pnkx.web.websocket.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Notice
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 16:01
 * @description WebSocket消息实体
 */
@Data
public class WebSocketMessage {
    /**
     * 消息类型
     */
    private String webSocket;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 信息内容
     */
    private Object message;

    // 获取当前时间
    LocalDateTime now = LocalDateTime.now();
    // 定义日期时间格式
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 发送时间
     */
    private String sendTime = now.format(formatter);
}
