package com.pnkx.service;

import com.alibaba.fastjson.JSONObject;

/**
 * AI生活提醒数据服务接口
 */
public interface AiLifeReminderDataService {
    /**
     * 构建提醒相关数据
     *
     * @param userId 用户ID
     * @param scene 场景: commemoration, lovers_card, menstruation
     * @return 数据JSON
     */
    JSONObject buildReminderData(String userId, String scene);

    /**
     * 一次性聚合全部提醒相关数据（纪念日 / 情侣卡 / 经期）。
     * <p>
     * 返回结构与首页待办聚合（{@code PxAdminController.getAllToDo}）兼容，
     * 用于替代分散在各 Controller 中的硬编码聚合逻辑，新增提醒类型只需改本方法一处。
     *
     * @param userId 用户ID
     * @return 含 commemoration / card / menstruation 三个字段的 JSON
     */
    JSONObject buildAllReminders(String userId);
}
