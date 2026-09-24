package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxLifeReminder;
import com.pnkx.domain.po.PxToDo;
import com.pnkx.domain.po.PxReminderPreference;
import com.pnkx.service.AiLifeReminderDataService;
import com.pnkx.service.IPxLifeReminderService;
import com.pnkx.service.IPxToDoService;
import com.pnkx.service.WxSubscribeMessageService;
import com.pnkx.service.CalendarAggregateService;
import com.pnkx.system.service.ISysConfigService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import com.pnkx.common.exception.ServiceException;

/**
 * @author PHY
 * @classname PxReminderController
 * @date 2026/05/25
 * @description 统一提醒中心：提醒配置管理 + 今日提醒聚合 + 通知中心
 */
@RestController
@RequestMapping("/reminder")
public class PxReminderController extends BaseController {

    @Resource
    private AiLifeReminderDataService reminderDataService;

    @Resource
    private IPxLifeReminderService lifeReminderService;
    @Resource
    private WxSubscribeMessageService wxSubscribeMessageService;
    @Resource
    private IPxToDoService pxToDoService;
    @Resource
    private ISysConfigService configService;
    @Resource
    private CalendarAggregateService calendarAggregateService;
    @Value("${wx.subscribe.templates.commemoration:}")
    private String commemorationTemplateId;
    @Value("${wx.subscribe.templates.menstruation:}")
    private String menstruationTemplateId;

    // ==================== 今日提醒聚合（既有，保留兼容） ====================

    /**
     * 获取今日提醒聚合数据（纪念日 / 情侣卡 / 经期）。
     * <p>
     * 同时附带今日待办与经期配置，作为小程序首页“今日仪表盘”的唯一数据出口，
     * 避免首页并发多次拉取经期/待办等重复数据。
     */
    @GetMapping("/today")
    public AjaxResult getTodayReminders() {
        String userId = SecurityUtils.getUserId();
        JSONObject result = new JSONObject();
        result.put("commemorationDays", reminderDataService.buildReminderData(userId, "commemoration"));
        result.put("loversCards", reminderDataService.buildReminderData(userId, "lovers_card"));
        result.put("menstruation", reminderDataService.buildReminderData(userId, "menstruation"));

        // 今日待办（与小程序首页 getTodayToDoList 语义一致：执行者为当前用户、未完成、未过期）
        PxToDo pxToDo = new PxToDo();
        pxToDo.setPerformer(userId);
        pxToDo.setStatus(false);
        pxToDo.setPlanEndTime(DateUtils.getTime());
        result.put("todo", pxToDoService.selectPxToDoList(pxToDo));

        // 姨妈助手设置（cycle 周期 / duration 经期 / state 状态：zjjq 经期/why 怀孕）
        JSONObject menstruationAssistantSetting = new JSONObject();
        menstruationAssistantSetting.put("cycle", configService.selectConfigByKey("ymzq"));
        menstruationAssistantSetting.put("duration", configService.selectConfigByKey("ymsc"));
        menstruationAssistantSetting.put("state", configService.selectConfigByKey("ymdqzt"));
        result.put("menstruationAssistantSetting", menstruationAssistantSetting);
        result.put("timeline", calendarAggregateService.getTodayCockpit(userId));
        return AjaxResult.success(result);
    }

    // ==================== 提醒配置管理 ====================

    /**
     * 查询提醒配置列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxLifeReminder pxLifeReminder) {
        pxLifeReminder.setUserId(SecurityUtils.getUserId());
        startPage();
        List<PxLifeReminder> list = lifeReminderService.selectPxLifeReminderList(pxLifeReminder);
        return getDataTable(list);
    }

    /**
     * 获取提醒配置详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(requireReminderOwner(id));
    }

    /**
     * 新增提醒配置
     */
    @Log(title = "提醒配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxLifeReminder pxLifeReminder) {
        pxLifeReminder.setUserId(SecurityUtils.getUserId());
        pxLifeReminder.setCreateBy(SecurityUtils.getUserId());
        return toAjax(lifeReminderService.insertPxLifeReminder(pxLifeReminder));
    }

    /**
     * 修改提醒配置
     */
    @Log(title = "提醒配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxLifeReminder pxLifeReminder) {
        requireReminderOwner(pxLifeReminder.getId());
        pxLifeReminder.setUserId(SecurityUtils.getUserId());
        pxLifeReminder.setUpdateBy(SecurityUtils.getUserId());
        return toAjax(lifeReminderService.updatePxLifeReminder(pxLifeReminder));
    }

