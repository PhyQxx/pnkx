package com.pnkx.web.websocket.controller;

import com.pnkx.service.ReminderPushChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 提醒推送通道的 WebSocket 实现。
 * <p>
 * 位于 pnkx-admin 模块以访问 {@link WebSocketController}，
 * 实现 pnkx-life 中定义的 {@link ReminderPushChannel} 接口，
 * 由 Spring 自动装配到提醒 Service。
 *
 * @author PHY
 * @date 2026/07/02
 */
@Component("webSocketReminderPushChannel")
public class WebSocketReminderPushChannel implements ReminderPushChannel {

    private static final Logger log = LoggerFactory.getLogger(WebSocketReminderPushChannel.class);

    private final WebSocketController webSocketController;

    public WebSocketReminderPushChannel(WebSocketController webSocketController) {
        this.webSocketController = webSocketController;
    }

    @Override
    public void push(String userName, String payload) {
        if (userName == null || userName.isEmpty()) {
            return;
        }
        log.info("【提醒推送】向用户 {} 发送实时提醒", userName);
        webSocketController.sendOneMessage(userName, payload);
    }
}
