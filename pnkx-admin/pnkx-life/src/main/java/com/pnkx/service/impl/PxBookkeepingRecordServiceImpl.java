package com.pnkx.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.constant.HttpStatus;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxBookkeepingAccount;
import com.pnkx.domain.po.PxBookkeepingClassification;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.domain.po.PxCommemorationDay;
import com.pnkx.mapper.PxBookkeepingAccountMapper;
import com.pnkx.mapper.PxBookkeepingClassificationMapper;
import com.pnkx.mapper.PxBookkeepingRecordMapper;
import com.pnkx.service.IPxBookkeepingRecordService;
import com.pnkx.service.IPxCommemorationDayService;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * @author PHY
 * @classname PxBookkeepingRecordServiceImpl
 * @data 2021/11/18 0018 14:36
 * @description 描述
 */
@Service
public class PxBookkeepingRecordServiceImpl implements IPxBookkeepingRecordService {
    private static final Logger logger = LoggerFactory.getLogger(PxBookkeepingRecordServiceImpl.class);
    @Resource
    private PxBookkeepingRecordMapper pxBookkeepingRecordMapper;
    @Resource
    private AiClient aiClient;
    @Resource
    private PxBookkeepingClassificationMapper classificationMapper;
    @Resource
    private PxBookkeepingAccountMapper accountMapper;
    @Resource
    private IPxCommemorationDayService commemorationDayService;

    /**
     * 礼物类支出自动关联纪念日的匹配窗口（天数）
     */
    private static final long COMMEMORATION_MATCH_WINDOW_DAYS = 30L;

    @Override
    public PxBookkeepingRecord selectPxBookkeepingRecordById(Long id) {
        return pxBookkeepingRecordMapper.selectPxBookkeepingRecordById(id);
    }

    @Override
    public int insertPxBookkeepingRecord(PxBookkeepingRecord pxBookkeepingRecord) {
        pxBookkeepingRecord.setCreateTime(DateUtils.getNowDate());
        pxBookkeepingRecord.setCreateBy(SecurityUtils.getUserId());
        // 礼物类支出联动纪念日：用户未手动指定时，自动匹配消费时间附近最近的纪念日
        autoMatchCommemorationDay(pxBookkeepingRecord);
        return pxBookkeepingRecordMapper.insertPxBookkeepingRecord(pxBookkeepingRecord);
    }

    /**
     * 礼物类支出自动关联纪念日。
     * 仅当用户未手动指定 commemorationDayId，且消费分类名含"礼物"时触发：
     * 在消费时间前后 {@value #COMMEMORATION_MATCH_WINDOW_DAYS} 天内，选取日期最近的纪念日关联。
     *
     * @param record 记账记录
     */
    private void autoMatchCommemorationDay(PxBookkeepingRecord record) {
        if (record.getCommemorationDayId() != null) {
            // 用户已手动指定，尊重用户选择
            return;
        }
        if (record.getType() == null || record.getPayTime() == null) {
            return;
        }
        // 判断分类名是否含"礼物"
        PxBookkeepingClassification classification = classificationMapper.selectPxBookkeepingClassificationById(record.getType());
        if (classification == null || classification.getTypeName() == null
                || !classification.getTypeName().contains("礼物")) {
            return;
        }
        // 查找消费时间附近最近的纪念日
        List<PxCommemorationDay> days = commemorationDayService.getCommemorationDayList(new PxCommemorationDay());
        if (days == null || days.isEmpty()) {
            return;
        }
        Instant payInstant = record.getPayTime().toInstant();
        PxCommemorationDay nearest = null;
        long minDiff = Long.MAX_VALUE;
        for (PxCommemorationDay day : days) {
            if (day.getDate() == null || Boolean.TRUE.equals(day.getDelFlag())) {
                continue;
            }
            // 重复型纪念日取今年的日期比较
            Instant dayInstant = day.getDate().toInstant();
            if (Boolean.TRUE.equals(day.getRepeat())) {
                dayInstant = dayInstant.atZone(ZoneId.systemDefault())
                        .withYear(record.getPayTime().toInstant().atZone(ZoneId.systemDefault()).getYear())
                        .toInstant();
            }
            long diff = Math.abs(Duration.between(payInstant, dayInstant).toDays());
            if (diff <= COMMEMORATION_MATCH_WINDOW_DAYS && diff < minDiff) {
                minDiff = diff;
                nearest = day;
            }
        }
        if (nearest != null) {
            record.setCommemorationDayId(nearest.getId());
            logger.info("记账联动纪念日：支出自动关联「{}」（距离 {} 天）", nearest.getName(), minDiff);
        }
    }

