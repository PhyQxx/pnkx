package com.pnkx.web.controller.chat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.redis.RedisCache;
import com.pnkx.common.constant.RedisConstants;
import com.pnkx.domain.po.WebhookEvent;
import com.pnkx.service.IPxBookkeepingRecordService;
import com.pnkx.service.IPxChatRecordService;
import com.pnkx.service.IPxCustomReplyService;
import com.pnkx.service.IPxDeepSeekService;
import com.pnkx.service.IPxMessageSendService;
import com.pnkx.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/webhook")
public class PxWebhookController {

    // 常量定义
    private static final String EVENT_TYPE_REPLY = "reply";
    private static final String EVENT_TYPE_NEW_USER = "new_user";
    private static final String EVENT_TYPE_CHAT = "chat";
    private static final String REPLY_TYPE_CUSTOM = "custom";
    private static final String REPLY_TYPE_AI = "ai";
    private static final String REPLY_KEY = "reply";
    private static final String TYPE_KEY = "type";
    private static final int MAX_HISTORY_SIZE = 20;
    private static final String BOT_MENTION_PATTERN = "@" + "%s";
    private static final String LEGACY_BOT_MENTION = "@3";
    private static final String USER_PREFIX = "用户: ";
    private static final String AI_PREFIX = "AI: ";
    private static final String NO_REPLY_MESSAGE = "无回复";
    private static final String ANALYZE_MONTHLY_CONSUMPTION = "分析本月消费";
    private static final String ANALYZE_MONTHLY_CONSUMPTION_ALL = "分析所有记录";

    // 存储用户对话上下文：userId -> 对话列表（使用Redis持久化）
    @Resource
    private RedisCache redisCache;

    @Resource
    private IPxDeepSeekService deepSeekService;

    @Resource
    private IPxMessageSendService messageSendService;

    @Resource
    private IPxChatRecordService chatRecordService;

    @Resource
    private IPxCustomReplyService customReplyService;

    @Resource
    private IPxBookkeepingRecordService pxBookkeepingRecordService;

    @Resource
    private ISysConfigService configService;

    // 机器人用户ID（用于识别@消息，从数据库读取）
    private Integer getBotUserId() {
        String val = configService.selectConfigByKey("sys.bot.user-id");
        return val != null ? Integer.valueOf(val) : 3;
    }

    /**
     * Webhook健康检查接口
     */
    @GetMapping
    public AjaxResult healthCheck() {
        log.info("📥 Webhook healthCheck");
        return AjaxResult.success("Webhook is running");
    }

    /**
     * VoceChat Webhook核心入口，固定路径：/webhook
     */
    @PostMapping
    public AjaxResult webhook(@RequestBody String requestBody, HttpServletRequest request) {
        log.info("📥 收到Webhook请求: {}", requestBody);

        try {
            WebhookEvent event = parseWebhookEvent(requestBody);

            if (!isValidEvent(event)) {
                log.warn("⚠️ 无效的Webhook事件: {}", event);
                return AjaxResult.success(NO_REPLY_MESSAGE);
            }

            // 保存用户消息记录
            chatRecordService.saveMessage(event, false, null);

            // 处理不同类型的事件
            AjaxResult result = handleEventByType(event);
            if (result != null) {
                return result;
            }

            return AjaxResult.success(NO_REPLY_MESSAGE);

        } catch (Exception e) {
            log.error("❌ 处理Webhook请求异常", e);
            return AjaxResult.error("处理Webhook请求失败: " + e.getMessage());
        }
    }

    /**
     * 解析Webhook事件
     */
    private WebhookEvent parseWebhookEvent(String requestBody) {
        if (!StringUtils.hasText(requestBody)) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        return JSON.parseObject(requestBody, WebhookEvent.class);
    }

    /**
     * 验证事件有效性
     */
    private boolean isValidEvent(WebhookEvent event) {
        return event != null;
    }

