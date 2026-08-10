package com.pnkx.strategy;

/**
 * 消息发送策略接口
 * 定义四种消息发送动作的统一接口
 */
public interface MessageSendStrategy {
    
    /**
     * 发送消息
     * @param targetId 目标ID（用户ID、频道ID或消息ID）
     * @param content 消息内容
     * @param contentType 消息类型（text/plain, text/markdown, vocechat/file）
     * @return 发送结果
     */
    String send(String targetId, String content, String contentType);
    
    /**
     * 获取策略类型
     * @return 策略类型
     */
    StrategyType getType();
    
    /**
     * 策略类型枚举
     */
    enum StrategyType {
        WELCOME_NEW_USER,    // 欢迎新用户
        SEND_TO_USER,        // 向特定用户发消息（私聊）
        SEND_TO_GROUP,       // 向特定频道发消息（群聊）
        REPLY_TO_MESSAGE     // 回复特定消息
    }
}
