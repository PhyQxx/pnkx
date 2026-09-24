package com.pnkx.service;

/**
 * 提醒推送通道接口。
 * <p>
 * 定义在 pnkx-life(数据层),由 pnkx-admin(web 层)实现,
 * 以便实现类能访问 {@code WebSocketController} 进行实时站内推送。
 * 通过 Spring 自动装配，pnkx-life 的 service 无需直接依赖 pnkx-admin。
 *
 * @author PHY
 * @date 2026/07/02
 */
public interface ReminderPushChannel {

    /**
     * 向指定用户推送一条实时提醒（WebSocket 单点消息）。
     * <p>
     * 仅当用户当前在线时送达；离线时静默跳过（由邮件渠道兜底）。
     *
     * @param userName 用户名（WebSocket 连接 key）
     * @param payload  消息内容（JSON 字符串）
     */
    void push(String userName, String payload);

    default void pushWebSocket(String userName, String payload) { push(userName, payload); }

    default void pushApp(String userName, String payload) { }

    /** App 推送是否已具备服务端配置；未启用时不应记录为成功投递。 */
    default boolean isAppPushEnabled() { return false; }
}