    @Override
    public int insertBatchRecord(List<PxBookkeepingRecord> list) {
        int rows = 0;
        for (PxBookkeepingRecord record : list) {
            rows += insertPxBookkeepingRecord(record);
        }
        return rows;
    }

    @DataScopeSelf(alias = "r")
    @Override
    public TableDataInfo selectPxBookkeepingRecordList(PxBookkeepingRecord pxBookkeepingRecord) {
        List<PxBookkeepingRecord> list = pxBookkeepingRecordMapper.selectPxBookkeepingRecordList(pxBookkeepingRecord);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg(pxBookkeepingRecordMapper.getInflowMoney(pxBookkeepingRecord) + "," + pxBookkeepingRecordMapper.getFlowOutMoney(pxBookkeepingRecord));
        rspData.setRows(list);
        // total 由 ThreadLocal 拦截器回填到 BaseController 的 Page，这里从 getPage() 取
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PxBookkeepingRecord> page = com.pnkx.common.core.controller.BaseController.getPage();
        rspData.setTotal(page != null ? page.getTotal() : list.size());
        com.pnkx.common.core.controller.BaseController.clearPage();
        return rspData;
    }

    @DataScopeSelf(alias = "r")
    @Override
    public List<PxBookkeepingRecord> selectPxBookkeepingRecordAll(PxBookkeepingRecord pxBookkeepingRecord) {
        return pxBookkeepingRecordMapper.selectPxBookkeepingRecordList(pxBookkeepingRecord);
    }

    @Override
    public int updatePxBookkeepingRecord(PxBookkeepingRecord pxBookkeepingRecord) {
        pxBookkeepingRecord.setUpdateTime(DateUtils.getNowDate());
        pxBookkeepingRecord.setUpdateBy(SecurityUtils.getUserId());
        return pxBookkeepingRecordMapper.updatePxBookkeepingRecord(pxBookkeepingRecord);
    }

    @Override
    public int deletePxBookkeepingRecordByIds(Long[] ids) {
        return pxBookkeepingRecordMapper.deletePxBookkeepingRecordByIds(ids);
    }

    @Override
    public int deletePxBookkeepingRecordById(Long id) {
        return pxBookkeepingRecordMapper.deletePxBookkeepingRecordById(id);
    }

    @Override
    public JSONObject aiAnalysis() {
        return aiAnalysis(false);
    }

    @Override
    public JSONObject aiAnalysis(Boolean isAll) {
        StringBuilder question = new StringBuilder();
        PxBookkeepingRecord pxBookkeepingRecord = new PxBookkeepingRecord();
        pxBookkeepingRecord.setPayTime(DateUtils.getNowDate());
        
        if (isAll == null || !isAll) {
            // 确保使用当前登录用户的记录进行分析
            try {
                pxBookkeepingRecord.setCreateBy(SecurityUtils.getUserId());
            } catch (Exception e) {
                logger.warn("无法获取当前用户ID，可能在Webhook非登录上下文中被调用", e);
            }
        }

        List<JSONObject> list = pxBookkeepingRecordMapper.getNaturalLanguageList(pxBookkeepingRecord);
        question.append("请分析当月的记账数据：\n");
        for (JSONObject jsonObject : list) {
            question.append(jsonObject.getString("日期")).append(" ")
                    .append(jsonObject.getString("分类")).append(" ")
                    .append(jsonObject.getString("类型")).append(" ")
                    .append(jsonObject.getString("账户")).append(" ")
                    .append(jsonObject.getString("金额")).append(" ")
                    .append(jsonObject.getString("备注") == null ? "" : jsonObject.getString("备注"))
                    .append("\n");
        }
        logger.info("AI账单分析，问题：{}", question);
        return aiClient.chat("你是一个可以管理记账的AI助手。", question.toString());
    }

    @Override
    public void aiAnalysisStream(java.util.function.Consumer<String> onChunk, Runnable onError) {
        aiAnalysisStream(false, onChunk, onError);
    }

