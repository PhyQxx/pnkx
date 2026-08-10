package com.pnkx.service.impl;

import com.pnkx.chat.domain.dto.KnowledgeSearchResult;
import com.pnkx.domain.po.PxArticle;
import com.pnkx.domain.po.PxCustomReplyContent;
import com.pnkx.domain.po.PxCustomReplyRule;
import com.pnkx.domain.vo.PxArticleVo;
import com.pnkx.mapper.PxArticleMapper;
import com.pnkx.mapper.PxCustomReplyContentMapper;
import com.pnkx.mapper.PxCustomReplyRuleMapper;
import com.pnkx.service.IPxKnowledgeSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PxKnowledgeSearchServiceImpl implements IPxKnowledgeSearchService {

    @Autowired
    private PxCustomReplyRuleMapper customReplyMapper;

    @Autowired
    private PxCustomReplyContentMapper customReplyContentMapper;

    @Autowired
    private PxArticleMapper articleMapper;

    @Override
    public List<KnowledgeSearchResult> search(String question, int limit) {
        List<KnowledgeSearchResult> results = new ArrayList<>();

        PxCustomReplyRule queryRule = new PxCustomReplyRule();
        queryRule.setEnabled(true);
        List<PxCustomReplyRule> rules = customReplyMapper.selectPxCustomReplyRuleList(queryRule);
        for (PxCustomReplyRule rule : rules) {
            String content = buildReplyContent(rule.getId());
            double score = calculateScore(question, rule.getRuleName() + " " + rule.getKeywords() + " " + content);
            if (score > 0.3) {
                results.add(KnowledgeSearchResult.builder()
                        .sourceType("custom_reply")
                        .sourceId(rule.getId())
                        .title(rule.getRuleName())
                        .content(content)
                        .score(BigDecimal.valueOf(score))
                        .build());
            }
        }

        PxArticleVo queryArticle = new PxArticleVo();
        queryArticle.setOpen("1");
        queryArticle.setState("1");
        List<PxArticleVo> articles = articleMapper.selectPxArticleList(queryArticle);
        for (PxArticleVo article : articles) {
            String content = article.getContent() != null ? article.getContent() : article.getRichText();
            double score = calculateScore(question, article.getTitle() + " " + content);
            if (score > 0.3) {
                results.add(KnowledgeSearchResult.builder()
                        .sourceType("blog")
                        .sourceId(article.getId() != null ? article.getId().longValue() : null)
                        .title(article.getTitle())
                        .content(excerpt(content, 500))
                        .score(BigDecimal.valueOf(score))
                        .build());
            }
        }

        return results.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public String buildKnowledgePrompt(String question, List<KnowledgeSearchResult> results) {
        if (results.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个知识库问答助手。请根据以下参考资料回答用户的问题。\n\n");
        sb.append("用户问题：").append(question).append("\n\n");
        sb.append("参考资料：\n");
        for (int i = 0; i < results.size(); i++) {
            KnowledgeSearchResult res = results.get(i);
            sb.append("[").append(i + 1).append("] 标题：").append(res.getTitle()).append("\n");
            sb.append("内容：").append(res.getContent()).append("\n\n");
        }
        sb.append("要求：\n");
        sb.append("1. 优先根据资料内容回答，不要编造。\n");
        sb.append("2. 如果资料中没有相关信息，请明确告知用户，并说明哪些内容来自资料、哪些是补充。\n");
        sb.append("3. 回答要简洁专业。");
        return sb.toString();
    }

    private double calculateScore(String question, String target) {
        if (question == null || question.isBlank() || target == null || target.isBlank()) {
            return 0.0;
        }
        String q = question.toLowerCase();
        String t = target.toLowerCase();

        if (t.contains(q)) {
            return 0.9;
        }

        String[] keywords = q.split("\\s+");
        int matches = 0;
        for (String kw : keywords) {
            if (kw.length() > 1 && t.contains(kw)) {
                matches++;
            }
        }

        return keywords.length == 0 ? 0.0 : (double) matches / keywords.length;
    }

    private String excerpt(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("<[^>]+>", "").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String buildReplyContent(Long ruleId) {
        List<PxCustomReplyContent> contents = customReplyContentMapper.selectPxCustomReplyContentByRuleId(ruleId);
        if (contents == null || contents.isEmpty()) {
            return "";
        }
        return contents.stream()
                .map(PxCustomReplyContent::getContent)
                .filter(content -> content != null && !content.isBlank())
                .collect(Collectors.joining("\n"));
    }
}
