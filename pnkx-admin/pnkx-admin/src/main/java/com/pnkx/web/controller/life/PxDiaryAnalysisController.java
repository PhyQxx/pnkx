package com.pnkx.web.controller.life;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.PxDiary;
import com.pnkx.service.IPxDiaryService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author PHY
 * @classname PxDiaryAnalysisController
 * @description 日记心情分析Controller
 */
@RestController
@RequestMapping("/diary/analysis")
public class PxDiaryAnalysisController extends BaseController {
    @Resource
    private IPxDiaryService pxDiaryService;

    /**
     * 获取日记心情分析数据
     * @param isAll true=分析所有日记，false=分析本月日记
     */
    @GetMapping("/data")
    public AjaxResult getData(@RequestParam(value = "isAll", required = false, defaultValue = "false") Boolean isAll,
                              @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                              @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                              @RequestParam(value = "timelineOnly", required = false, defaultValue = "false") Boolean timelineOnly) {
        PxDiary query = new PxDiary();

        // 构建心情分布
        Map<String, Long> moodCount = new LinkedHashMap<>();
        List<PxDiary> diaries = Collections.emptyList();
        if (!Boolean.TRUE.equals(timelineOnly)) {
            diaries = pxDiaryService.selectPxDiaryList(query);
            for (PxDiary diary : diaries) {
                String mood = diary.getMood();
                if (mood != null && !mood.isEmpty()) {
                    moodCount.merge(mood, 1L, Long::sum);
                }
            }
        }

        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        // timeline 分页参数来自 @RequestParam（非前端统一参数），手动构造 Page 并写入 ThreadLocal，
        // 由分页拦截器对单参数 selectPxDiaryList 自动分页
        Page<PxDiary> timelinePage = new Page<>(safePageNum, safePageSize);
        setPage(timelinePage);
        List<PxDiary> timelineDiaries = pxDiaryService.selectPxDiaryList(query);
        long timelineTotal = timelinePage.getTotal();
        boolean hasMore = timelinePage.getCurrent() < timelinePage.getPages();
        clearPage();
        List<Map<String, Object>> timeline = timelineDiaries.stream()
                .map(this::buildTimelineItem)
                .collect(Collectors.toList());

        List<Map<String, Object>> moodDistribution = moodCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", entry.getKey());
                    m.put("value", entry.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        if (!Boolean.TRUE.equals(timelineOnly)) {
            result.put("total", diaries.size());
            result.put("moodDistribution", moodDistribution);
        }
        result.put("timeline", timeline);
        result.put("timelineTotal", timelineTotal);
        result.put("pageNum", safePageNum);
        result.put("pageSize", safePageSize);
        result.put("hasMore", hasMore);

        return AjaxResult.success(result);
    }

    private Map<String, Object> buildTimelineItem(PxDiary diary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", diary.getId());
        item.put("date", diary.getDate() != null ? DateUtils.dateTime(diary.getDate()) : "");
        item.put("mood", diary.getMood() != null ? diary.getMood() : "");
        item.put("weather", diary.getWeather() != null ? diary.getWeather() : "");
        item.put("content", diary.getContent() != null ? diary.getContent() : "");
        item.put("contentHtml", diary.getRichText() != null && !diary.getRichText().isEmpty()
                ? diary.getRichText()
                : diary.getContent());
        item.put("title", diary.getTitle() != null ? diary.getTitle() : "");
        return item;
    }

    /**
     * 写SSE数据
     */
    private void writeSse(OutputStream out, String data) throws IOException {
        String[] lines = data.split("\n", -1);
        for (String line : lines) {
            out.write(("data: " + line + "\n").getBytes(StandardCharsets.UTF_8));
        }
        out.write("\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * AI日记心情分析（流式输出）
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void stream(@RequestParam(value = "isAll", required = false, defaultValue = "false") Boolean isAll,
                       HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        OutputStream out = response.getOutputStream();
        CountDownLatch latch = new CountDownLatch(1);

        pxDiaryService.aiAnalysisStream(isAll,
                chunk -> {
                    try {
                        writeSse(out, chunk);
                        if ("[DONE]".equals(chunk)) {
                            latch.countDown();
                        }
                    } catch (IOException e) {
                        logger.error("流式发送失败: {}", e.getMessage());
                        latch.countDown();
                    }
                },
                () -> {
                    try {
                        writeSse(out, "分析数据时出错，请重试。");
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
    }
}