    /**
     * 根据事件类型处理消息
     */
    private AjaxResult handleEventByType(WebhookEvent event) {
        String eventType = event.getType();

        if (EVENT_TYPE_REPLY.equals(eventType)) {
            log.debug("📤 忽略回复消息类型");
            return AjaxResult.success();
        }

        // 处理新用户欢迎消息
        if (EVENT_TYPE_NEW_USER.equals(eventType)) {
            return handleNewUserWelcome(event);
        }

        String content = getProcessedContent(event);
        
        String cleanContent = content != null ? content.trim() : "";

        // 提取去除@机器人的纯文本内容用于判断指令
        String question = extractQuestion(cleanContent);

        // 特定指令处理：分析本月消费
        if (ANALYZE_MONTHLY_CONSUMPTION.equals(question)) {
            return handleMonthlyConsumptionAnalysis(event, false);
        }
        if (ANALYZE_MONTHLY_CONSUMPTION_ALL.equals(question) || "分析所有消费".equals(question)) {
            return handleMonthlyConsumptionAnalysis(event, true);
        }

        // 优先处理知识库和自定义回复规则
        String knowledgeReply = handleKnowledgeReply(question, event);
        if (knowledgeReply != null) {
            return buildSuccessResponse(knowledgeReply, "knowledge_base");
        }

        // 处理AI回复
        String aiReply = handleAiReply(cleanContent, event);
        if (aiReply != null) {
            return buildSuccessResponse(aiReply, REPLY_TYPE_AI);
        }

        return null;
    }

    /**
     * 处理本月消费分析
     */
    private AjaxResult handleMonthlyConsumptionAnalysis(WebhookEvent event, boolean isAll) {
        log.info("📈 开始处理本月消费分析指令, isAll: {}", isAll);
        try {
            JSONObject analysisResult = pxBookkeepingRecordService.aiAnalysis(isAll);
            String reply = analysisResult.getString("content");

            if (StringUtils.hasText(reply)) {
                sendReplyMessage(event, reply);
                return buildSuccessResponse(reply, "ai_analysis");
            } else {
                String noDataMessage = "本月还没有消费记录哦，快去记一笔吧！";
                sendReplyMessage(event, noDataMessage);
                return buildSuccessResponse(noDataMessage, "ai_analysis_empty");
            }
        } catch (Exception e) {
            log.error("❌ 分析本月消费异常", e);
            String errorMessage = "抱歉，分析消费数据时出错了，请稍后再试。";
            sendReplyMessage(event, errorMessage);
            return AjaxResult.error("分析本月消费失败: " + e.getMessage());
        }
    }

