package com.pnkx.service;

import com.pnkx.domain.po.WebhookEvent;

/**
 * 消息发送服务接口
 * 封装策略模式的使用，提供统一的消息发送接口
 */
public interface IPxMessageSendService {

    /**
     * 欢迎新用户
     * @param userId 新用户ID
     * @return 发送结果
     */
    String welcomeNewUser(String userId);

    /**
     * 向特定用户发送消息（私聊）
     * @param userId 用户ID
     * @param content 消息内容
     * @param contentType 消息类型
     * @return 发送结果
     */
    String sendToUser(String userId, String content, String contentType);

    /**
     * 向特定频道发送消息（群聊）
     * @param groupId 频道ID
     * @param content 消息内容
     * @param contentType 消息类型
     * @return 发送结果
     */
    String sendToGroup(String groupId, String content, String contentType);

    /**
     * 回复特定消息
     * @param messageId 消息ID
     * @param content 回复内容
     * @param contentType 消息类型
     * @return 发送结果
     */
    String replyToMessage(String messageId, String content, String contentType);

    /**
     * 发送文本消息到用户（简化方法）
     * @param userId 用户ID
     * @param content 文本内容
     * @return 发送结果
     */
    String sendTextToUser(String userId, String content);

    /**
     * 发送Markdown消息到用户（简化方法）
     * @param userId 用户ID
     * @param content Markdown内容
     * @return 发送结果
     */
    String sendMarkdownToUser(String userId, String content);

    /**
     * 发送文本消息到频道（简化方法）
     * @param groupId 频道ID
     * @param content 文本内容
     * @return 发送结果
     */
    String sendTextToGroup(String groupId, String content);

    /**
     * 发送Markdown消息到频道（简化方法）
     * @param groupId 频道ID
     * @param content Markdown内容
     * @return 发送结果
     */
    String sendMarkdownToGroup(String groupId, String content);

    /**
     * 回复特定事件触发的消息
     * @param event 触发事件
     * @param replyContent 回复内容
     */
    void reply(WebhookEvent event, String replyContent);
}
