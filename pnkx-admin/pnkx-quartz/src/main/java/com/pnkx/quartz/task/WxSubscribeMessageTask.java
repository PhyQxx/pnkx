package com.pnkx.quartz.task;

import com.pnkx.service.WxSubscribeMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component("wxSubscribeMessageTask")
public class WxSubscribeMessageTask {
    @Resource private WxSubscribeMessageService subscribeMessageService;

    public void sendDaily() {
        subscribeMessageService.dispatchDailyReminders();
    }
}
