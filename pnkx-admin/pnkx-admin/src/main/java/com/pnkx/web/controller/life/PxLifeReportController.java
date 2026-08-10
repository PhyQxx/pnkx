package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.service.AiLifeReportDataService;
import com.pnkx.domain.po.PxLifeReportHistory;
import com.pnkx.mapper.PxLifeReportHistoryMapper;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 生活报告控制器 - 独立页面访问生活报告数据
 */
@RestController
@RequestMapping("/lifeReport")
public class PxLifeReportController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(PxLifeReportController.class);

    @Resource
    private AiLifeReportDataService lifeReportDataService;

    @Resource
    private AiClient aiClient;
    @Resource
    private PxLifeReportHistoryMapper reportHistoryMapper;

    @GetMapping("/history")
    public AjaxResult history(@RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return AjaxResult.success(reportHistoryMapper.selectRecent(SecurityUtils.getUserId().toString(), safeLimit));
    }

    /**
     * 获取生活报告结构化数据
     */
    @GetMapping("/data")
    public AjaxResult getReportData(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "summary") String reportType) {
        try {
            String userId = SecurityUtils.getUserId().toString();
            JSONObject data = lifeReportDataService.buildReportData(userId, period, reportType);
            return AjaxResult.success(data);
        } catch (Exception e) {
            logger.error("获取生活报告数据失败", e);
            return AjaxResult.error("获取报告数据失败");
        }
    }

    /**
     * SSE 流式生成生活报告
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamReport(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "summary") String reportType,
            HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        OutputStream out = response.getOutputStream();

        try {
            String userId = SecurityUtils.getUserId().toString();
            JSONObject reportData = lifeReportDataService.buildReportData(userId, period, reportType);
            StringBuilder generatedContent = new StringBuilder();

            String periodLabel = "week".equals(period) ? "周" : "月";
            String promptText = "你是一个生活分析专家。根据以下用户" + periodLabel + "度数据，生成一份生活报告。\n"
                    + "数据：" + reportData.toJSONString() + "\n\n"
                    + "报告要求：\n"
                    + "1. 消费总览与建议\n"
                    + "2. 心情与日记关键词\n"
                    + "3. 待办完成情况\n"
                    + "4. 下期建议\n"
                    + "请用 Markdown 格式输出，突出重点。";

            CountDownLatch latch = new CountDownLatch(1);
            Flux<ChatResponse> flux = aiClient.chatStream("你是一个专业的生活助理和数据分析师。", promptText);
            flux.subscribe(resp -> {
                if (resp.getContent() != null) {
                    for (ContentBlock block : resp.getContent()) {
                        if (block instanceof TextBlock textBlock) {
                            try {
                                String text = textBlock.getText();
                                generatedContent.append(text);
                                writeSse(out, text);
                            } catch (IOException e) {
                                logger.error("发送报告流式数据失败: {}", e.getMessage());
                            }
                        }
                    }
                }
            }, error -> {
                logger.error("生成生活报告流式调用出错: {}", error.getMessage());
                latch.countDown();
            }, () -> {
                try {
                    writeSse(out, "[DONE]");
                } catch (IOException ignored) {
                }
                latch.countDown();
            });

            try {
                latch.await(180, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!generatedContent.isEmpty()) {
                PxLifeReportHistory history = new PxLifeReportHistory();
                history.setUserId(userId);
                history.setPeriod(period);
                history.setReportType(reportType);
                history.setSource("manual");
                history.setContent(generatedContent.toString());
                reportHistoryMapper.insert(history);
            }
        } catch (Exception e) {
            logger.error("生成生活报告失败", e);
            writeSse(out, "生成报告失败，请稍后重试。");
            writeSse(out, "[DONE]");
        }
    }

    /**
     * 写 SSE 数据
     */
    private void writeSse(OutputStream out, String data) throws IOException {
        if (data == null) {
            return;
        }
        String[] lines = data.split("\r?\n");
        for (String line : lines) {
            out.write(("data:" + line + "\n").getBytes(StandardCharsets.UTF_8));
        }
        out.write("\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
