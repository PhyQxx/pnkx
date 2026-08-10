package com.pnkx.service;

import java.util.List;

/**
 * DeepSeek AI服务接口
 */
public interface IPxDeepSeekService {

    /**
     * 调用DeepSeek API生成回复
     */
    String generateReply(String question);

    /**
     * 生成带上下文的回复
     */
    String generateReplyWithContext(String question, List<String> history);
}
