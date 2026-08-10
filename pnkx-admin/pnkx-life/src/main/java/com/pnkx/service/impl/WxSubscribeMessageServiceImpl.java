package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.domain.po.PxWxSubscription;
import com.pnkx.mapper.PxWxSubscriptionMapper;
import com.pnkx.service.AiLifeReminderDataService;
import com.pnkx.service.WxSubscribeMessageService;
import com.pnkx.system.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

@Service
public class WxSubscribeMessageServiceImpl implements WxSubscribeMessageService {
    private static final Logger log = LoggerFactory.getLogger(WxSubscribeMessageServiceImpl.class);
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=";

    @Value("${wx.appid:}") private String appId;
    @Value("${wx.secret:}") private String appSecret;
    @Value("${wx.subscribe.templates.commemoration:}") private String commemorationTemplateId;
    @Value("${wx.subscribe.templates.menstruation:}") private String menstruationTemplateId;
    @Resource private PxWxSubscriptionMapper subscriptionMapper;
    @Resource private SysUserMapper userMapper;
    @Resource private AiLifeReminderDataService reminderDataService;

    private volatile String accessToken;
    private volatile long tokenExpiresAt;

    @Override
    public void saveChoice(Long userId, String templateType, boolean accepted) {
        if (!"commemoration".equals(templateType) && !"menstruation".equals(templateType)) {
            throw new IllegalArgumentException("不支持的订阅消息类型");
        }
        PxWxSubscription subscription = new PxWxSubscription();
        subscription.setUserId(userId);
        subscription.setTemplateType(templateType);
        subscription.setAccepted(accepted);
        subscriptionMapper.upsert(subscription);
    }

    @Override
    public List<PxWxSubscription> getChoices(Long userId) {
        return subscriptionMapper.selectByUserId(userId);
    }

    @Override
    public void dispatchDailyReminders() {
        for (PxWxSubscription subscription : subscriptionMapper.selectEnabled()) {
            try {
                SysUser user = userMapper.selectUserById(subscription.getUserId());
                if (user == null || user.getOpenid() == null || user.getOpenid().isBlank()) continue;
                JSONObject message = buildDueMessage(subscription, user.getOpenid());
                if (message == null) continue;
                JSONObject result = new RestTemplate().postForObject(SEND_URL + getAccessToken(), message, JSONObject.class);
                boolean success = result != null && result.getIntValue("errcode") == 0;
                subscriptionMapper.insertLog(subscription.getUserId(), subscription.getTemplateType(), success,
                        result == null ? "empty response" : result.toJSONString());
                if (success) subscriptionMapper.consume(subscription.getId());
            } catch (Exception e) {
                subscriptionMapper.insertLog(subscription.getUserId(), subscription.getTemplateType(), false, e.getMessage());
                log.error("微信订阅消息发送失败，userId={}, type={}", subscription.getUserId(), subscription.getTemplateType(), e);
            }
        }
    }

    private JSONObject buildDueMessage(PxWxSubscription subscription, String openid) {
        String type = subscription.getTemplateType();
        String templateId = "commemoration".equals(type) ? commemorationTemplateId : menstruationTemplateId;
        if (templateId == null || templateId.isBlank()) return null;
        JSONObject data = new JSONObject();
        if ("commemoration".equals(type)) {
            JSONArray upcoming = reminderDataService.buildReminderData(subscription.getUserId().toString(), "commemoration")
                    .getJSONArray("upcoming");
            JSONObject due = upcoming == null ? null : upcoming.stream().map(JSONObject.class::cast)
                    .filter(item -> item.getLongValue("daysLeft") == 0).findFirst().orElse(null);
            if (due == null) return null;
            data.put("thing1", value(due.getString("name")));
            data.put("date2", value(due.getString("date")));
            data.put("thing3", value("今天是重要的日子，记得准备惊喜"));
        } else {
            JSONObject reminder = reminderDataService.buildReminderData(subscription.getUserId().toString(), "menstruation");
            Integer days = reminder.getInteger("daysSinceLastStart");
            if (days == null || days < 27) return null;
            data.put("thing1", value("经期提醒"));
            data.put("date2", value(LocalDate.now().toString()));
            data.put("thing3", value("请提前做好准备，注意休息与保暖"));
        }
        JSONObject body = new JSONObject();
        body.put("touser", openid);
        body.put("template_id", templateId);
        body.put("page", "pages/index");
        body.put("miniprogram_state", "formal");
        body.put("lang", "zh_CN");
        body.put("data", data);
        return body;
    }

    private JSONObject value(String text) {
        return new JSONObject().fluentPut("value", text);
    }

    private synchronized String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt) return accessToken;
        JSONObject result = new RestTemplate().getForObject(String.format(TOKEN_URL, appId, appSecret), JSONObject.class);
        if (result == null || result.getString("access_token") == null) throw new IllegalStateException("获取微信 access_token 失败");
        accessToken = result.getString("access_token");
        tokenExpiresAt = System.currentTimeMillis() + Math.max(60, result.getLongValue("expires_in") - 120) * 1000L;
        return accessToken;
    }
}
