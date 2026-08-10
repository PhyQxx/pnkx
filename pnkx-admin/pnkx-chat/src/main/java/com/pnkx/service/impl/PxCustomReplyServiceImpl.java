package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.pnkx.ai.AiClient;
import com.pnkx.chat.domain.dto.KnowledgeSearchResult;
import com.pnkx.domain.po.PxCustomReplyContent;
import com.pnkx.domain.po.PxCustomReplyRule;
import com.pnkx.mapper.PxCustomReplyContentMapper;
import com.pnkx.mapper.PxCustomReplyRuleMapper;
import com.pnkx.service.IPxCustomReplyService;
import com.pnkx.service.IPxKnowledgeSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class PxCustomReplyServiceImpl implements IPxCustomReplyService {

    private static final String CURRENT_TIME_PLACEHOLDER = "{current_time}";
    private static final String TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Resource
    private PxCustomReplyRuleMapper pxCustomReplyRuleMapper;

    @Resource
    private PxCustomReplyContentMapper pxCustomReplyContentMapper;

    @Resource
    private IPxKnowledgeSearchService knowledgeSearchService;

    @Resource
    private AiClient aiClient;

    /**
     * 使用ThreadLocalRandom替代Random，性能更好且线程安全
     */
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Override
    public String searchKnowledgeAndReply(String question) {
        // 1. 先尝试精确匹配自定义回复
        String directReply = matchCustomReply(question);
        if (directReply != null) {
            return directReply;
        }

        // 2. 搜索知识库
        List<KnowledgeSearchResult> results = knowledgeSearchService.search(question, 3);
        if (results.isEmpty()) {
            return null;
        }

        KnowledgeSearchResult best = results.get(0);
        BigDecimal thresholdHigh = new BigDecimal("0.75");
        BigDecimal thresholdLow = new BigDecimal("0.40");

        if (best.getScore().compareTo(thresholdHigh) >= 0) {
            // 分数高，直接基于知识库回答
            String prompt = knowledgeSearchService.buildKnowledgePrompt(question, results);
            JSONObject aiResponse = aiClient.chat("你是一个知识库助手。", prompt);
            return aiResponse != null ? aiResponse.getString("content") : best.getContent();
        } else if (best.getScore().compareTo(thresholdLow) >= 0) {
            // 分数中等，作为上下文调用 AI
            String context = "参考知识：" + best.getTitle() + " - " + best.getContent();
            JSONObject aiResponse = aiClient.chat("你是一个智能助手，可以参考以下知识库内容。", context + "\n\n用户问题：" + question);
            return aiResponse != null ? aiResponse.getString("content") : null;
        }

        return null;
    }

    /**
     * 根据消息内容匹配自定义回复规则
     */
    @Override
    public String matchCustomReply(String messageContent) {
        if (!StringUtils.hasText(messageContent)) {
            return null;
        }

        // 获取启用的规则列表
        List<PxCustomReplyRule> enabledRules = getEnabledRules();
        if (CollectionUtils.isEmpty(enabledRules)) {
            return null;
        }

        // 按优先级排序（优先级高的先匹配）
        enabledRules.sort((r1, r2) -> {
            Long p1 = Objects.nonNull(r1.getPriority()) ? r1.getPriority() : 0L;
            Long p2 = Objects.nonNull(r2.getPriority()) ? r2.getPriority() : 0L;
            return Long.compare(p2, p1); // 降序排列
        });

        for (PxCustomReplyRule rule : enabledRules) {
            String reply = matchRule(rule, messageContent.trim());
            if (reply != null) {
                return reply;
            }
        }

        return null;
    }

    /**
     * 获取启用的规则列表
     */
    private List<PxCustomReplyRule> getEnabledRules() {
        PxCustomReplyRule param = new PxCustomReplyRule();
        param.setDeleted(0L);
        param.setEnabled(true);
        List<PxCustomReplyRule> rules = pxCustomReplyRuleMapper.selectPxCustomReplyRuleList(param);
        return Objects.nonNull(rules) ? rules : Collections.emptyList();
    }

    /**
     * 匹配单个规则
     */
    private String matchRule(PxCustomReplyRule rule, String messageContent) {
        try {
            List<String> keywords = parseKeywords(rule.getKeywords());
            if (CollectionUtils.isEmpty(keywords)) {
                return null;
            }

            for (String keyword : keywords) {
                if (isMatch(rule, keyword, messageContent)) {
                    return getRandomReplyContent(rule.getId());
                }
            }
        } catch (Exception e) {
            log.error("处理自定义回复规则时出错，规则ID: {}", rule.getId(), e);
        }
        return null;
    }

    /**
     * 解析关键词列表
     */
    private List<String> parseKeywords(String keywordsJson) {
        if (!StringUtils.hasText(keywordsJson)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(keywordsJson, String.class);
        } catch (Exception e) {
            log.error("解析关键词JSON失败: {}", keywordsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查是否匹配
     */
    private boolean isMatch(PxCustomReplyRule rule, String keyword, String messageContent) {
        if (Boolean.TRUE.equals(rule.getExactMatch())) {
            return messageContent.equals(keyword);
        } else {
            return messageContent.contains(keyword);
        }
    }

    /**
     * 根据规则ID随机获取回复内容
     */
    private String getRandomReplyContent(Long ruleId) {
        List<PxCustomReplyContent> contents = pxCustomReplyContentMapper.selectPxCustomReplyContentByRuleId(ruleId);
        if (CollectionUtils.isEmpty(contents)) {
            return null;
        }

        // 如果有权重，按权重随机选择
        if (hasWeightedContent(contents)) {
            return getWeightedRandomContent(contents);
        } else {
            // 无权重，均匀随机选择
            return processReplyContent(contents.get(random.nextInt(contents.size())).getContent());
        }
    }

    /**
     * 检查是否存在有权重的内容
     */
    private boolean hasWeightedContent(List<PxCustomReplyContent> contents) {
        return contents.stream()
                .anyMatch(content -> Objects.nonNull(content.getWeight()) && content.getWeight() > 1);
    }

    /**
     * 按权重随机选择回复内容
     */
    private String getWeightedRandomContent(List<PxCustomReplyContent> contents) {
        int totalWeight = calculateTotalWeight(contents);
        if (totalWeight <= 0) {
            log.warn("权重总和为0或负数，使用均匀随机选择");
            return processReplyContent(contents.get(random.nextInt(contents.size())).getContent());
        }

        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (PxCustomReplyContent content : contents) {
            int weight = getContentWeight(content);
            currentWeight += weight;
            if (randomValue < currentWeight) {
                return processReplyContent(content.getContent());
            }
        }

        // 理论上不会执行到这里，但为了安全返回第一个
        log.warn("权重随机选择异常，返回第一个内容");
        return processReplyContent(contents.get(0).getContent());
    }

    /**
     * 计算总权重
     */
    private int calculateTotalWeight(List<PxCustomReplyContent> contents) {
        return contents.stream()
                .mapToInt(this::getContentWeight)
                .sum();
    }

    /**
     * 获取内容权重，默认为1
     */
    private int getContentWeight(PxCustomReplyContent content) {
        return Objects.nonNull(content.getWeight()) ? content.getWeight() : 1;
    }

    /**
     * 处理回复内容中的变量
     */
    private String processReplyContent(String replyContent) {
        if (!StringUtils.hasText(replyContent)) {
            return replyContent;
        }

        // 替换时间变量
        if (replyContent.contains(CURRENT_TIME_PLACEHOLDER)) {
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT_PATTERN));
            replyContent = replyContent.replace(CURRENT_TIME_PLACEHOLDER, currentTime);
        }

        return replyContent;
    }

    /**
     * 查询自定义回复规则（包含回复内容列表）
     */
    @Override
    public PxCustomReplyRule selectPxCustomReplyRuleById(Long id) {
        if (Objects.isNull(id) || id <= 0) {
            log.warn("查询自定义回复规则时ID参数无效: {}", id);
            return null;
        }

        PxCustomReplyRule rule = pxCustomReplyRuleMapper.selectPxCustomReplyRuleById(id);
        if (Objects.nonNull(rule)) {
            enrichRuleWithContents(rule);
        }
        return rule;
    }

    /**
     * 查询自定义回复规则列表（包含回复内容列表）
     */
    @Override
    public List<PxCustomReplyRule> selectPxCustomReplyRuleList(PxCustomReplyRule pxCustomReplyRule) {
        List<PxCustomReplyRule> rules = pxCustomReplyRuleMapper.selectPxCustomReplyRuleList(pxCustomReplyRule);
        if (CollectionUtils.isEmpty(rules)) {
            return Collections.emptyList();
        }

        for (PxCustomReplyRule rule : rules) {
            enrichRuleWithContents(rule);
        }
        return rules;
    }

    /**
     * 为规则填充回复内容
     */
    private void enrichRuleWithContents(PxCustomReplyRule rule) {
        if (Objects.isNull(rule) || Objects.isNull(rule.getId())) {
            return;
        }

        List<PxCustomReplyContent> contents = pxCustomReplyContentMapper.selectPxCustomReplyContentByRuleId(rule.getId());
        rule.setReplyContents(Objects.nonNull(contents) ? contents : Collections.emptyList());
    }

    /**
     * 新增自定义回复规则（包含回复内容）
     */
    @Override
    public int insertPxCustomReplyRule(PxCustomReplyRule pxCustomReplyRule) {
        if (Objects.isNull(pxCustomReplyRule)) {
            log.warn("新增自定义回复规则时参数为空");
            return 0;
        }

        int result = pxCustomReplyRuleMapper.insertPxCustomReplyRule(pxCustomReplyRule);
        if (result > 0) {
            insertReplyContents(pxCustomReplyRule);
        }
        return result;
    }

    /**
     * 修改自定义回复规则（包含回复内容）
     */
    @Override
    public int updatePxCustomReplyRule(PxCustomReplyRule pxCustomReplyRule) {
        if (Objects.isNull(pxCustomReplyRule) || Objects.isNull(pxCustomReplyRule.getId())) {
            log.warn("修改自定义回复规则时参数无效");
            return 0;
        }

        // 先删除原有回复内容
        pxCustomReplyContentMapper.deletePxCustomReplyContentByRuleId(pxCustomReplyRule.getId());

        int result = pxCustomReplyRuleMapper.updatePxCustomReplyRule(pxCustomReplyRule);
        if (result > 0) {
            insertReplyContents(pxCustomReplyRule);
        }
        return result;
    }

    /**
     * 插入回复内容
     */
    private void insertReplyContents(PxCustomReplyRule rule) {
        if (CollectionUtils.isEmpty(rule.getReplyContents())) {
            return;
        }

        for (PxCustomReplyContent content : rule.getReplyContents()) {
            content.setRuleId(rule.getId());
            pxCustomReplyContentMapper.insertPxCustomReplyContent(content);
        }
    }

    /**
     * 批量删除自定义回复规则（同时删除关联的回复内容）
     */
    @Override
    public int deletePxCustomReplyRuleByIds(Long[] ids) {
        if (Objects.isNull(ids) || ids.length == 0) {
            log.warn("批量删除自定义回复规则时ID数组为空");
            return 0;
        }

        // 先删除关联的回复内容
        for (Long id : ids) {
            if (Objects.nonNull(id) && id > 0) {
                pxCustomReplyContentMapper.deletePxCustomReplyContentByRuleId(id);
            }
        }

        return pxCustomReplyRuleMapper.deletePxCustomReplyRuleByIds(ids);
    }

    /**
     * 删除自定义回复规则信息
     */
    @Override
    public int deletePxCustomReplyRuleById(Long id) {
        if (Objects.isNull(id) || id <= 0) {
            log.warn("删除自定义回复规则时ID参数无效: {}", id);
            return 0;
        }

        // 先删除关联的回复内容
        pxCustomReplyContentMapper.deletePxCustomReplyContentByRuleId(id);

        return pxCustomReplyRuleMapper.deletePxCustomReplyRuleById(id);
    }
}
