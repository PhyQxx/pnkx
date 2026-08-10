package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 普通对话意图处理器（兜底）
 * 不匹配任何特定意图时的默认处理
 *
 * @author PHY
 */
@Component
public class ChatHandler implements IntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatHandler.class);

    @Resource
    private AiClient aiClient;

    @Override
    public String intentName() {
        return "chat";
    }

    @Override
    public String promptDescription() {
        return "- {\"intent\":\"chat\"} — 其他普通对话";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) throws IOException {
        String systemPrompt = intentData.getString("systemPrompt");

        try {
            Long modelId = intentData.getLong("modelId");
            Flux<ChatResponse> stream = modelId == null
                    ? aiClient.chatStream(systemPrompt, question)
                    : aiClient.chatStreamWithModelId(systemPrompt, question, modelId);

            stream
                    .toStream()
                    .forEach(chatResponse -> {
                        try {
                            if (chatResponse.getContent() != null) {
                                for (ContentBlock block : chatResponse.getContent()) {
                                    if (block instanceof TextBlock textBlock) {
                                        String text = textBlock.getText();
                                        if (text != null && !text.isEmpty()) {
                                            IntentHandler.writeSse(out, text);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            logger.error("流式发送失败: {}", e.getMessage());
                        }
                    });
            IntentHandler.writeSse(out, "[DONE]");
        } catch (Exception e) {
            logger.error("AI流式对话失败: {}", e.getMessage());
            IntentHandler.writeSse(out, "抱歉，AI 回复出错了，请重试。");
        }
        return true;
    }
}
