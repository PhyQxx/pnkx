package com.pnkx.web.websocket.controller;

import com.pnkx.system.service.impl.UniPushService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketReminderPushChannelTest {
    @Test
    void appPushExposesAvailabilityAndPreservesDeepLinkPayload() throws Exception {
        AtomicReference<Map<String, String>> delivered = new AtomicReference<>();
        UniPushService pushService = new UniPushService() {
            @Override public boolean enabled() { return true; }
            @Override public int sendToUser(String userId, String title, String content,
                                            Map<String, String> payload) {
                delivered.set(payload);
                return 1;
            }
        };
        WebSocketReminderPushChannel channel = new WebSocketReminderPushChannel(null);
        Field field = WebSocketReminderPushChannel.class.getDeclaredField("uniPushService");
        field.setAccessible(true);
        field.set(channel, pushService);

        channel.pushApp("7", "{\"type\":\"life_reminder\",\"sourceType\":\"todo\","
                + "\"sourceId\":\"42\",\"title\":\"待办\",\"content\":\"测试\"}");

        assertTrue(channel.isAppPushEnabled());
        assertEquals("life_reminder", delivered.get().get("type"));
        assertEquals("todo", delivered.get().get("sourceType"));
        assertEquals("42", delivered.get().get("sourceId"));
    }
}
