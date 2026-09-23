package com.pnkx.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxBookkeepingAccount;
import com.pnkx.domain.po.PxBookkeepingClassification;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.mapper.PxBookkeepingAccountMapper;
import com.pnkx.mapper.PxBookkeepingClassificationMapper;
import com.pnkx.mapper.PxBookkeepingRecordMapper;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.function.Consumer;

/**
 * 记账 AI 能力服务
 * <p>
 * 从 PxBookkeepingRecordServiceImpl 拆出：消费分析（整包/流式）、
 * 一句话记账解析（整包/流式/批量）与 AI 提示词上下文构建。
 * 解析结果只做数据装配，不落库——是否写入由调用方决定。
 *
 * @author PHY
 */
@Service
public class PxBookkeepingAiService {

    private static final Logger logger = LoggerFactory.getLogger(PxBookkeepingAiService.class);

    /**
     * 消费分析的系统提示词
     */
    private static final String ANALYSIS_SYSTEM_PROMPT = "你是一个可以管理记账的AI助手。";

    /**
     * 一句话解析的系统提示词
     */
    private static final String PARSE_SYSTEM_PROMPT = "你是一个记账助手，请严格从给定的分类和账户中选择匹配项返回JSON。";

    /**
     * 批量解析的系统提示词
     */
    private static final String BATCH_PARSE_SYSTEM_PROMPT =
            "你是一个记账助手，请严格从给定的分类和账户中选择匹配项返回JSON数组。";

    /**
     * 流式响应结束标记
     */
    private static final String STREAM_DONE = "[DONE]";

    @Resource
    private PxBookkeepingRecordMapper pxBookkeepingRecordMapper;

    @Resource
    private PxBookkeepingClassificationMapper classificationMapper;

    @Resource
    private PxBookkeepingAccountMapper accountMapper;

    @Resource
    private AiClient aiClient;

    // ==================== 消费分析 ====================

    /**
     * AI 分析当月（或全部）记账数据
     *
     * @param isAll true 分析全部记录；false/null 仅当前登录用户当月记录
     */
    public JSONObject aiAnalysis(Boolean isAll) {
        String question = buildAnalysisQuestion(isAll);
        logger.info("AI账单分析，问题：{}", question);
        return aiClient.chat(ANALYSIS_SYSTEM_PROMPT, question);
    }

    /**
     * AI 流式分析当月（或全部）记账数据
     */
    public void aiAnalysisStream(Boolean isAll, Consumer<String> onChunk, Runnable onError) {
        String question = buildAnalysisQuestion(isAll);
        logger.info("AI账单流式分析，问题：{}", question);
        Flux<ChatResponse> stream = aiClient.chatStream(ANALYSIS_SYSTEM_PROMPT, question);
        stream.subscribe(
                resp -> emitTextChunks(resp, onChunk),
                error -> {
                    logger.error("AI流式分析失败: {}", error.getMessage());
                    onError.run();
                },
                () -> emitDone(onChunk)
        );
    }

    /**
     * 构建"请分析当月记账数据"问题文本（分析/流式分析共用）
     */
    private String buildAnalysisQuestion(Boolean isAll) {
        PxBookkeepingRecord pxBookkeepingRecord = new PxBookkeepingRecord();
        pxBookkeepingRecord.setPayTime(DateUtils.getNowDate());
        if (isAll == null || !isAll) {
            try {
                pxBookkeepingRecord.setCreateBy(SecurityUtils.getUserId());
            } catch (Exception e) {
                logger.warn("无法获取当前用户ID，可能在Webhook非登录上下文中被调用", e);
            }
        }
        List<JSONObject> list = pxBookkeepingRecordMapper.getNaturalLanguageList(pxBookkeepingRecord);
        StringBuilder question = new StringBuilder("请分析当月的记账数据：\n");
        for (JSONObject jsonObject : list) {
            question.append(jsonObject.getString("日期")).append(" ")
                    .append(jsonObject.getString("分类")).append(" ")
                    .append(jsonObject.getString("类型")).append(" ")
                    .append(jsonObject.getString("账户")).append(" ")
                    .append(jsonObject.getString("金额")).append(" ")
                    .append(jsonObject.getString("备注") == null ? "" : jsonObject.getString("备注"))
                    .append("\n");
        }
        return question.toString();
    }

