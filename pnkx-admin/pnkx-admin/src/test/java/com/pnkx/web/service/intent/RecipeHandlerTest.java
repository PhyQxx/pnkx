package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RecipeHandlerTest {
    @Test void routesRecipeBeforeLinkClippingWithoutCallingModel() {
        var ai = mock(AiClient.class);
        var result = new IntentDetectionService().detect(ai, List.of(new RecipeHandler()),
                "https://www.xiachufang.com/recipe/100505867/\n生成到菜谱库");
        assertEquals("recipe", result.getIntent());
        verifyNoInteractions(ai);
        assertFalse(IntentDetectionService.isRecipeCreation("查菜谱"));
        assertFalse(IntentDetectionService.isRecipeCreation("收藏链接 https://example.com"));
    }
    @Test void failedFetchDoesNotGenerateOrSave() throws Exception {
        var handler = new RecipeHandler();
        var pages = mock(RecipePageReader.class);
        var ai = mock(AiClient.class);
        var plans = mock(LocalIntentPlanService.class);
        ReflectionTestUtils.setField(handler, "pages", pages);
        ReflectionTestUtils.setField(handler, "aiClient", ai);
        ReflectionTestUtils.setField(handler, "plans", plans);
        when(pages.read(anyString())).thenThrow(new IOException());
        var out = new ByteArrayOutputStream();
        assertTrue(handler.handle("https://example.com 生成到菜谱库", new JSONObject(), out));
        assertTrue(out.toString().contains("尚未保存菜谱"));
        verifyNoInteractions(ai, plans);
    }
    @Test void textRecipePreparesConfirmationAndDelegatesSave() throws Exception {
        var handler = new RecipeHandler();
        var ai = mock(AiClient.class);
        var plans = mock(LocalIntentPlanService.class);
        var pending = mock(AiPendingActionService.class);
        ReflectionTestUtils.setField(handler, "aiClient", ai);
        ReflectionTestUtils.setField(handler, "plans", plans);
        ReflectionTestUtils.setField(handler, "pending", pending);
        when(ai.chat(anyString(), anyString())).thenReturn(JSONObject.parseObject("{\"content\":\"{\\\"title\\\":\\\"蒸蛋\\\",\\\"servings\\\":1,\\\"ingredients\\\":[{\\\"name\\\":\\\"鸡蛋\\\",\\\"quantity\\\":\\\"2个\\\"}],\\\"notes\\\":\\\"1.蒸熟\\\"}\"}"));
        var out = new ByteArrayOutputStream();
        handler.handle("把蒸蛋做法生成到菜谱库", new JSONObject().fluentPut("requestId", "req"), out);
        assertTrue(out.toString().contains("[PENDING_CONFIRM]"));
        verify(plans).prepare(any(), eq("recipe.create"), anyString());
        verify(pending).save(eq("req"), eq("recipe"), any());
        verify(plans, never()).confirm(any(), any());
        var draft = new JSONObject();
        handler.confirm(draft, out);
        verify(plans).confirm(draft, out);
    }
    @Test void refusesLocalNetworkRecipeUrl() {
        assertThrows(IOException.class, () -> new RecipePageReader().read("http://127.0.0.1/recipe"));
    }
}
