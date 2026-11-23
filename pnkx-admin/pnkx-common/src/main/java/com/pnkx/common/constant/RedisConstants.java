package com.pnkx.common.constant;

/**
 * RedisConstants
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 16:29
 * @description Redis缓存常量
 */
public class RedisConstants {

    /**
     * 聊天室人员
     */
    public static final String PX_CHAT_MEMBER = "pxChatMember";

    /**
     * 聊天室消息
     */
    public static final String PX_CHAT_MESSAGE = "pxChatMessage";

    /**
     * AI 对话历史（Webhook）
     */
    public static final String AI_CHAT_HISTORY = "AI_CHAT_HISTORY:";

    /**
     * AI 响应缓存
     */
    public static final String AI_RESPONSE_CACHE = "AI_RESPONSE_CACHE:";

    /**
     * AI 待确认操作
     */
    public static final String AI_PENDING_ACTION = "AI_PENDING_ACTION:";

    /**
     * 工具箱：AI 创意文案 IP 限流
     */
    public static final String TOOL_CREATIVE_RATE_LIMIT = "TOOL_CREATIVE_RATE_LIMIT:";

    /**
     * 工具箱：汇率缓存（后缀拼接基准币种，backup 后缀为 7 天兜底副本）
     */
    public static final String TOOL_EXRATE_CACHE = "TOOL_EXRATE_CACHE:";

    /**
     * 工具箱：汇率查询 IP 限流
     */
    public static final String TOOL_EXRATE_RATE_LIMIT = "TOOL_EXRATE_RATE_LIMIT:";

    /**
     * 工具箱：使用打点 IP 限流
     */
    public static final String TOOL_USE_LOG_RATE_LIMIT = "TOOL_USE_LOG_RATE_LIMIT:";
}