    @Override
    public void aiAnalysisStream(Boolean isAll, java.util.function.Consumer<String> onChunk, Runnable onError) {
        StringBuilder question = new StringBuilder();
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
        question.append("请分析当月的记账数据：\n");
        for (JSONObject jsonObject : list) {
            question.append(jsonObject.getString("日期")).append(" ")
                    .append(jsonObject.getString("分类")).append(" ")
                    .append(jsonObject.getString("类型")).append(" ")
                    .append(jsonObject.getString("账户")).append(" ")
                    .append(jsonObject.getString("金额")).append(" ")
                    .append(jsonObject.getString("备注") == null ? "" : jsonObject.getString("备注"))
                    .append("\n");
        }
        logger.info("AI账单流式分析，问题：{}", question);

        Flux<ChatResponse> stream = aiClient.chatStream("你是一个可以管理记账的AI助手。", question.toString());
        stream.subscribe(
                resp -> {
                    if (resp.getContent() != null) {
                        for (ContentBlock block : resp.getContent()) {
                            if (block instanceof TextBlock textBlock) {
                                String text = textBlock.getText();
                                if (text != null && !text.isEmpty()) {
                                    onChunk.accept(text);
                                }
                            }
                        }
                    }
                },
                error -> {
                    logger.error("AI流式分析失败: {}", error.getMessage());
                    onError.run();
                },
                () -> {
                    try {
                        onChunk.accept("[DONE]");
                    } catch (Exception ignored) {
                    }
                }
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

    /**
     * 加载分类和账户数据并构建上下文
     */
    private record ParseContext(String classContext, String accountContext, String nickName) {}

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

    @Override
    public JSONObject aiParse(String text) {
        ParseContext ctx = loadParseContext();

        String prompt = "你是记账助手。当前用户昵称是：" + ctx.nickName() + "。用户输入一句话记账，请根据账户名称中包含的用户昵称选择最匹配的账户。\n\n" +
                "【分类】\n" + ctx.classContext() +
                "【账户】\n" + ctx.accountContext() +
                "返回格式（必须是标准JSON，不能有注释，不能有方括号，不能有思考过程）：\n" +
                "{\"type\":\"0表示收入，1表示支出\",\"money\":\"金额\",\"typeId\":分类id,\"accountId\":账户id,\"payTime\":\"yyyy-MM-dd HH:mm:ss\",\"remark\":\"备注\"}\n" +
                "只返回标准JSON字符串，不要任何其他内容。如果金额或分类无法识别，返回{}。账户必须选择一项（优先选择账户名称中包含当前用户昵称的账户）。\n" +
                "用户输入：" + text;

        JSONObject result = aiClient.chat("你是一个记账助手，请严格从给定的分类和账户中选择匹配项返回JSON。", prompt);
        if (result == null) {
            return new JSONObject();
        }

        try {
            // AiClient返回 {"content": "...", "model": "..."}
            String content = result.getString("content");
            if (content == null || content.trim().isEmpty()) {
                return new JSONObject();
            }

            // 剥掉思考标签
            String trimmed = content.trim();
            int thinkEnd = trimmed.indexOf("<end_thinking>");
            if (thinkEnd >= 0) {
                String afterThink = trimmed.substring(thinkEnd + 16);
                int jsonStartIdx = afterThink.indexOf('{');
                if (jsonStartIdx >= 0) {
                    trimmed = afterThink.substring(jsonStartIdx).trim();
                }
            } else {
                int jsonStartIdx = trimmed.indexOf('{');
                if (jsonStartIdx >= 0) {
                    trimmed = trimmed.substring(jsonStartIdx).trim();
                }
            }

            int jsonStart = -1;
            int jsonEnd = -1;
            for (int i = 0; i < trimmed.length(); i++) {
                if (trimmed.charAt(i) == '{' || trimmed.charAt(i) == '[' ) {
                    jsonStart = i;
                    break;
                }
            }
            if (jsonStart >= 0) {
                jsonEnd = trimmed.lastIndexOf('}');
                if (jsonEnd == -1) jsonEnd = trimmed.lastIndexOf(']');
            }
            String jsonStr;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonStr = trimmed.substring(jsonStart, jsonEnd + 1);
            } else {
                logger.error("未找到有效的JSON对象，content: {}", trimmed);
                return new JSONObject();
            }

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
            String finalJson = lastOpen >= 0 && lastClose > lastOpen ? noComment.substring(lastOpen, lastClose + 1) : noComment;
            logger.info("最终JSON: {}", finalJson);
            JSONObject parsed = JSON.parseObject(finalJson);
            if (parsed == null || parsed.isEmpty()) {
                return new JSONObject();
            }

            Long typeId = parsed.getLong("typeId");
            Long accountId = parsed.getLong("accountId");
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

            parsed.put("payTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", DateUtils.getNowDate()));
            return parsed;
        } catch (Exception e) {
            logger.error("AI解析失败: {}", text, e);
            return new JSONObject();
        }
    }

