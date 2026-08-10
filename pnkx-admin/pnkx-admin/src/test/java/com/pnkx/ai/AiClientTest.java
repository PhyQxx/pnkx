package com.pnkx.ai;

import com.pnkx.domain.po.PxAiModelConfig;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class AiClientTest {

    @Test
    public void buildCacheKeySeparatesUsersAndSystemPrompts() {
        PxAiModelConfig model = new PxAiModelConfig();
        model.setModelKey("deepseek-chat");

        String userOneKey = AiClient.buildCacheKey(model, "记账助手", "午饭花了30", 1L);
        String userTwoKey = AiClient.buildCacheKey(model, "记账助手", "午饭花了30", 2L);
        String diaryPromptKey = AiClient.buildCacheKey(model, "日记助手", "午饭花了30", 1L);

        assertNotEquals(userOneKey, userTwoKey);
        assertNotEquals(userOneKey, diaryPromptKey);
        assertTrue(userOneKey.startsWith("ai:deepseek-chat:1:"));
    }
}
