package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxLifeNotification;
import com.pnkx.domain.po.PxLifeReminder;
import com.pnkx.domain.po.PxCommemorationDay;
import com.pnkx.domain.po.PxToDo;
import com.pnkx.domain.po.PxSubscription;
import com.pnkx.domain.po.PxBookkeepingAccount;
import com.pnkx.domain.po.PxMenstruationRecord;
import com.pnkx.domain.po.PxReminderPreference;
import com.pnkx.mapper.PxCommemorationDayMapper;
import com.pnkx.mapper.PxLifeNotificationMapper;
import com.pnkx.mapper.PxLifeReminderMapper;
import com.pnkx.mapper.PxToDoMapper;
import com.pnkx.mapper.PxSubscriptionMapper;
import com.pnkx.mapper.PxBookkeepingAccountMapper;
import com.pnkx.mapper.PxMenstruationRecordMapper;
import com.pnkx.mapper.PxReminderPreferenceMapper;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.service.IPxLifeReminderService;
import com.pnkx.service.ReminderPushChannel;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.system.mapper.SysUserMapper;
import com.pnkx.system.service.ISysConfigService;
import com.pnkx.system.service.ISysEmailService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.time.LocalTime;

/**
 * @author PHY
 * @classname PxLifeReminderServiceImpl
 * @date 2026/07/02
 * @description 统一提醒引擎 Service 实现
 */
@Service
public class PxLifeReminderServiceImpl implements IPxLifeReminderService {

    private static final Logger log = LoggerFactory.getLogger(PxLifeReminderServiceImpl.class);

    @Resource
    private PxLifeReminderMapper pxLifeReminderMapper;
    @Resource
    private PxLifeNotificationMapper pxLifeNotificationMapper;
    @Resource
    private PxToDoMapper pxToDoMapper;
    @Resource
    private PxCommemorationDayMapper pxCommemorationDayMapper;
    @Resource
    private PxSubscriptionMapper pxSubscriptionMapper;
    @Resource
    private PxBookkeepingAccountMapper pxBookkeepingAccountMapper;
    @Resource
    private PxMenstruationRecordMapper pxMenstruationRecordMapper;
    @Resource
    private PxReminderPreferenceMapper pxReminderPreferenceMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private ISysConfigService sysConfigService;
    @Resource
    private ISysEmailService sysEmailService;

    /**
     * 推送通道（由 pnkx-admin 实现，可能为 null——如单元测试环境）。
     * 普通调度按可用能力跳过未配置通道；用户显式重试时则返回失败，避免误记为已发送。
     */
    @Autowired(required = false)
    @Qualifier("webSocketReminderPushChannel")
    private ReminderPushChannel pushChannel;

    // ==================== CRUD ====================

    @Override
    public PxLifeReminder selectPxLifeReminderById(Long id) {
        return pxLifeReminderMapper.selectPxLifeReminderById(id);
    }

    @DataScopeSelf
    @Override
    public List<PxLifeReminder> selectPxLifeReminderList(PxLifeReminder pxLifeReminder) {
        return pxLifeReminderMapper.selectPxLifeReminderList(pxLifeReminder);
    }


    @Override
    public int insertPxLifeReminder(PxLifeReminder pxLifeReminder) {
        pxLifeReminder.setCreateTime(DateUtils.getNowDate());
        return pxLifeReminderMapper.insertPxLifeReminder(pxLifeReminder);
    }

    @Override
    public int updatePxLifeReminder(PxLifeReminder pxLifeReminder) {
        pxLifeReminder.setUpdateTime(DateUtils.getNowDate());
        return pxLifeReminderMapper.updatePxLifeReminder(pxLifeReminder);
    }

    @Override
    public int deletePxLifeReminderByIds(Long[] ids) {
        return pxLifeReminderMapper.deletePxLifeReminderByIds(ids);
    }

    @Override
    public int deletePxLifeReminderById(Long id) {
        return pxLifeReminderMapper.deletePxLifeReminderById(id);
    }

    // ==================== 绑定 / 解绑 ====================

    @Override
    public int bindReminder(PxLifeReminder reminder) {
        validateReminderSource(reminder);
        PxLifeReminder existing = pxLifeReminderMapper.selectBySource(
                reminder.getSourceType(), reminder.getSourceId(), reminder.getUserId());
        reminder.setUpdateTime(DateUtils.getNowDate());
        if (existing != null) {
            reminder.setId(existing.getId());
            return pxLifeReminderMapper.updatePxLifeReminder(reminder);
        }
        reminder.setCreateTime(DateUtils.getNowDate());
        return pxLifeReminderMapper.insertPxLifeReminder(reminder);
    }