    /**
     * 处理新用户欢迎消息
     */
    private AjaxResult handleNewUserWelcome(WebhookEvent event) {
        try {
            Integer newUserId = event.getUid();
            if (newUserId == null) {
                log.warn("⚠️ 新用户事件中用户ID为空");
                return AjaxResult.success("新用户ID为空，跳过欢迎消息");
            }

            String userName = event.getName() != null ? event.getName() : "新用户";
            log.info("👋 欢迎新用户，用户ID: {}, 用户名: {}", newUserId, userName);

            // 使用关键字"新用户"匹配自定义回复规则
            String customWelcomeReply = customReplyService.matchCustomReply("新用户");

            String welcomeMessage;
            String replyType;

            if (customWelcomeReply != null) {
                // 使用自定义回复规则中的欢迎消息
                welcomeMessage = customWelcomeReply;
                replyType = "custom_welcome";
                log.info("🎯 匹配到新用户自定义欢迎回复: {}", welcomeMessage);
            } else {
                // 使用默认欢迎消息，包含用户名
                welcomeMessage = String.format("欢迎%s加入！我是AI助手，有什么可以帮助你的吗？", userName);
                replyType = "default_welcome";
                log.info("📝 使用默认新用户欢迎消息");
            }

            // 使用SendToUserStrategy向新用户发送欢迎消息
            String welcomeResult = messageSendService.sendTextToUser(newUserId.toString(), welcomeMessage);

            log.info("✅ 新用户欢迎消息发送结果: {}", welcomeResult);

            // 保存欢迎消息记录
            chatRecordService.saveMessage(event, true, welcomeMessage);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("reply", welcomeMessage);
            responseData.put("type", replyType);
            responseData.put("userId", newUserId);
            responseData.put("userName", userName);
            responseData.put("result", welcomeResult);

            return AjaxResult.success(responseData);

        } catch (Exception e) {
            log.error("❌ 处理新用户欢迎消息异常", e);
            return AjaxResult.error("处理新用户欢迎消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取处理后的内容
     */
    private String getProcessedContent(WebhookEvent event) {
        return event.getDetail().getContent();
    }

    /**
     * 处理自定义回复
     */
    private String handleCustomReply(String content, WebhookEvent event) {
        String customReply = customReplyService.matchCustomReply(content);
        if (customReply != null) {
            log.info("🤖 匹配到自定义回复: {}", customReply);
            sendReplyMessage(event, customReply);
            return customReply;
        }
        return null;
    }

    /**
     * 处理知识库或自定义回复
     */
    private String handleKnowledgeReply(String question, WebhookEvent event) {
        String reply = customReplyService.searchKnowledgeAndReply(question);
        if (StringUtils.hasText(reply)) {
            log.info("🤖 匹配到知识库或自定义回复: {}", reply);
            sendReplyMessage(event, reply);
            return reply;
        }
        return null;
    }

    /**
     * 处理AI回复
     */
    private String handleAiReply(String content, WebhookEvent event) {
        String reply = processMessage(content, event.getFrom_uid().toString(), event);
        if (reply != null) {
            log.info("🤖 生成AI回复: {}", reply);
            sendReplyMessage(event, reply);
            return reply;
        }
        return null;
    }

    /**
     * 发送回复消息
     */
    private void sendReplyMessage(WebhookEvent event, String reply) {
        messageSendService.reply(event, reply);
        chatRecordService.saveMessage(event, true, reply);
    }

    /**
     * 构建成功响应
     */
    private AjaxResult buildSuccessResponse(String reply, String type) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put(REPLY_KEY, reply);
        responseData.put(TYPE_KEY, type);
        return AjaxResult.success(responseData);
    }

    /**
     * 处理消息并生成AI回复
     */
    private String processMessage(String content, String userId, WebhookEvent event) {
        if (!StringUtils.hasText(content)) {
            log.warn("⚠️ 消息内容为空，跳过处理");
            return null;
        }

        // 检查是否为私聊消息或@机器人的消息
        if (isPrivateChat(event) || isBotMentioned(content)) {
            String question = extractQuestion(content);

            if (StringUtils.hasText(question)) {
                return generateAiReply(question, userId);
            }
        }

        return null;
    }

    /**
     * 检查是否提及机器人
     */
    private boolean isBotMentioned(String content) {
        if (getBotUserId() == null) {
            log.warn("⚠️ 机器人用户ID未配置");
            return false;
        }

        String botMention = String.format(BOT_MENTION_PATTERN, getBotUserId());
        return content.contains(botMention) || content.contains(LEGACY_BOT_MENTION);
    }

    /**
     * 提取问题内容（移除@部分）
     */
    private String extractQuestion(String content) {
        if (getBotUserId() == null) {
            return content.trim();
        }

        String botMention = String.format(BOT_MENTION_PATTERN, getBotUserId());
        String question = content.replaceAll(botMention, "")
            .replace(LEGACY_BOT_MENTION, "")
            .trim();

        log.debug("🔍 提取问题内容: 原始='{}', 处理后='{}'", content, question);
        return question;
    }

    /**
     * 生成AI回复
     */
    private String generateAiReply(String question, String userId) {
        try {
            // 获取用户对话历史
            List<String> history = getChatHistory(userId);

            // 调用DeepSeek API生成回复
            String reply = deepSeekService.generateReplyWithContext(question, history);

            if (!StringUtils.hasText(reply)) {
                log.warn("⚠️ AI回复为空");
                return null;
            }

            // 更新对话历史
            updateChatHistory(userId, question, reply);

            return reply;
        } catch (Exception e) {
            log.error("❌ 生成AI回复异常", e);
            return "抱歉，我遇到了一些问题，请稍后再试。";
        }
    }

    /**
     * 获取对话历史
     */
    private List<String> getChatHistory(String userId) {
        String key = RedisConstants.AI_CHAT_HISTORY + userId;
        List<String> history = redisCache.getCacheList(key);
        return history != null ? history : new ArrayList<>();
    }

    /**
     * 更新对话历史
     */
    private void updateChatHistory(String userId, String question, String reply) {
        String key = RedisConstants.AI_CHAT_HISTORY + userId;

        // 添加新的对话记录
        redisCache.redisTemplate.opsForList().rightPush(key, USER_PREFIX + question);
        redisCache.redisTemplate.opsForList().rightPush(key, AI_PREFIX + reply);

        // 限制历史记录长度，保留最近 MAX_HISTORY_SIZE 条
        redisCache.redisTemplate.opsForList().trim(key, -MAX_HISTORY_SIZE, -1);

        // 设置过期时间24小时
        redisCache.expire(key, 24, TimeUnit.HOURS);

        log.debug("📚 更新用户 {} 的对话历史，当前记录数: {}", userId, getChatHistory(userId).size());
    }

    private boolean isPrivateChat(WebhookEvent event) {
        return event.getTarget() != null &&
            event.getTarget().getUid() != null &&
            event.getTarget().getUid().equals(getBotUserId());
    }
}
