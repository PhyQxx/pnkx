package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IntentDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(IntentDetectionService.class);

    private static final Map<String, List<String>> KEYWORDS = Map.of(
            "bookkeeping", List.of("花了", "消费", "支出", "收入", "微信", "支付宝", "元"),
            "todo", List.of("提醒我", "待办", "明天", "后天", "记得"),
            "diary_write", List.of("今天", "心情", "日记", "记录一下"),
            "diary_analysis", List.of("日记分析", "心情趋势", "回顾日记"),
            "analysis", List.of("消费分析", "本月花了", "账单", "花销"),
            "commemoration_day", List.of("纪念日", "周年", "生日", "恋爱", "结婚", "在一起"),
            "shopping_list", List.of("购物清单", "买东西", "超市", "买菜", "加入清单", "要买的"),
            "meal_plan", List.of("膳食计划", "吃什么", "明天吃", "菜谱", "食谱", "这周吃")
    );

    public String buildPrompt(List<IntentHandler> handlers) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个意图分类器。根据用户输入，判断意图类型，并且提取关键信息（slots）。\n");
        sb.append("你必须严格只返回一个 JSON 对象，结构如下：\n");
        sb.append("{\n");
        sb.append("  \"intent\": \"意图名称\",\n");
        sb.append("  \"confidence\": 0.95,\n");
        sb.append("  \"reason\": \"判断理由\",\n");
        sb.append("  \"slots\": { \"key\": \"value\" }\n");
        sb.append("}\n\n");
        sb.append("可选意图列表：\n");
        for (IntentHandler handler : handlers) {
            sb.append("- ").append(handler.intentName()).append(": ").append(handler.promptDescription()).append("\n");
        }
        sb.append("- chat: 用户只是在进行普通聊天，没有明确的工具操作意图。\n\n");
        sb.append("约束：\n");
        sb.append("1. confidence 必须在 0 到 1 之间。\n");
        sb.append("2. slots 必须是对象，包含意图相关的参数。\n");
        sb.append("3. 如果不确定意图，请返回 chat，并将 confidence 设低。\n");
        sb.append("只返回 JSON，不要任何解释文字。");
        return sb.toString();
    }

    public IntentDetectionResult detect(AiClient aiClient, List<IntentHandler> handlers, String question) {
        String prompt = buildPrompt(handlers);
        String rawContent = null;
        try {
            JSONObject result = aiClient.chat(prompt, "[意图识别]" + question);
            if (result != null) {
                rawContent = result.getString("content");
                if (rawContent != null) {
                    JSONObject json = parseJson(rawContent);
                    if (json != null) {
                        String intent = json.getString("intent");
                        // 校验 intent 是否合法
                        boolean isValidIntent = handlers.stream().anyMatch(h -> h.intentName().equals(intent)) || "chat".equals(intent);
                        if (isValidIntent) {
                            // AI 返回 chat 时，用关键词二次校验，避免漏识别
                            if ("chat".equals(intent)) {
                                IntentDetectionResult keywordResult = detectByKeywords(question, rawContent);
                                if (!"chat".equals(keywordResult.getIntent()) && !"fallback".equals(keywordResult.getSource())) {
                                    logger.info("关键词覆盖AI结果: ai=chat, keyword={}, question={}", keywordResult.getIntent(), question);
                                    return keywordResult;
                                }
                            }
                            JSONObject slots = json.getJSONObject("slots");
                            return IntentDetectionResult.builder()
                                    .intent(intent)
                                    .confidence(normalizeConfidence(json.getBigDecimal("confidence")))
                                    .slots(slots != null ? slots : new JSONObject())
                                    .reason(json.getString("reason"))
                                    .source("ai")
                                    .rawContent(rawContent)
                                    .build();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("AI意图识别异常: {}", e.getMessage());
        }

        // 关键词兜底
        return detectByKeywords(question, rawContent);
    }

    private JSONObject parseJson(String content) {
        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return JSON.parseObject(content.substring(start, end + 1));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return BigDecimal.ZERO;
        }
        if (confidence.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return confidence;
    }

    private IntentDetectionResult detectByKeywords(String question, String rawContent) {
        String bestIntent = "chat";
        int maxMatches = 0;
        int matchCount = 0;

        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            int currentMatches = 0;
            for (String keyword : entry.getValue()) {
                if (question.contains(keyword)) {
                    currentMatches++;
                }
            }
            if (currentMatches > maxMatches) {
                maxMatches = currentMatches;
                bestIntent = entry.getKey();
                matchCount = 1;
            } else if (currentMatches > 0 && currentMatches == maxMatches) {
                matchCount++;
            }
        }

        // 如果命中多个意图且匹配数相同，降级为 chat 避免误操作
        if (matchCount > 1) {
            bestIntent = "chat";
        }

        return IntentDetectionResult.builder()
                .intent(bestIntent)
                .confidence(new BigDecimal("0.65"))
                .slots(new JSONObject())
                .reason("关键词匹配兜底")
                .source(maxMatches > 0 ? "keyword" : "fallback")
                .rawContent(rawContent)
                .build();
    }
}