    @Override
    public void aiParseStream(String text, java.util.function.Consumer<String> onChunk, Runnable onError) {
        ParseContext ctx = loadParseContext();

        String prompt = "你是记账助手。当前用户昵称是：" + ctx.nickName() + "。用户输入一句话记账，请根据账户名称中包含的用户昵称选择最匹配的账户。\n\n" +
                "【分类】\n" + ctx.classContext() +
                "【账户】\n" + ctx.accountContext() +
                "返回格式（必须是标准JSON，不能有注释，不能有方括号，不能有思考过程）：\n" +
                "{\"type\":\"0表示收入，1表示支出\",\"money\":\"金额\",\"typeId\":分类id,\"accountId\":账户id,\"payTime\":\"yyyy-MM-dd HH:mm:ss\",\"remark\":\"备注\"}\n" +
                "只返回标准JSON字符串，不要任何其他内容。如果金额或分类无法识别，返回{}。账户必须选择一项（优先选择账户名称中包含当前用户昵称的账户）。\n" +
                "用户输入：" + text;

        Flux<ChatResponse> stream = aiClient.chatStream("你是一个记账助手，请严格从给定的分类和账户中选择匹配项返回JSON。", prompt);
        stream.subscribe(
                resp -> {
                    if (resp.getContent() != null) {
                        for (ContentBlock block : resp.getContent()) {
                            if (block instanceof TextBlock textBlock) {
                                String chunk = textBlock.getText();
                                if (chunk != null && !chunk.isEmpty()) {
                                    onChunk.accept(chunk);
                                }
                            }
                        }
                    }
                },
                error -> {
                    logger.error("AI流式解析失败: {}", error.getMessage());
                    onError.run();
                },
                () -> {
                    try {
                        onChunk.accept("[DONE]");
                    } catch (Exception ignored) {
                    }
                }
        );
    }

    @Override
    public JSONObject aiBatchParse(String text) {
        ParseContext ctx = loadParseContext();

        String prompt = "你是记账助手。当前用户昵称是：" + ctx.nickName() + "。用户输入多行文本记账，请根据账户名称中包含的用户昵称选择最匹配的账户。\n\n" +
                "【分类】\n" + ctx.classContext() +
                "【账户】\n" + ctx.accountContext() +
                "返回格式（必须是标准JSON数组，每个元素包含：typeDifference(0收入,1支出), money, type(分类id), account(账户id), payTime(yyyy-MM-dd HH:mm:ss), remark。不能有注释，不能有思考过程）：\n" +
                "[{\"typeDifference\":\"1\",\"money\":\"金额\",\"type\":分类id,\"account\":账户id,\"payTime\":\"yyyy-MM-dd HH:mm:ss\",\"remark\":\"备注\"}]\n" +
                "只返回标准JSON数组字符串，不要任何其他内容。账户必须选择一项（优先选择账户名称中包含当前用户昵称的账户）。\n" +
                "用户输入：\n" + text;

        JSONObject result = aiClient.chat("你是一个记账助手，请严格从给定的分类和账户中选择匹配项返回JSON数组。", prompt);
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
            if (jsonStartIdx >= 0) {
                trimmed = trimmed.substring(jsonStartIdx).trim();
            }

            int jsonEndIdx = trimmed.lastIndexOf(']');
            String jsonStr;
            if (jsonStartIdx >= 0 && jsonEndIdx >= 0 && jsonEndIdx >= jsonStartIdx) {
                jsonStr = trimmed.substring(0, jsonEndIdx + 1);
            } else {
                logger.error("未找到有效的JSON数组，content: {}", trimmed);
                return new JSONObject();
            }

            List<JSONObject> parsedList = JSON.parseArray(jsonStr, JSONObject.class);
            for (JSONObject parsed : parsedList) {
                Long typeId = parsed.getLong("type");
                Long accountId = parsed.getLong("account");
                if (typeId != null) {
                    PxBookkeepingClassification typeObj = classificationMapper.selectPxBookkeepingClassificationById(typeId);
                    parsed.put("typeObject", typeObj);
                }
                if (accountId != null) {
                    PxBookkeepingAccount accountObj = accountMapper.selectPxBookkeepingAccountById(accountId);
                    parsed.put("accountObject", accountObj);
                }
            }
            JSONObject finalResult = new JSONObject();
            finalResult.put("list", parsedList);
            return finalResult;
        } catch (Exception e) {
            logger.error("AI批量解析失败: {}", text, e);
            return new JSONObject();
        }
    }
}