    /**
     * 删除提醒配置
     */
    @Log(title = "提醒配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        for (Long id : ids) requireReminderOwner(id);
        return toAjax(lifeReminderService.deletePxLifeReminderByIds(ids));
    }

    // ==================== 绑定 / 解绑（按来源实体） ====================

    /**
     * 给来源实体绑定/更新提醒（upsert）。
     * <p>
     * Body 需含：sourceType, sourceId, userId, eventTime(来源事件时间), leadMinutes(提前量)。
     */
    @Log(title = "提醒绑定", businessType = BusinessType.INSERT)
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody PxLifeReminder pxLifeReminder) {
        pxLifeReminder.setUserId(SecurityUtils.getUserId());
        pxLifeReminder.setCreateBy(SecurityUtils.getUserId());
        if (pxLifeReminder.getEnabled() == null) {
            pxLifeReminder.setEnabled(true);
        }
        return toAjax(lifeReminderService.bindReminder(pxLifeReminder));
    }

    /**
     * 按来源实体解绑提醒
     */
    @Log(title = "提醒解绑", businessType = BusinessType.DELETE)
    @DeleteMapping("/unbind")
    public AjaxResult unbind(@RequestParam("sourceType") String sourceType,
                             @RequestParam("sourceId") Long sourceId) {
        return toAjax(lifeReminderService.unbindReminder(sourceType, sourceId, SecurityUtils.getUserId()));
    }

    // ==================== 通知中心 ====================

    /**
     * 查询当前用户的通知列表
     */
    @GetMapping("/notifications")
    public AjaxResult notifications() {
        return AjaxResult.success(lifeReminderService.selectNotifications(SecurityUtils.getUserId()));
    }

    /**
     * 统计当前用户未读通知数（顶部铃铛用）
     */
    @GetMapping("/unread/count")
    public AjaxResult unreadCount() {
        return AjaxResult.success(lifeReminderService.countUnread(SecurityUtils.getUserId()));
    }

    /**
     * 标记已读（ids 为空则全部已读）
     */
    @PutMapping("/notifications/read")
    public AjaxResult markRead(@RequestBody(required = false) Long[] ids) {
        return toAjax(lifeReminderService.markRead(SecurityUtils.getUserId(), ids));
    }

    @DeleteMapping("/notifications/{id}")
    public AjaxResult deleteNotification(@PathVariable Long id) {
        return toAjax(lifeReminderService.deleteNotification(SecurityUtils.getUserId(), id));
    }

    @PostMapping("/notifications/{id}/retry")
    public AjaxResult retryNotification(@PathVariable Long id) {
        return toAjax(lifeReminderService.retryNotification(SecurityUtils.getUserId(), id));
    }

    @GetMapping("/preference")
    public AjaxResult preference() {
        return AjaxResult.success(lifeReminderService.getPreference(SecurityUtils.getUserId()));
    }

    @PutMapping("/preference")
    public AjaxResult savePreference(@RequestBody PxReminderPreference preference) {
        preference.setUserId(SecurityUtils.getUserId());
        return toAjax(lifeReminderService.savePreference(preference));
    }

    @GetMapping("/wechat-subscription")
    public AjaxResult wechatSubscription() {
        return AjaxResult.success(wxSubscribeMessageService.getChoices(Long.valueOf(SecurityUtils.getUserId())));
    }

    @GetMapping("/wechat-template-ids")
    public AjaxResult wechatTemplateIds() {
        JSONObject ids = new JSONObject();
        ids.put("commemoration", commemorationTemplateId);
        ids.put("menstruation", menstruationTemplateId);
        return AjaxResult.success(ids);
    }

    @PostMapping("/wechat-subscription")
    public AjaxResult saveWechatSubscription(@RequestBody JSONObject body) {
        String templateType = body.getString("templateType");
        boolean accepted = body.getBooleanValue("accepted");
        wxSubscribeMessageService.saveChoice(Long.valueOf(SecurityUtils.getUserId()), templateType, accepted);
        return AjaxResult.success();
    }

    private PxLifeReminder requireReminderOwner(Long id) {
        PxLifeReminder reminder = id == null ? null : lifeReminderService.selectPxLifeReminderById(id);
        if (reminder == null || !Objects.equals(SecurityUtils.getUserId(), reminder.getUserId())) {
            throw new ServiceException("提醒不存在或无权访问");
        }
        return reminder;
    }
}
