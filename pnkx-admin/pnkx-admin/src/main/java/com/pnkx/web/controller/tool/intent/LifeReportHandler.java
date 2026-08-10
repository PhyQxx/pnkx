package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.service.AiLifeReportDataService;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 生活周报/月报处理器
 */
@Component
public class LifeReportHandler implements IntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(LifeReportHandler.class);

    @Resource
    private AiClient aiClient;

    @Resource
    private AiLifeReportDataService lifeReportDataService;

    @Override
    public String intentName() {
        return "life_report";
    }

    @Override
    public String promptDescription() {
        return "用户想看生活周报、月报、消费复盘或心情总结。slots: {\"period\": \"week/month\", \"reportType\": \"summary/expense/mood\"}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) throws IOException {
        String period = intentData.getString("period");
        String reportType = intentData.getString("reportType");
        String userId = SecurityUtils.getUserId().toString();

        JSONObject reportData = lifeReportDataService.buildReportData(userId, period, reportType);

        StringBuilder context = new StringBuilder();
        context.append("你是一个生活分析专家。根据以下用户数据，生成一份").append("week".equals(period) ? "周" : "月").append("度生活报告。\n");
        context.append("数据如下：\n").append(reportData.toJSONString()).append("\n\n");
        context.append("报告要求：\n");
        context.append("1. 开头一句风趣的总览。\n");
        context.append("2. 分析消费概览（如果有数据）。\n");
        context.append("3. 总结情绪或日记关键词。\n");
        context.append("4. 统计待办完成情况。\n");
        context.append("5. 提供下个周期的建议。\n");
        context.append("6. 如果有生理期数据，只能做生活记录提醒，严禁提供医疗建议。\n");
        context.append("请用 Markdown 格式输出，突出重点。");

        CountDownLatch latch = new CountDownLatch(1);
        aiClient.chatStream("你是一个专业的生活助理和数据分析师。", context.toString())
                .subscribe(resp -> {
                    if (resp.getContent() != null) {
                        for (ContentBlock block : resp.getContent()) {
                            if (block instanceof TextBlock textBlock) {
                                try {
                                    IntentHandler.writeSse(out, textBlock.getText());
                                } catch (IOException e) {
                                    logger.error("发送报告失败: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }, error -> {
                    logger.error("生成生活报告流式调用出错: {}", error.getMessage());
                    latch.countDown();
                }, () -> {
                    try {
                        IntentHandler.writeSse(out, "[DONE]");
                    } catch (IOException ignored) {}
                    latch.countDown();
                });

        try {
            latch.await(180, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}