    // ==================== 一句话记账解析 ====================

    /**
     * AI 解析一句话记账，返回装配了分类/账户对象的 JSON（失败返回空对象）
     */
    public JSONObject aiParse(String text) {
        ParseContext ctx = loadParseContext();
        JSONObject result = aiClient.chat(PARSE_SYSTEM_PROMPT, buildParsePrompt(text, ctx));
        if (result == null) {
            return new JSONObject();
        }
        try {
            String content = result.getString("content");
            if (content == null || content.trim().isEmpty()) {
                return new JSONObject();
            }
            String finalJson = extractSingleJson(content);
            if (finalJson == null) {
                logger.error("未找到有效的JSON对象，content: {}", content);
                return new JSONObject();
            }
            logger.info("最终JSON: {}", finalJson);
            JSONObject parsed = JSON.parseObject(finalJson);
            if (parsed == null || parsed.isEmpty()) {
                return new JSONObject();
            }
            fillTypeAndAccount(parsed, parsed.getLong("typeId"), parsed.getLong("accountId"));
            parsed.put("payTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", DateUtils.getNowDate()));
            return parsed;
        } catch (Exception e) {
            logger.error("AI解析失败: {}", text, e);
            return new JSONObject();
        }
    }

    /**
     * AI 流式解析一句话记账（只透传文本流，不做 JSON 装配）
     */
    public void aiParseStream(String text, Consumer<String> onChunk, Runnable onError) {
        ParseContext ctx = loadParseContext();
        Flux<ChatResponse> stream = aiClient.chatStream(PARSE_SYSTEM_PROMPT, buildParsePrompt(text, ctx));
        stream.subscribe(
                resp -> emitTextChunks(resp, onChunk),
                error -> {
                    logger.error("AI流式解析失败: {}", error.getMessage());
                    onError.run();
                },
                () -> emitDone(onChunk)
        );
    }

    /**
     * AI 批量解析多行记账文本，返回 {list: [...]}（失败返回空对象）
     */
    public JSONObject aiBatchParse(String text) {
        ParseContext ctx = loadParseContext();
        JSONObject result = aiClient.chat(BATCH_PARSE_SYSTEM_PROMPT, buildBatchParsePrompt(text, ctx));
        if (result == null) {
            return new JSONObject();
        }
        try {
            String content = result.getString("content");
            if (content == null || content.trim().isEmpty()) {
                return new JSONObject();
            }
            String trimmed = content.trim();
            int jsonStartIdx = trimmed.indexOf('[');
            int jsonEndIdx = trimmed.lastIndexOf(']');
            if (jsonStartIdx < 0 || jsonEndIdx < jsonStartIdx) {
                logger.error("未找到有效的JSON数组，content: {}", trimmed);
                return new JSONObject();
            }
            String jsonStr = trimmed.substring(jsonStartIdx, jsonEndIdx + 1);
            List<JSONObject> parsedList = JSON.parseArray(jsonStr, JSONObject.class);
            for (JSONObject parsed : parsedList) {
                fillTypeAndAccount(parsed, parsed.getLong("type"), parsed.getLong("account"));
            }
            JSONObject finalResult = new JSONObject();
            finalResult.put("list", parsedList);
            return finalResult;
        } catch (Exception e) {
            logger.error("AI批量解析失败: {}", text, e);
            return new JSONObject();
        }
    }

    // ==================== 提示词与解析辅助 ====================

    /**
     * 一句话解析提示词
     */
    private String buildParsePrompt(String text, ParseContext ctx) {
        return "你是记账助手。当前用户昵称是：" + ctx.nickName() + "。用户输入一句话记账，请根据账户名称中包含的用户昵称选择最匹配的账户。\n\n" +
                "【分类】\n" + ctx.classContext() +
                "【账户】\n" + ctx.accountContext() +
                "返回格式（必须是标准JSON，不能有注释，不能有方括号，不能有思考过程）：\n" +
                "{\"type\":\"0表示收入，1表示支出\",\"money\":\"金额\",\"typeId\":分类id,\"accountId\":账户id,\"payTime\":\"yyyy-MM-dd HH:mm:ss\",\"remark\":\"备注\"}\n" +
                "只返回标准JSON字符串，不要任何其他内容。如果金额或分类无法识别，返回{}。账户必须选择一项（优先选择账户名称中包含当前用户昵称的账户）。\n" +
                "用户输入：" + text;
    }

    /**
     * 批量解析提示词
     */
    private String buildBatchParsePrompt(String text, ParseContext ctx) {
        return "你是记账助手。当前用户昵称是：" + ctx.nickName() + "。用户输入多行文本记账，请根据账户名称中包含的用户昵称选择最匹配的账户。\n\n" +
                "【分类】\n" + ctx.classContext() +
                "【账户】\n" + ctx.accountContext() +
                "返回格式（必须是标准JSON数组，每个元素包含：typeDifference(0收入,1支出), money, type(分类id), account(账户id), payTime(yyyy-MM-dd HH:mm:ss), remark。不能有注释，不能有思考过程）：\n" +
                "[{\"typeDifference\":\"1\",\"money\":\"金额\",\"type\":分类id,\"account\":账户id,\"payTime\":\"yyyy-MM-dd HH:mm:ss\",\"remark\":\"备注\"}]\n" +
                "只返回标准JSON数组字符串，不要任何其他内容。账户必须选择一项（优先选择账户名称中包含当前用户昵称的账户）。\n" +
                "用户输入：\n" + text;
    }

    /**
     * 从 AI 返回文本中提取单个 JSON 对象：
     * 剥掉思考标签 → 截取首尾花括号 → 去掉 // 注释行 → 兜底取最后一组花括号
     *
     * @return 干净的 JSON 字符串；无法提取时返回 null
     */
    private String extractSingleJson(String content) {
        String trimmed = content.trim();
        int thinkEnd = trimmed.indexOf("<end_thinking>");
        if (thinkEnd >= 0) {
            trimmed = trimmed.substring(thinkEnd + "<end_thinking>".length());
        }
        int jsonStartIdx = trimmed.indexOf('{');
        if (jsonStartIdx < 0) {
            return null;
        }
        int jsonEnd = trimmed.lastIndexOf('}');
        if (jsonEnd <= jsonStartIdx) {
            return null;
        }
        String jsonStr = trimmed.substring(jsonStartIdx, jsonEnd + 1);

        StringBuilder cleanJson = new StringBuilder();
        for (String line : jsonStr.split("\n")) {
            int commentIdx = line.indexOf("//");
            if (commentIdx >= 0) {
                line = line.substring(0, commentIdx);
            }
            cleanJson.append(line).append("\n");
        }
        String noComment = cleanJson.toString();
        logger.info("去注释后: {}", noComment);

        int lastOpen = noComment.lastIndexOf('{');
        int lastClose = noComment.lastIndexOf('}');
        return lastOpen >= 0 && lastClose > lastOpen ? noComment.substring(lastOpen, lastClose + 1) : noComment;
    }

    /**
     * 装配分类/账户完整对象（及主分类名）到解析结果
     */
    private void fillTypeAndAccount(JSONObject parsed, Long typeId, Long accountId) {
        if (typeId != null) {
            PxBookkeepingClassification typeObj = classificationMapper.selectPxBookkeepingClassificationById(typeId);
            parsed.put("typeObject", typeObj);
            if (typeObj != null && typeObj.getTypeParentId() != null) {
                PxBookkeepingClassification primaryObj = classificationMapper.selectPxBookkeepingClassificationById(typeObj.getTypeParentId());
                parsed.put("primaryType", primaryObj != null ? primaryObj.getTypeName() : "");
                parsed.put("primaryTypeObject", primaryObj);
            }
            parsed.put("secondaryType", typeObj != null ? typeObj.getTypeName() : "");
        }
        if (accountId != null) {
            PxBookkeepingAccount accountObj = accountMapper.selectPxBookkeepingAccountById(accountId);
            parsed.put("accountObject", accountObj);
            parsed.put("account", accountObj != null ? accountObj.getAccountName() : "");
        }
    }

    // ==================== 上下文构建 ====================

    /**
     * 解析上下文：分类/账户提示词片段 + 用户昵称
     */
    private record ParseContext(String classContext, String accountContext, String nickName) {}

    /**
     * 加载分类和账户数据并构建上下文
     */
    private ParseContext loadParseContext() {
        PxBookkeepingClassification queryClass = new PxBookkeepingClassification();
        queryClass.setDelFlag(false);
        List<PxBookkeepingClassification> allClassifications = classificationMapper.selectPxBookkeepingClassificationList(queryClass);

        PxBookkeepingAccount queryAccount = new PxBookkeepingAccount();
        queryAccount.setDelFlag(false);
        List<PxBookkeepingAccount> allAccounts = accountMapper.selectPxBookkeepingAccountList(queryAccount);

        return new ParseContext(
                buildClassificationContext(allClassifications),
                buildAccountContext(allAccounts),
                SecurityUtils.getLoginUser().getUser().getNickName()
        );
    }

    /**
     * 构建分类上下文字符串
     */
    private String buildClassificationContext(List<PxBookkeepingClassification> allClassifications) {
        StringBuilder classContext = new StringBuilder();
        for (String diff : new String[]{"0", "1"}) {
            String diffName = "1".equals(diff) ? "支出" : "收入";
            classContext.append("【").append(diffName).append("】\n");
            for (PxBookkeepingClassification parent : allClassifications) {
                if (!"0".equals(parent.getTypeLevel()) || !diff.equals(parent.getTypeDifference())) {
                    continue;
                }
                classContext.append("  - ").append(parent.getTypeName()).append("(id=").append(parent.getId()).append(")\n");
                for (PxBookkeepingClassification child : allClassifications) {
                    if (!"1".equals(child.getTypeLevel()) || !diff.equals(child.getTypeDifference()) || !parent.getId().equals(child.getTypeParentId())) {
                        continue;
                    }
                    classContext.append("      · ").append(child.getTypeName()).append("(id=").append(child.getId()).append(")\n");
                }
            }
        }
        return classContext.toString();
    }

    /**
     * 构建账户上下文字符串
     */
    private String buildAccountContext(List<PxBookkeepingAccount> allAccounts) {
        StringBuilder sb = new StringBuilder();
        for (PxBookkeepingAccount acc : allAccounts) {
            sb.append("  - ").append(acc.getAccountName()).append("(id=").append(acc.getId()).append(")\n");
        }
        return sb.toString();
    }

    // ==================== 流式输出辅助 ====================

    /**
     * 从流式响应提取文本块并推送给回调
     */
    private void emitTextChunks(ChatResponse resp, Consumer<String> onChunk) {
        if (resp.getContent() == null) {
            return;
        }
        for (ContentBlock block : resp.getContent()) {
            if (block instanceof TextBlock textBlock) {
                String text = textBlock.getText();
                if (text != null && !text.isEmpty()) {
                    onChunk.accept(text);
                }
            }
        }
    }

    /**
     * 推送流结束标记
     */
    private void emitDone(Consumer<String> onChunk) {
        try {
            onChunk.accept(STREAM_DONE);
        } catch (Exception ignored) {
        }
    }
}
