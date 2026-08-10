package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.service.IPxBookkeepingRecordService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 消费分析意图处理器
 * 用户说"分析本月消费"、"查看账单"、"这个月花了多少"等时触发
 *
 * @author PHY
 */
@Component
public class AnalysisHandler implements IntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisHandler.class);

    @Resource
    private IPxBookkeepingRecordService bookkeepingRecordService;

    @Override
    public String intentName() {
        return "analysis";
    }

    @Override
    public String promptDescription() {
        return "用户想进行账单分析、查询消费记录、查看本月花了多少钱。slots: {\"isAll\": true/false}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) throws IOException {
        boolean isAll = intentData.getBooleanValue("isAll");
        CountDownLatch latch = new CountDownLatch(1);

        bookkeepingRecordService.aiAnalysisStream(isAll,
                chunk -> {
                    try {
                        IntentHandler.writeSse(out, chunk);
                        if ("[DONE]".equals(chunk)) {
                            latch.countDown();
                        }
                    } catch (IOException e) {
                        logger.error("分析结果发送失败: {}", e.getMessage());
                        latch.countDown();
                    }
                },
                () -> {
                    try {
                        IntentHandler.writeSse(out, "分析数据时出错，请重试。");
                    } catch (IOException e) {
                        logger.error("错误消息发送失败: {}", e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
        );

        try {
            latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}