    private void validateReminderSource(PxLifeReminder reminder) {
        if (reminder == null || reminder.getSourceId() == null || reminder.getRemindTime() == null
                || !Arrays.asList("todo", "commemoration", "menstruation", "subscription").contains(reminder.getSourceType())) {
            throw new ServiceException("提醒来源或提醒时间无效");
        }
        boolean owned;
        switch (reminder.getSourceType()) {
            case "todo":
                PxToDo todo = pxToDoMapper.selectPxToDoById(reminder.getSourceId());
                owned = todo != null && (Objects.equals(reminder.getUserId(), todo.getCreateBy())
                        || containsUser(todo.getPerformer(), reminder.getUserId()));
                break;
            case "commemoration":
                PxCommemorationDay day = pxCommemorationDayMapper.selectPxCommemorationDayById(reminder.getSourceId());
                owned = day != null && Objects.equals(reminder.getUserId(), day.getCreateBy());
                break;
            case "subscription":
                PxSubscription subscription = pxSubscriptionMapper.selectPxSubscriptionById(reminder.getSourceId());
                owned = subscription != null && Objects.equals(reminder.getUserId(), subscription.getCreateBy());
                break;
            case "menstruation":
                PxMenstruationRecord record = pxMenstruationRecordMapper.selectPxMenstruationRecordById(reminder.getSourceId());
                owned = record != null && (Objects.equals(reminder.getUserId(), record.getCreateBy())
                        || Objects.equals(reminder.getUserId(), String.valueOf(record.getUserId())));
                break;
            default:
                owned = false;
        }
        if (!owned) throw new ServiceException("提醒来源不存在或不属于当前用户");
    }

    private boolean containsUser(String csv, String userId) {
        if (csv == null || userId == null) return false;
        return Arrays.stream(csv.split(",")).map(String::trim).anyMatch(userId::equals);
    }

    @Override
    public int unbindReminder(String sourceType, Long sourceId, String userId) {
        PxLifeReminder reminder = pxLifeReminderMapper.selectBySource(sourceType, sourceId, userId);
        return reminder == null ? 0 : pxLifeReminderMapper.deletePxLifeReminderById(reminder.getId());
    }

    // ==================== 通知中心 ====================

    @Override
    public List<PxLifeNotification> selectNotifications(String userId) {
        return pxLifeNotificationMapper.selectByUser(userId, null);
    }

    @Override
    public int countUnread(String userId) {
        return pxLifeNotificationMapper.countUnread(userId);
    }

    @Override
    public int markRead(String userId, Long[] ids) {
        return pxLifeNotificationMapper.markRead(userId, ids);
    }

    @Override
    public int deleteNotification(String userId, Long id) {
        return pxLifeNotificationMapper.deleteByUser(userId, id);
    }

    @Override
    public int retryNotification(String userId, Long id) {
        PxLifeNotification notification = pxLifeNotificationMapper.selectByIdForUser(userId, id);
        if (notification == null || !"1".equals(notification.getStatus())) {
            throw new ServiceException("仅发送失败的通知可以重试");
        }
        try {
            if ("email".equals(notification.getChannel())) {
                SysUser user = sysUserMapper.selectUserById(Long.valueOf(userId));
                if (user == null || StringUtils.isEmpty(user.getEmail())) throw new ServiceException("未绑定邮箱");
                SysEmail email = new SysEmail();
                email.setReceiverEmail(user.getEmail());
                email.setSubject(notification.getTitle());
                email.setContent(notification.getContent());
                sysEmailService.sendMail(email);
            } else {
                if (pushChannel == null) {
                    throw new ServiceException("推送通道不可用");
                }
                JSONObject payload = new JSONObject();
                payload.put("type", "life_reminder");
                payload.put("sourceType", notification.getSourceType());
                payload.put("sourceId", notification.getSourceId());
                payload.put("title", notification.getTitle());
                payload.put("content", notification.getContent());
                if ("unipush".equals(notification.getChannel())) {
                    if (!pushChannel.isAppPushEnabled()) throw new ServiceException("uniPush 未启用");
                    pushChannel.pushApp(userId, payload.toJSONString());
                } else if ("websocket".equals(notification.getChannel())) {
                    pushChannel.pushWebSocket(userId, payload.toJSONString());
                } else {
                    throw new ServiceException("不支持的通知渠道：" + notification.getChannel());
                }
            }
            return pxLifeNotificationMapper.updateStatus(userId, id, "0", DateUtils.getNowDate());
        } catch (Exception e) {
            pxLifeNotificationMapper.updateStatus(userId, id, "1", DateUtils.getNowDate());
            throw new ServiceException("重试失败：" + e.getMessage());
        }
    }

