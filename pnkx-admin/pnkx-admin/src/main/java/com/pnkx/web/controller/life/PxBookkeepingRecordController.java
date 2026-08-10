package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.service.IPxBookkeepingRecordService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/bookkeeping/record")
public class PxBookkeepingRecordController extends BaseController {
    @Resource
    private IPxBookkeepingRecordService pxBookkeepingRecordService;

    /**
     * 获取AI分析结果
     * @return 描述
     */
    @RequestMapping("/aiAnalysis")
    public AjaxResult aiAnalysis(@RequestParam(value = "isAll", required = false, defaultValue = "false") Boolean isAll) {
        return AjaxResult.success(pxBookkeepingRecordService.aiAnalysis(isAll));
    }

    /**
     * 写SSE数据（符合SSE标准：多行文本每行加 data: 前缀）
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
     * AI账单分析（流式输出）
     * 直接写入HttpServletResponse，避免SseEmitter触发async dispatch导致AccessDenied
     */
    @GetMapping(value = "/aiAnalysis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void aiAnalysisStream(@RequestParam(value = "isAll", required = false, defaultValue = "false") Boolean isAll,
                                 HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        OutputStream out = response.getOutputStream();
        CountDownLatch latch = new CountDownLatch(1);

        pxBookkeepingRecordService.aiAnalysisStream(isAll,
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

    /**
     * AI解析自然语言为记账数据
     * @param text 用户输入的自然语言
     * @return 解析结果
     */
    @PostMapping("/aiParse")
    public AjaxResult aiParse(@RequestParam("text") String text) {
        return AjaxResult.success(pxBookkeepingRecordService.aiParse(text));
    }

    @PostMapping("/aiBatchParse")
    public AjaxResult aiBatchParse(@RequestParam("text") String text) {
        return AjaxResult.success(pxBookkeepingRecordService.aiBatchParse(text));
    }

    @GetMapping(value = "/aiParse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void aiParseStream(@RequestParam("text") String text, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        OutputStream out = response.getOutputStream();
        CountDownLatch latch = new CountDownLatch(1);

        pxBookkeepingRecordService.aiParseStream(text,
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
                        writeSse(out, "解析数据时出错，请重试。");
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

    /**
     * 查询账本记录列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxBookkeepingRecord pxBookkeepingRecord) {
        startPage();
        return pxBookkeepingRecordService.selectPxBookkeepingRecordList(pxBookkeepingRecord);
    }

    /**
     * 获取账本记录详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxBookkeepingRecordService.selectPxBookkeepingRecordById(id));
    }

    /**
     * 新增账本记录
     */
    @Log(title = "新增账本记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxBookkeepingRecord pxBookkeepingRecord) {
        int rows = pxBookkeepingRecordService.insertPxBookkeepingRecord(pxBookkeepingRecord);
        if (rows > 0) {
            return AjaxResult.success(pxBookkeepingRecord.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 批量新增账本记录
     */
    @Log(title = "批量新增账本记录", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult addBatch(@RequestBody java.util.List<PxBookkeepingRecord> list) {
        return toAjax(pxBookkeepingRecordService.insertBatchRecord(list));
    }

    /**
     * 修改账本记录
     */
    @Log(title = "修改账本记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxBookkeepingRecord pxBookkeepingRecord) {
        return toAjax(pxBookkeepingRecordService.updatePxBookkeepingRecord(pxBookkeepingRecord));
    }

    /**
     * 删除账本记录
     */
    @Log(title = "账本记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxBookkeepingRecordService.deletePxBookkeepingRecordByIds(ids));
    }
}
