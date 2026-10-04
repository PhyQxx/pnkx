package com.pnkx.life.service;

import com.pnkx.life.domain.po.PxWxSubscription;

import java.util.List;

public interface WxSubscribeMessageService {
    void saveChoice(Long userId, String templateType, boolean accepted);
    List<PxWxSubscription> getChoices(Long userId);
    void dispatchDailyReminders();
}