    @Override
    public PxReminderPreference getPreference(String userId) {
        PxReminderPreference preference = pxReminderPreferenceMapper.selectByUserId(userId);
        if (preference != null) return preference;
        preference = new PxReminderPreference();
        preference.setUserId(userId);
        preference.setWebsocketEnabled(true);
        preference.setEmailEnabled(true);
        preference.setPushEnabled(true);
        preference.setQuietStart("23:00");
        preference.setQuietEnd("07:00");
        return preference;
    }

    @Override
    public int savePreference(PxReminderPreference preference) {
        return pxReminderPreferenceMapper.upsert(preference);
    }

    // ==================== 调度核心 ====================

    @Override
    public int dispatchReminders() {
        int stopDaysAfter = getIntConfig("sys.life.remind.stop.days.after", 3);
        Date now = DateUtils.getNowDate();
        List<PxLifeReminder> dueList = pxLifeReminderMapper.selectDueReminders(now, stopDaysAfter);
        if (dueList.isEmpty()) {
            return 0;
        }

        boolean wsEnabled = "true".equalsIgnoreCase(getConfig("sys.life.remind.channel.websocket", "true"));
        boolean emailEnabled = "true".equalsIgnoreCase(getConfig("sys.life.remind.channel.email", "true"));
        int triggered = 0;

        for (PxLifeReminder reminder : dueList) {
            try {
                ReminderContent rc = buildContent(reminder);
                if (rc == null) {
                    // 来源实体已不存在（被删除），跳过并标记
                    pxLifeReminderMapper.updateLastTriggeredTime(reminder.getId(), now);
                    continue;
                }
                if (isQuiet(getPreference(reminder.getUserId()), LocalTime.now())) {
                    continue;
                }
                deliver(reminder, rc, wsEnabled, emailEnabled, now);
                pxLifeReminderMapper.updateLastTriggeredTime(reminder.getId(), now);
                triggered++;
            } catch (Exception e) {
                log.error("分发提醒失败 reminderId={} sourceType={} sourceId={}",
                        reminder.getId(), reminder.getSourceType(), reminder.getSourceId(), e);
            }
        }
        log.info("提醒调度完成，共触发 {} 条提醒", triggered);
        return triggered;
    }

