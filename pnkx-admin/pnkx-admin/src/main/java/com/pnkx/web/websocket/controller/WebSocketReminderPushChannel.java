package com.pnkx.web.websocket.controller;

import com.pnkx.service.ReminderPushChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

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

    @Resource
    private com.pnkx.system.service.impl.UniPushService uniPushService;

    @Override
    public void push(String userName, String payload) {
        if (userName == null || userName.isEmpty()) {
            return;
        }
        log.info("【提醒推送】向用户 {} 发送实时提醒", userName);
        pushWebSocket(userName, payload);
        if (isAppPushEnabled()) {
            try {
                pushApp(userName, payload);
            } catch (Exception e) {
                // 聚合推送至少保证 WebSocket 已送达；统一提醒引擎会直接调用 pushApp 并记录可重试失败。
                log.warn("【提醒推送】App 通道投递失败, user={}", userName, e);
            }
        }
    }

    @Override
    public void pushWebSocket(String userName, String payload) {
        webSocketController.sendOneMessage(userName, payload);
    }

    @Override
    public void pushApp(String userName, String payload) {
        try {
            com.alibaba.fastjson.JSONObject msg = com.alibaba.fastjson.JSON.parseObject(payload);
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("type", msg.getString("type"));
            data.put("sourceType", msg.getString("sourceType"));
            data.put("sourceId", msg.getString("sourceId"));
            uniPushService.sendToUser(userName, msg.getString("title"), msg.getString("content"), data);
        } catch (Exception e) {
            log.warn("【提醒推送】离线推送失败, user={}", userName, e);
            // 交由提醒服务记录失败状态，使通知中心能够展示并重试。
            throw new IllegalStateException("UniPush 推送失败", e);
        }
    }

    @Override
    public boolean isAppPushEnabled() {
        return uniPushService.enabled();
    }
}
