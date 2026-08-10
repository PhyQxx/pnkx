package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.service.IPxDiaryService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 日记分析意图处理器
 * 用户说"分析我的日记"、"看看最近心情怎么样"、"日记情感分析"等时触发
 *
 * @author PHY
 */
@Component
public class DiaryAnalysisHandler implements IntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiaryAnalysisHandler.class);

    @Resource
    private IPxDiaryService diaryService;

    @Override
    public String intentName() {
        return "diary_analysis";
    }

    @Override
    public String promptDescription() {
        return "用户想分析日记心情、总结日记、回顾过去的内容。slots: {\"isAll\": true/false}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) throws IOException {
        boolean isAll = intentData.getBooleanValue("isAll");
        CountDownLatch latch = new CountDownLatch(1);

        diaryService.aiAnalysisStream(isAll,
                chunk -> {
                    try {
                        IntentHandler.writeSse(out, chunk);
                        if ("[DONE]".equals(chunk)) {
                            latch.countDown();
                        }
                    } catch (IOException e) {
                        logger.error("日记分析结果发送失败: {}", e.getMessage());
                        latch.countDown();
                    }
                },
                () -> {
                    try {
                        IntentHandler.writeSse(out, "分析日记数据时出错，请重试。");
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
