package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IntentDetectionServiceTest {

    private AiClient aiClient;
    private IntentDetectionService intentDetectionService;
    private JSONObject aiResponse;

    private List<IntentHandler> handlers;

    @BeforeEach
    public void setUp() {
        intentDetectionService = new IntentDetectionService();
        aiClient = new AiClient() {
            @Override
            public JSONObject chat(String userInfo, String question) {
                return aiResponse;
            }
        };
        handlers = new ArrayList<>();
        handlers.add(new BookkeepingHandler());
        handlers.add(new TodoHandler());
        handlers.add(new NoteHandler());
    }

    @Test
    public void testDetectWithValidJson() {
        String json = "{\n" +
                "  \"intent\": \"bookkeeping\",\n" +
                "  \"confidence\": 0.95,\n" +
                "  \"reason\": \"Contains amount and category\",\n" +
                "  \"slots\": { \"money\": 21.0 }\n" +
                "}";
        aiResponse = new JSONObject().fluentPut("content", json);

        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "中午吃包子21元");

        assertEquals("bookkeeping", result.getIntent());
        assertEquals("ai", result.getSource());
        assertEquals(0, new BigDecimal("0.95").compareTo(result.getConfidence()));
        assertEquals(21.0, result.getSlots().getDouble("money"), 0.001);
        assertFalse(result.isLowConfidence());
    }

    @Test
    public void testDetectWithInvalidJsonKeywordFallback() {
        aiResponse = new JSONObject().fluentPut("content", "invalid json");

        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "提醒我明天开会");

        assertEquals("todo", result.getIntent());
        assertEquals("keyword", result.getSource());
        assertEquals(0, new BigDecimal("0.65").compareTo(result.getConfidence()));
        assertTrue(result.isLowConfidence()); // 写库类 0.65 < 0.72
    }

    @Test
    public void testDetectLowConfidence() {
        String json = "{\n" +
                "  \"intent\": \"bookkeeping\",\n" +
                "  \"confidence\": 0.5,\n" +
                "  \"reason\": \"Maybe bookkeeping\",\n" +
                "  \"slots\": {}\n" +
                "}";
        aiResponse = new JSONObject().fluentPut("content", json);

        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "帮我记一下");

        assertEquals("bookkeeping", result.getIntent());
        assertTrue(result.isLowConfidence());
    }

    @Test
    public void testNoteCreateIsWriteIntentForConfidenceGate() {
        String json = "{\n" +
                "  \"intent\": \"note\",\n" +
                "  \"confidence\": 0.65,\n" +
                "  \"reason\": \"Create note\",\n" +
                "  \"slots\": { \"action\": \"create\" }\n" +
                "}";
        aiResponse = new JSONObject().fluentPut("content", json);

        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "帮我记一条笔记");

        assertEquals("note", result.getIntent());
        assertTrue(result.isLowConfidence());
    }

    @Test
    public void testConfidenceIsClampedAndSlotsDefaultToObject() {
        String json = "{\n" +
                "  \"intent\": \"bookkeeping\",\n" +
                "  \"confidence\": 1.5,\n" +
                "  \"reason\": \"Too high\"\n" +
                "}";
        aiResponse = new JSONObject().fluentPut("content", json);

        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "午饭花了21元");

        assertEquals(BigDecimal.ONE, result.getConfidence());
        assertNotNull(result.getSlots());
    }

    @Test
    public void testDetectKeywordConflictFallbackToChat() {
        // "支出" (bookkeeping) and "记得" (todo) - both match once
        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "记得支出");
        
        assertEquals("chat", result.getIntent());
    }

    @Test
    public void testCommemorationKeywordFallback() {
        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "记录恋爱纪念日");
        assertEquals("commemoration_day", result.getIntent());
    }

    @Test
    public void testShoppingListKeywordFallback() {
        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "把鸡蛋加入购物清单");
        assertEquals("shopping_list", result.getIntent());
    }

    @Test
    public void testMealPlanKeywordFallback() {
        IntentDetectionResult result = intentDetectionService.detect(aiClient, handlers, "这周吃什么，安排膳食计划");
        assertEquals("meal_plan", result.getIntent());
    }
}
