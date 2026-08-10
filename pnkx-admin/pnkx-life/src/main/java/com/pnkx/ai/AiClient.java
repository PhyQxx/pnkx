package com.pnkx.ai;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.constant.RedisConstants;
import com.pnkx.common.core.redis.RedisCache;
import com.pnkx.domain.po.PxAiModelConfig;
import com.pnkx.mapper.PxAiModelConfigMapper;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AI客户端（基于AgentScope Java框架，支持多模型配置）
 * 所有现代LLM（DeepSeek、OpenAI、Claude等）均采用OpenAI兼容API格式
 *
 * @author PHY
 */
@Component
public class AiClient {

    private static final Logger logger = LoggerFactory.getLogger(AiClient.class);

    @Resource
    private PxAiModelConfigMapper aiModelConfigMapper;

    @Resource
    private RedisCache redisCache;

    private final ConcurrentHashMap<Long, Model> modelCache = new ConcurrentHashMap<>();

    /**
     * 使用默认模型调用AI（同步）
     */
    public JSONObject chat(String userInfo, String question) {
        PxAiModelConfig defaultModel = aiModelConfigMapper.selectDefaultEnabledModel();
        if (defaultModel == null) {
            logger.error("未配置默认AI模型，请先在AI模型配置中添加");
            return null;
        }
        return chatWithModel(defaultModel, userInfo, question);
    }

    /**
     * 使用指定模型ID调用AI（同步）
     */
    public JSONObject chatWithModelId(String userInfo, String question, Long modelId) {
        PxAiModelConfig model = aiModelConfigMapper.selectPxAiModelConfigById(modelId);
        if (model == null) {
            logger.error("AI模型不存在: {}", modelId);
            return null;
        }
        return chatWithModel(model, userInfo, question);
    }

    /**
     * 流式AI调用，返回AgentScope Flux流
     */
    public Flux<ChatResponse> chatStream(String userInfo, String question) {
        PxAiModelConfig defaultModel = aiModelConfigMapper.selectDefaultEnabledModel();
        if (defaultModel == null) {
            logger.error("未配置默认AI模型");
            return Flux.empty();
        }
        return chatStreamWithModel(defaultModel, userInfo, question);
    }

    /**
     * 流式AI调用，返回AgentScope Flux流（指定模型）
     */
    public Flux<ChatResponse> chatStreamWithModelId(String userInfo, String question, Long modelId) {
        PxAiModelConfig model = aiModelConfigMapper.selectPxAiModelConfigById(modelId);
        if (model == null) {
            logger.error("AI模型不存在: {}", modelId);
            return Flux.empty();
        }
        return chatStreamWithModel(model, userInfo, question);
    }

    /**
     * 通用AI调用（同步，通过stream + blockLast实现）
     */
    private JSONObject chatWithModel(PxAiModelConfig modelConfig, String userInfo, String question) {
        String cacheKey = buildCacheKey(modelConfig, userInfo, question, currentUserId());
        String redisKey = RedisConstants.AI_RESPONSE_CACHE + cacheKey;
        JSONObject cached = redisCache.getCacheObject(redisKey);
        if (cached != null) {
            logger.info("缓存命中");
            return cached;
        }

        logger.info("AI调用 - 模型: {} 问题: {}", modelConfig.getModelKey(), question);

        try {
            Model model = getOrCreateModel(modelConfig);

            Msg systemMsg = Msg.builder().textContent(userInfo).build();
            Msg userMsg = Msg.builder().textContent(question).build();

            // 使用 stream + collectList 获取完整响应
            List<ChatResponse> responses = model.stream(List.of(systemMsg, userMsg), null, null)
                    .collectList().block();

            if (responses != null && !responses.isEmpty()) {
                // 拼接所有响应的文本内容
                StringBuilder sb = new StringBuilder();
                for (ChatResponse resp : responses) {
                    if (resp.getContent() != null) {
                        for (ContentBlock block : resp.getContent()) {
                            if (block instanceof TextBlock textBlock) {
                                sb.append(textBlock.getText());
                            }
                        }
                    }
                }
                String content = sb.toString();
                if (!content.isEmpty()) {
                    JSONObject result = new JSONObject();
                    result.put("content", content);
                    result.put("model", modelConfig.getModelKey());
                    redisCache.setCacheObject(redisKey, result, 1, TimeUnit.HOURS);
                    logger.info("AI调用成功");
                    return result;
                }
            }
        } catch (Exception e) {
            logger.error("AI调用失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 流式AI调用
     */
    private Flux<ChatResponse> chatStreamWithModel(PxAiModelConfig modelConfig, String userInfo, String question) {
        logger.info("AI流式调用 - 模型: {} 问题: {}", modelConfig.getModelKey(), question);

        try {
            Model model = getOrCreateModel(modelConfig);

            Msg systemMsg = Msg.builder().textContent(userInfo).build();
            Msg userMsg = Msg.builder().textContent(question).build();

            return model.stream(List.of(systemMsg, userMsg), null, null);
        } catch (Exception e) {
            logger.error("AI流式调用失败: {}", e.getMessage());
            return Flux.empty();
        }
    }

    /**
     * 获取或创建模型实例（带缓存）
     */
    private Model getOrCreateModel(PxAiModelConfig config) {
        return modelCache.computeIfAbsent(config.getId(), id ->
            OpenAIChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelKey())
                .baseUrl(config.getBaseUrl())
                .stream(true)
                .generateOptions(GenerateOptions.builder()
                    .temperature(config.getTemperature())
                    .build())
                .build()
        );
    }

    public static String buildCacheKey(PxAiModelConfig modelConfig, String userInfo, String question, Long userId) {
        String modelKey = modelConfig == null || modelConfig.getModelKey() == null
                ? "unknown"
                : modelConfig.getModelKey();
        String userPart = userId == null ? "anonymous" : String.valueOf(userId);
        return "ai:" + modelKey + ":" + userPart + ":" + sha256(userInfo) + ":" + sha256(question);
    }

    private Long currentUserId() {
        try {
            Class<?> securityUtils = Class.forName("com.pnkx.common.utils.SecurityUtils");
            Object value = securityUtils.getMethod("getUserId").invoke(null);
            String userId = value == null ? null : String.valueOf(value);
            return userId == null || userId.trim().isEmpty() ? null : Long.parseLong(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String item = Integer.toHexString(0xff & b);
                if (item.length() == 1) {
                    hex.append('0');
                }
                hex.append(item);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 清除模型缓存
     */
    public void clearModelCache(Long modelId) {
        modelCache.remove(modelId);
    }

    public void clearAllModelCache() {
        modelCache.clear();
    }
}
