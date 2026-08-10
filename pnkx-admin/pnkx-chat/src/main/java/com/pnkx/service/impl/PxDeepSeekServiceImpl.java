package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.service.IPxDeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 基于AgentScope Java框架的AI服务实现
 * 支持所有OpenAI兼容API（DeepSeek、OpenAI、Claude等）
 *
 * @author PHY
 */
@Service
@Slf4j
public class PxDeepSeekServiceImpl implements IPxDeepSeekService {

    @Resource
    private AiClient aiClient;

    @Override
    public String generateReply(String question) {
        if (!StringUtils.hasText(question)) {
            log.warn("生成回复时问题内容为空");
            return "您的问题内容为空，请重新提问。";
        }

        try {
            JSONObject result = aiClient.chat("你是一个智能助手，请简洁回答用户问题。", question);
            if (result != null) {
                String content = result.getString("content");
                if (StringUtils.hasText(content)) {
                    return content;
                }
            }

            throw new RuntimeException("AI返回的回复内容为空");
        } catch (Exception e) {
            log.error("调用AI API异常", e);
            throw new RuntimeException("调用AI API异常: " + e.getMessage());
        }
    }

    @Override
    public String generateReplyWithContext(String question, List<String> history) {
        if (!StringUtils.hasText(question)) {
            log.warn("生成带上下文回复时问题内容为空");
            return "您的问题内容为空，请重新提问。";
        }

        String contextPrompt = buildContextPrompt(question, history);
        return generateReply(contextPrompt);
    }

    private String buildContextPrompt(String question, List<String> history) {
        StringBuilder context = new StringBuilder();
        context.append("你是智能机器人-小爱同学，请结合以下对话上下文，回答用户的问题，回答要精准简洁，不要编造内容。\n");

        if (!CollectionUtils.isEmpty(history)) {
            context.append("对话上下文：\n");
            history.forEach(h -> context.append(h).append("\n"));
        }

        context.append("用户当前问题：").append(question);
        return context.toString();
    }
}
