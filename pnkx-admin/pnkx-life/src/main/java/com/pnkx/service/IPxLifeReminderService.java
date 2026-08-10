package com.pnkx.service;

import com.pnkx.domain.po.PxLifeNotification;
import com.pnkx.domain.po.PxLifeReminder;

import java.util.List;

/**
 * @author PHY
 * @classname IPxLifeReminderService
 * @date 2026/07/02
 * @description 统一提醒引擎 Service 接口
 */
public interface IPxLifeReminderService {

    /**
     * 查询提醒配置
     *
     * @param id 主键
     * @return 提醒配置
     */
    PxLifeReminder selectPxLifeReminderById(Long id);

    /**
     * 查询提醒配置列表
     *
     * @param pxLifeReminder 提醒配置
     * @return 提醒配置集合
     */
    List<PxLifeReminder> selectPxLifeReminderList(PxLifeReminder pxLifeReminder);

    /**
     * 新增提醒配置
     *
     * @param pxLifeReminder 提醒配置
     * @return 结果
     */
    int insertPxLifeReminder(PxLifeReminder pxLifeReminder);

    /**
     * 修改提醒配置
     *
     * @param pxLifeReminder 提醒配置
     * @return 结果
     */
    int updatePxLifeReminder(PxLifeReminder pxLifeReminder);

    /**
     * 批量删除提醒配置
     *
     * @param ids 主键数组
     * @return 结果
     */
    int deletePxLifeReminderByIds(Long[] ids);

    /**
     * 删除提醒配置
     *
     * @param id 主键
     * @return 结果
     */
    int deletePxLifeReminderById(Long id);

    /**
     * 给来源实体绑定/更新提醒（upsert 语义）。
     * <p>
     * 若同 source_type + source_id + user_id 已存在则更新，否则新增。
     * 同时根据 leadMinutes 重算 remindTime。
     *
     * @param pxLifeReminder 提醒配置（需含 sourceType/sourceId/userId/eventTime/leadMinutes）
     * @return 结果
     */
    int bindReminder(PxLifeReminder pxLifeReminder);

    /**
     * 按来源实体解绑提醒
     *
     * @param sourceType 来源类型
     * @param sourceId   来源实体ID
     * @return 结果
     */
    int unbindReminder(String sourceType, Long sourceId);

    /**
     * 查询用户的通知列表（通知中心）
     *
     * @param userId 用户ID
     * @return 通知集合
     */
    List<PxLifeNotification> selectNotifications(String userId);

    /**
     * 统计用户未读通知数
     *
     * @param userId 用户ID
     * @return 未读数
     */
    int countUnread(String userId);

    /**
     * 标记已读
     *
     * @param userId 用户ID
     * @param ids    通知ID数组（为空则全部标记已读）
     * @return 结果
     */
    int markRead(String userId, Long[] ids);

    int deleteNotification(String userId, Long id);

    /**
     * 调度入口：扫描到期提醒并分发投递（WebSocket + 邮件），记录投递日志防重发。
     * <p>
     * 由 Quartz 定时任务调用，建议每 5 分钟一次。
     *
     * @return 本次触发的提醒数
     */
    int dispatchReminders();
}