    /**
     * 分发单条提醒到各渠道并记录投递日志。
     */
    private void deliver(PxLifeReminder reminder, ReminderContent rc,
                         boolean wsEnabled, boolean emailEnabled, Date now) {
        PxReminderPreference preference = getPreference(reminder.getUserId());
        if (isQuiet(preference, LocalTime.now())) {
            return;
        }
        // 1. WebSocket 站内推送
        if (wsEnabled && Boolean.TRUE.equals(preference.getWebsocketEnabled()) && pushChannel != null) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", "life_reminder");
                payload.put("sourceType", reminder.getSourceType());
                payload.put("sourceId", reminder.getSourceId());
                payload.put("title", rc.title);
                payload.put("content", rc.content);
                payload.put("sendTime", DateUtils.dateTimeNow());
                // WebSocket key 为用户名（userName），userId 在本系统即存储用户名
                pushChannel.pushWebSocket(reminder.getUserId(), payload.toJSONString());
                recordNotification(reminder, "websocket", rc, "0", now);
            } catch (Exception e) {
                log.error("WebSocket 推送失败 userId={}", reminder.getUserId(), e);
            }
        }

        if (Boolean.TRUE.equals(preference.getPushEnabled()) && pushChannel != null
                && pushChannel.isAppPushEnabled()) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", "life_reminder");
                payload.put("sourceType", reminder.getSourceType());
                payload.put("sourceId", reminder.getSourceId());
                payload.put("title", rc.title);
                payload.put("content", rc.content);
                pushChannel.pushApp(reminder.getUserId(), payload.toJSONString());
                recordNotification(reminder, "unipush", rc, "0", now);
            } catch (Exception e) {
                recordNotification(reminder, "unipush", rc, "1", now);
            }
        }

        // 2. 邮件推送
        if (emailEnabled && Boolean.TRUE.equals(preference.getEmailEnabled())) {
            try {
                SysUser user = sysUserMapper.selectUserById(Long.valueOf(reminder.getUserId()));
                if (user != null && StringUtils.isNotEmpty(user.getEmail())) {
                    SysEmail email = new SysEmail();
                    email.setReceiverEmail(user.getEmail());
                    email.setSubject(rc.title);
                    email.setContent(rc.content);
                    sysEmailService.sendMail(email);
                    recordNotification(reminder, "email", rc, "0", now);
                }
            } catch (Exception e) {
                log.error("邮件推送失败 userId={}", reminder.getUserId(), e);
                recordNotification(reminder, "email", rc, "1", now);
            }
        }
    }

    static boolean isQuiet(PxReminderPreference preference, LocalTime now) {
        if (preference == null || preference.getQuietStart() == null || preference.getQuietEnd() == null) return false;
        try {
            LocalTime start = LocalTime.parse(preference.getQuietStart());
            LocalTime end = LocalTime.parse(preference.getQuietEnd());
            if (start.equals(end)) return false;
            return start.isBefore(end)
                    ? !now.isBefore(start) && now.isBefore(end)
                    : !now.isBefore(start) || now.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 记录投递日志（防重发依据 + 通知中心历史）
     */
    private void recordNotification(PxLifeReminder reminder, String channel,
                                    ReminderContent rc, String status, Date now) {
        PxLifeNotification n = new PxLifeNotification();
        n.setReminderId(reminder.getId());
        n.setUserId(reminder.getUserId());
        n.setChannel(channel);
        n.setSourceType(reminder.getSourceType());
        n.setSourceId(reminder.getSourceId());
        n.setTitle(rc.title);
        n.setContent(rc.content);
        n.setSendTime(now);
        n.setStatus(status);
        n.setCreateTime(now);
        pxLifeNotificationMapper.insertPxLifeNotification(n);
    }

    /**
     * 根据来源类型读取实体，构建通知标题与内容。
     *
     * @return 实体不存在时返回 null
     */
    private ReminderContent buildContent(PxLifeReminder reminder) {
        String type = reminder.getSourceType();
        switch (type == null ? "" : type) {
            case "todo": {
                PxToDo todo = pxToDoMapper.selectPxToDoById(reminder.getSourceId());
                if (todo == null) return null;
                return new ReminderContent(
                        "👉 叮咚！待办提醒：" + todo.getContent(),
                        "您有一条待办即将到期：" + todo.getContent());
            }
            case "commemoration": {
                PxCommemorationDay day = pxCommemorationDayMapper.selectPxCommemorationDayById(reminder.getSourceId());
                if (day == null) return null;
                return new ReminderContent(
                        "🎉 叮咚！纪念日提醒：" + day.getName(),
                        "纪念日「" + day.getName() + "」即将到来");
            }
            case "menstruation": {
                return new ReminderContent(
                        "💗 叮咚！经期提醒",
                        "根据周期预测，经期即将到来，请注意");
            }
            case "subscription": {
                PxSubscription subscription = pxSubscriptionMapper.selectPxSubscriptionById(reminder.getSourceId());
                if (subscription == null) return null;
                String date = subscription.getNextPaymentDate() == null
                        ? "近期"
                        : DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, subscription.getNextPaymentDate());
                PxBookkeepingAccount account = subscription.getAccountId() == null
                        ? null : pxBookkeepingAccountMapper.selectPxBookkeepingAccountById(subscription.getAccountId());
                String accountText = account == null ? "" : "，账户 " + account.getAccountName();
                return new ReminderContent(
                        "💳 订阅续费提醒：" + subscription.getName(),
                        "订阅「" + subscription.getName() + "」将在 " + date
                                + " 扣费" + (subscription.getAmount() == null ? "" : "，金额 " + subscription.getAmount())
                                + accountText);
            }
            default:
                return new ReminderContent("叮咚！生活提醒", "您有一条新的提醒");
        }
    }

    private int getIntConfig(String key, int defaultValue) {
        String val = sysConfigService.selectConfigByKey(key);
        if (StringUtils.isNotEmpty(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.error("配置 {} 格式错误: {}", key, val);
            }
        }
        return defaultValue;
    }

    private String getConfig(String key, String defaultValue) {
        String val = sysConfigService.selectConfigByKey(key);
        return StringUtils.isNotEmpty(val) ? val : defaultValue;
    }

    /**
     * 通知内容内部载体
     */
    private static class ReminderContent {
        final String title;
        final String content;

        ReminderContent(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
