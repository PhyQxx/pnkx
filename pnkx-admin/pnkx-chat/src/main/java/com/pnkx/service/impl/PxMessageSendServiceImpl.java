package com.pnkx.service.impl;

import com.pnkx.domain.po.WebhookEvent;
import com.pnkx.service.IPxMessageSendService;
import com.pnkx.strategy.MessageSendStrategy;
import com.pnkx.strategy.MessageSendStrategyFactory;
import com.pnkx.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 消息发送服务
 * 封装策略模式的使用，提供统一的消息发送接口
 */
@Service
@Slf4j
public class PxMessageSendServiceImpl implements IPxMessageSendService {

    @Resource
    private MessageSendStrategyFactory strategyFactory;

    @Resource
    private ISysConfigService configService;

    /**
     * 欢迎新用户
     * @param userId 新用户ID
     * @return 发送结果
     */
    @Override
    public String welcomeNewUser(String userId) {
        log.info("🎉 欢迎新用户，用户ID: {}", userId);
        MessageSendStrategy strategy = strategyFactory.getWelcomeNewUserStrategy();
        return strategy.send(userId, "", "text/plain");
    }

    /**
     * 向特定用户发送消息（私聊）
     * @param userId 用户ID
     * @param content 消息内容
     * @param contentType 消息类型
     * @return 发送结果
     */
    @Override
    public String sendToUser(String userId, String content, String contentType) {
        log.info("💬 发送私聊消息，用户ID: {}, 类型: {}", userId, contentType);
        MessageSendStrategy strategy = strategyFactory.getSendToUserStrategy();
        return strategy.send(userId, content, contentType);
    }

    /**
     * 向特定频道发送消息（群聊）
     * @param groupId 频道ID
     * @param content 消息内容
     * @param contentType 消息类型
     * @return 发送结果
     */
    @Override
    public String sendToGroup(String groupId, String content, String contentType) {
        log.info("👥 发送群聊消息，频道ID: {}, 类型: {}", groupId, contentType);
        MessageSendStrategy strategy = strategyFactory.getSendToGroupStrategy();
        return strategy.send(groupId, content, contentType);
    }

    /**
     * 回复特定消息
     * @param messageId 消息ID
     * @param content 回复内容
     * @param contentType 消息类型
     * @return 发送结果
     */
    @Override
    public String replyToMessage(String messageId, String content, String contentType) {
        log.info("↩️ 回复消息，消息ID: {}, 类型: {}", messageId, contentType);
        MessageSendStrategy strategy = strategyFactory.getReplyToMessageStrategy();
        return strategy.send(messageId, content, contentType);
    }

    /**
     * 发送文本消息到用户（简化方法）
     * @param userId 用户ID
     * @param content 文本内容
     * @return 发送结果
     */
    @Override
    public String sendTextToUser(String userId, String content) {
        return sendToUser(userId, content, "text/plain");
    }

    /**
     * 发送Markdown消息到用户（简化方法）
     * @param userId 用户ID
     * @param content Markdown内容
     * @return 发送结果
     */
    @Override
    public String sendMarkdownToUser(String userId, String content) {
        return sendToUser(userId, content, "text/markdown");
    }

    /**
     * 发送文本消息到频道（简化方法）
     * @param groupId 频道ID
     * @param content 文本内容
     * @return 发送结果
     */
    @Override
    public String sendTextToGroup(String groupId, String content) {
        return sendToGroup(groupId, content, "text/plain");
    }

    /**
     * 发送Markdown消息到频道（简化方法）
     * @param groupId 频道ID
     * @param content Markdown内容
     * @return 发送结果
     */
    @Override
    public String sendMarkdownToGroup(String groupId, String content) {
        return sendToGroup(groupId, content, "text/markdown");
    }

    /**
     * 回复特定事件触发的消息
     * @param event 触发事件
     * @param replyContent 回复内容
     */
    @Override
    public void reply(WebhookEvent event, String replyContent) {
        // 判断是私聊还是群聊
        if (isPrivateChat(event)) {
            // 私聊：向用户发送消息
            sendToUser(event.getFrom_uid().toString(), replyContent, "text/plain");
        } else {
            // 群聊：向群组发送消息
            sendToGroup(event.getTarget().getGid().toString(), replyContent, "text/plain");
        }
    }

    /**
     * 判断是否为私聊消息
     * @param event Webhook事件
     * @return true=私聊，false=群聊
     */
    private boolean isPrivateChat(WebhookEvent event) {
        if (event.getTarget() == null || event.getTarget().getUid() == null) {
            return false;
        }
        String botUserId = configService.selectConfigByKey("sys.bot.user-id");
        return event.getTarget().getUid().toString().equals(botUserId);
    }
}
