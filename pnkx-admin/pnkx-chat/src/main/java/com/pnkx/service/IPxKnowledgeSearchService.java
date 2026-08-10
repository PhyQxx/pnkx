package com.pnkx.service;

import com.pnkx.chat.domain.dto.KnowledgeSearchResult;

import java.util.List;

/**
 * 知识库搜索服务接口
 */
public interface IPxKnowledgeSearchService {
    /**
     * 搜索知识
     *
     * @param question 用户问题
     * @param limit 结果数量限制
     * @return 搜索结果列表
     */
    List<KnowledgeSearchResult> search(String question, int limit);

    /**
     * 构建知识库 Prompt
     *
     * @param question 用户问题
     * @param results 搜索结果
     * @return Prompt
     */
    String buildKnowledgePrompt(String question, List<KnowledgeSearchResult> results);
}
