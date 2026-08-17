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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * 单次请求的思考模式覆盖（null=沿用模型配置默认值）。
     * 由 AiController 在请求开始时设置、结束时清理；整个请求链同步运行在 Tomcat 线程上，
     * 因此意图识别、各 handler 的 AI 调用都会读取到该覆盖值。
     */
    private static final ThreadLocal<Boolean> thinkingOverride = new ThreadLocal<>();

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
     * 使用尚未保存的配置发送最小请求，用于后台配置页测试连接。
     * 测试模型不进入实例缓存，避免未保存的参数影响正常请求。
     */
    public void testConnection(PxAiModelConfig modelConfig) {
        GenerateOptions.Builder optionsBuilder = GenerateOptions.builder()
                .temperature(modelConfig.getTemperature() == null ? 0.7D : modelConfig.getTemperature());
        if (modelConfig.getThinking() != null) {
            optionsBuilder.additionalBodyParam("thinking",
                    Map.of("type", Boolean.TRUE.equals(modelConfig.getThinking()) ? "enabled" : "disabled"));
        }

        Model model = OpenAIChatModel.builder()
                .apiKey(modelConfig.getApiKey())
                .modelName(modelConfig.getModelKey())
                .baseUrl(modelConfig.getBaseUrl())
                .stream(true)
                .generateOptions(optionsBuilder.build())
                .build();

        Msg systemMsg = Msg.builder().textContent("你是一个连接测试助手。").build();
        Msg userMsg = Msg.builder().textContent("请仅回复 OK").build();
        List<ChatResponse> responses = model.stream(List.of(systemMsg, userMsg), null, null)
                .collectList()
                .block(Duration.ofSeconds(30));
        if (responses == null || responses.isEmpty()) {
            throw new IllegalStateException("模型未返回内容");
        }
    }

    /**
     * 通用AI调用（同步，通过stream + blockLast实现）
     */
    private JSONObject chatWithModel(PxAiModelConfig modelConfig, String userInfo, String question) {
        long start = System.currentTimeMillis();
        String modelKey = modelConfig.getModelKey();
        String cacheKey = buildCacheKey(modelConfig, userInfo, question, currentUserId());
        String redisKey = RedisConstants.AI_RESPONSE_CACHE + cacheKey;
        JSONObject cached = redisCache.getCacheObject(redisKey);
        if (cached != null) {
            logger.info("AI调用缓存命中 - 模型: {} 耗时: {}ms", modelKey, System.currentTimeMillis() - start);
            return cached;
        }

        logger.info("AI调用开始 - 模型: {} 问题: {}", modelKey, question);

        try {
            long tModel = System.currentTimeMillis();
            Model model = getOrCreateModel(modelConfig);
            logger.info("AI调用模型就绪 - 模型: {} 耗时: {}ms", modelKey, System.currentTimeMillis() - tModel);

            Msg systemMsg = Msg.builder().textContent(userInfo).build();
            Msg userMsg = Msg.builder().textContent(question).build();

            // 使用 stream + collectList 获取完整响应
            long tCall = System.currentTimeMillis();
            GenerateOptions perCallOptions = buildThinkingOptions(modelConfig, thinkingOverride.get());
            List<ChatResponse> responses = model.stream(List.of(systemMsg, userMsg), null, perCallOptions)
                    .collectList().block();
            logger.info("AI调用LLM返回 - 模型: {} 思考={} 耗时: {}ms", modelKey, thinkingOverride.get(), System.currentTimeMillis() - tCall);

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
                    result.put("model", modelKey);
                    redisCache.setCacheObject(redisKey, result, 1, TimeUnit.HOURS);
                    logger.info("AI调用成功 - 模型: {} 总耗时: {}ms", modelKey, System.currentTimeMillis() - start);
                    return result;
                }
            }
            logger.warn("AI调用返回空 - 模型: {} 总耗时: {}ms", modelKey, System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.error("AI调用失败 - 模型: {} 总耗时: {}ms 错误: {}", modelKey, System.currentTimeMillis() - start, e.getMessage());
        }
        return null;
    }

    /**
     * 流式AI调用
     */
    private Flux<ChatResponse> chatStreamWithModel(PxAiModelConfig modelConfig, String userInfo, String question) {
        long start = System.currentTimeMillis();
        String modelKey = modelConfig.getModelKey();
        logger.info("AI流式调用开始 - 模型: {} 问题: {}", modelKey, question);

        try {
            long tModel = System.currentTimeMillis();
            Model model = getOrCreateModel(modelConfig);
            logger.info("AI流式模型就绪 - 模型: {} 耗时: {}ms", modelKey, System.currentTimeMillis() - tModel);

            Msg systemMsg = Msg.builder().textContent(userInfo).build();
            Msg userMsg = Msg.builder().textContent(question).build();

            GenerateOptions perCallOptions = buildThinkingOptions(modelConfig, thinkingOverride.get());
            AtomicBoolean firstTokenLogged = new AtomicBoolean(false);
            return model.stream(List.of(systemMsg, userMsg), null, perCallOptions)
                    .doOnNext(resp -> {
                        if (firstTokenLogged.compareAndSet(false, true)) {
                            logger.info("AI流式首Token - 模型: {} TTFT: {}ms", modelKey, System.currentTimeMillis() - start);
                        }
                    })
                    .doOnComplete(() -> logger.info("AI流式调用完成 - 模型: {} 总耗时: {}ms", modelKey, System.currentTimeMillis() - start))
                    .doOnError(e -> logger.error("AI流式调用失败 - 模型: {} 总耗时: {}ms 错误: {}", modelKey, System.currentTimeMillis() - start, e.getMessage()));
        } catch (Exception e) {
            logger.error("AI流式调用构建失败: {}", e.getMessage());
            return Flux.empty();
        }
    }

    /**
     * 获取或创建模型实例（带缓存）
     */
    private Model getOrCreateModel(PxAiModelConfig config) {
        return modelCache.computeIfAbsent(config.getId(), id -> {
            GenerateOptions.Builder optionsBuilder = GenerateOptions.builder()
                    .temperature(config.getTemperature());
            // 思考模式开关：仅 thinking 非 null 时注入，透传为智谱 OpenAI 兼容的 thinking 字段
            // {"type":"enabled"} 开启 / {"type":"disabled"} 关闭；为 null 则不发送，兼容不支持思考的厂商
            if (config.getThinking() != null) {
                optionsBuilder.additionalBodyParam("thinking",
                        Map.of("type", Boolean.TRUE.equals(config.getThinking()) ? "enabled" : "disabled"));
            }
            return OpenAIChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelKey())
                    .baseUrl(config.getBaseUrl())
                    .stream(true)
                    .generateOptions(optionsBuilder.build())
                    .build();
        });
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
     * 设置当前请求的思考模式覆盖（由控制器在请求生命周期内调用）
     *
     * @param thinking null=沿用模型配置默认值；true=开启思考；false=关闭思考
     */
    public static void setThinkingOverride(Boolean thinking) {
        if (thinking == null) {
            thinkingOverride.remove();
        } else {
            thinkingOverride.set(thinking);
        }
    }

    /**
     * 清除当前请求的思考模式覆盖（务必在请求结束时调用，避免线程复用污染）
     */
    public static void clearThinkingOverride() {
        thinkingOverride.remove();
    }

    /**
     * 构建按请求覆盖思考模式的 per-call GenerateOptions。
     * 同时带上 temperature 以保证与模型配置一致（不受 mergeOptions 语义影响）。
     * 返回 null 表示不覆盖，沿用模型实例自身配置。
     */
    private GenerateOptions buildThinkingOptions(PxAiModelConfig config, Boolean thinking) {
        if (thinking == null) {
            return null;
        }
        return GenerateOptions.builder()
                .temperature(config.getTemperature())
                .additionalBodyParam("thinking",
                        Map.of("type", Boolean.TRUE.equals(thinking) ? "enabled" : "disabled"))
                .build();
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
