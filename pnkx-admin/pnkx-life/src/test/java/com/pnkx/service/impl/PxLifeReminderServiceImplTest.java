package com.pnkx.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.domain.po.PxLifeNotification;
import com.pnkx.domain.po.PxReminderPreference;
import com.pnkx.mapper.PxLifeNotificationMapper;
import com.pnkx.service.ReminderPushChannel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PxLifeReminderServiceImplTest {

    @Test
    void quietHoursSupportOvernightWindow() {
        PxReminderPreference preference = preference("23:00", "07:00");
        assertTrue(PxLifeReminderServiceImpl.isQuiet(preference, LocalTime.of(23, 30)));
        assertTrue(PxLifeReminderServiceImpl.isQuiet(preference, LocalTime.of(6, 59)));
        assertFalse(PxLifeReminderServiceImpl.isQuiet(preference, LocalTime.of(12, 0)));
    }

    @Test
    void equalQuietBoundsDisableQuietMode() {
        assertFalse(PxLifeReminderServiceImpl.isQuiet(preference("08:00", "08:00"), LocalTime.of(8, 0)));
    }

    @Test
    void retryKeepsFailureWhenPushChannelIsUnavailable() throws Exception {
        AtomicReference<String> persistedStatus = new AtomicReference<>();
        PxLifeReminderServiceImpl service = serviceWithNotification("websocket", persistedStatus);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.retryNotification("7", 11L));

        assertTrue(error.getMessage().contains("推送通道不可用"));
        assertEquals("1", persistedStatus.get());
    }

    @Test
    void retryRejectsUnknownChannelInsteadOfMarkingSuccess() throws Exception {
        AtomicReference<String> persistedStatus = new AtomicReference<>();
        PxLifeReminderServiceImpl service = serviceWithNotification("sms", persistedStatus);
        setField(service, "pushChannel", (ReminderPushChannel) (userName, payload) -> { });

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.retryNotification("7", 11L));

        assertTrue(error.getMessage().contains("不支持的通知渠道"));
        assertEquals("1", persistedStatus.get());
    }

    private PxLifeReminderServiceImpl serviceWithNotification(String channel,
                                                                AtomicReference<String> persistedStatus)
            throws Exception {
        PxLifeNotification notification = new PxLifeNotification();
        notification.setId(11L);
        notification.setStatus("1");
        notification.setChannel(channel);
        notification.setSourceType("todo");
        notification.setSourceId(23L);
        notification.setTitle("提醒");
        notification.setContent("内容");

        PxLifeNotificationMapper mapper = (PxLifeNotificationMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{PxLifeNotificationMapper.class},
                (proxy, method, args) -> {
                    if ("selectByIdForUser".equals(method.getName())) return notification;
                    if ("updateStatus".equals(method.getName())) {
                        persistedStatus.set((String) args[2]);
                        return 1;
                    }
                    return null;
                });
        PxLifeReminderServiceImpl service = new PxLifeReminderServiceImpl();
        setField(service, "pxLifeNotificationMapper", mapper);
        return service;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private PxReminderPreference preference(String start, String end) {
        PxReminderPreference preference = new PxReminderPreference();
        preference.setQuietStart(start);
        preference.setQuietEnd(end);
        return preference;
    }
}
