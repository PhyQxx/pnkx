package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.BaseEntity;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.*;
import com.pnkx.service.*;
import com.pnkx.service.IPxOfflineSyncService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 离线数据同步 Controller
 *
 * @author PHY
 */
@RestController
@RequestMapping("/offline")
public class PxOfflineController extends BaseController {

    @Resource
    private IPxOfflineSyncService offlineSyncService;

    @Resource
    private IPxDiaryService diaryService;

    @Resource
    private IPxToDoService toDoService;

    @Resource
    private IPxBookkeepingRecordService bookkeepingRecordService;

    @Resource
    private IPxNoteService noteService;

    @Resource
    private IPxCommemorationDayService commemorationDayService;

    @Resource
    private IPxLoversCardService loversCardService;

    /**
     * 批量提交离线操作（幂等）
     * 请求体示例：
     * {
     *   "operations": [
     *     { "tableName": "px_diary", "method": "POST", "payload": {...}, "clientUuid": "xxx" },
     *     { "tableName": "px_to_do", "method": "PUT", "payload": {...}, "clientUuid": "yyy" }
     *   ]
     * }
     */
    @Log(title = "离线批量同步", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchSubmit(@RequestBody Map<String, List<Map<String, Object>>> request) {
        List<Map<String, Object>> operations = request.get("operations");
        if (operations == null || operations.isEmpty()) {
            return AjaxResult.success("无操作需要同步");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (Map<String, Object> op : operations) {
            String tableName = (String) op.get("tableName");
            String method = (String) op.get("method");
            String clientUuid = (String) op.get("clientUuid");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) op.get("payload");

            try {
                Map<String, Object> result = processOneOperation(tableName, method, clientUuid, payload);
                results.add(result);
                String status = (String) result.get("status");
                if ("success".equals(status)) successCount++;
                else if ("skip".equals(status)) skipCount++;
                else failCount++;
            } catch (Exception e) {
                logger.error("离线同步单条失败: table={}, uuid={}", tableName, clientUuid, e);
                Map<String, Object> failResult = new HashMap<>();
                failResult.put("clientUuid", clientUuid);
                failResult.put("status", "fail");
                failResult.put("errorMsg", e.getMessage());
                results.add(failResult);
                failCount++;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", operations.size());
        summary.put("success", successCount);
        summary.put("skip", skipCount);
        summary.put("fail", failCount);
        summary.put("results", results);

        return AjaxResult.success(summary);
    }

    /**
     * 处理单条离线操作（幂等）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> processOneOperation(String tableName, String method,
                                                     String clientUuid, Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        result.put("clientUuid", clientUuid);

        if ("POST".equalsIgnoreCase(method)) {
            // POST 操作：走幂等新增
            Object id = null;
            switch (tableName) {
                case "px_diary":
                    id = offlineSyncService.insertDiaryIdempotent(payload, clientUuid);
                    break;
                case "px_todo":
                    id = offlineSyncService.insertToDoIdempotent(payload, clientUuid);
                    break;
                case "px_bookkeeping_record":
                    id = offlineSyncService.insertBookkeepingRecordIdempotent(payload, clientUuid);
                    break;
                case "px_note":
                    id = offlineSyncService.insertNoteIdempotent(payload, clientUuid);
                    break;
                case "px_commemoration_day":
                    id = offlineSyncService.insertCommemorationDayIdempotent(payload, clientUuid);
                    break;
                default:
                    result.put("status", "unsupported");
                    result.put("errorMsg", "不支持的表: " + tableName);
                    return result;
            }
            result.put("id", id);
            result.put("status", "success");

        } else if ("PUT".equalsIgnoreCase(method)) {
            // PUT 操作：直接走现有更新逻辑
            switch (tableName) {
                case "px_diary":
                    PxDiary diary = new PxDiary();
                    if (payload.get("id") != null) diary.setId(toLong(payload.get("id")));
                    diary.setTitle((String) payload.get("title"));
                    diary.setMood((String) payload.get("mood"));
                    diary.setWeather((String) payload.get("weather"));
                    diary.setContent((String) payload.get("content"));
                    diary.setRichText((String) payload.get("richText"));
                    diaryService.updatePxDiary(diary);
                    break;
                case "px_todo":
                    PxToDo todo = new PxToDo();
                    if (payload.get("id") != null) todo.setId(toLong(payload.get("id")));
                    todo.setContent((String) payload.get("content"));
                    todo.setPerformer((String) payload.get("performer"));
                    if (payload.get("status") != null) todo.setStatus(Boolean.parseBoolean(payload.get("status").toString()));
                    todo.setLabel((String) payload.get("label"));
                    toDoService.updatePxToDo(todo);
                    break;
                case "px_bookkeeping_record":
                    PxBookkeepingRecord record = new PxBookkeepingRecord();
                    if (payload.get("id") != null) record.setId(toLong(payload.get("id")));
                    if (payload.get("account") != null) record.setAccount(toLong(payload.get("account")));
                    if (payload.get("type") != null) record.setType(toLong(payload.get("type")));
                    record.setMoney((String) payload.get("money"));
                    bookkeepingRecordService.updatePxBookkeepingRecord(record);
                    break;
                case "px_note":
                    PxNote note = new PxNote();
                    if (payload.get("id") != null) note.setId(toLong(payload.get("id")));
                    note.setTitle((String) payload.get("title"));
                    note.setContent((String) payload.get("content"));
                    note.setRichText((String) payload.get("richText"));
                    if (payload.get("folder") != null) note.setFolder(toLong(payload.get("folder")));
                    noteService.updatePxNote(note);
                    break;
                case "px_commemoration_day":
                    PxCommemorationDay day = new PxCommemorationDay();
                    if (payload.get("id") != null) day.setId(toLong(payload.get("id")));
                    day.setName((String) payload.get("name"));
                    day.setIcon((String) payload.get("icon"));
                    if (payload.get("isRepeat") != null) day.setRepeat(Boolean.parseBoolean(payload.get("isRepeat").toString()));
                    commemorationDayService.updatePxCommemorationDay(day);
                    break;
                default:
                    result.put("status", "unsupported");
                    return result;
            }
            result.put("status", "success");

        } else if ("DELETE".equalsIgnoreCase(method)) {
            // DELETE 操作：根据 id 删除记录
            if (payload == null || payload.get("id") == null) {
                result.put("status", "fail");
                result.put("errorMsg", "DELETE 操作缺少 id");
                return result;
            }
            Long deleteId = toLong(payload.get("id"));
            switch (tableName) {
                case "px_diary":
                    diaryService.deletePxDiaryById(deleteId);
                    break;
                case "px_todo":
                    toDoService.deletePxToDoById(deleteId);
                    break;
                case "px_bookkeeping_record":
                    bookkeepingRecordService.deletePxBookkeepingRecordById(deleteId);
                    break;
                case "px_note":
                    noteService.deletePxNoteById(deleteId);
                    break;
                case "px_commemoration_day":
                    commemorationDayService.deletePxCommemorationDayById(deleteId);
                    break;
                case "px_lovers_card":
                    loversCardService.deletePxLoversCardById(deleteId);
                    break;
                default:
                    result.put("status", "unsupported");
                    result.put("errorMsg", "不支持的表: " + tableName);
                    return result;
            }
            result.put("status", "success");
        }

        return result;
    }

    // ──────────── 增量同步接口 ────────────

    @GetMapping("/sync/diary")
    public AjaxResult syncDiary(
            @RequestParam String since,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String userId = SecurityUtils.getUserId();
        List<PxDiary> items = offlineSyncService.selectDiaryIncremental(userId, since, offset, 50);
        return AjaxResult.success(buildSyncResult(items));
    }

    @GetMapping("/sync/todo")
    public AjaxResult syncTodo(
            @RequestParam String since,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String userId = SecurityUtils.getUserId();
        List<PxToDo> items = offlineSyncService.selectToDoIncremental(userId, since, offset, 50);
        return AjaxResult.success(buildSyncResult(items));
    }

    @GetMapping("/sync/record")
    public AjaxResult syncRecord(
            @RequestParam String since,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String userId = SecurityUtils.getUserId();
        List<PxBookkeepingRecord> items = offlineSyncService.selectRecordIncremental(userId, since, offset, 50);
        return AjaxResult.success(buildSyncResult(items));
    }

    @GetMapping("/sync/note")
    public AjaxResult syncNote(
            @RequestParam String since,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String userId = SecurityUtils.getUserId();
        List<PxNote> items = offlineSyncService.selectNoteIncremental(userId, since, offset, 50);
        return AjaxResult.success(buildSyncResult(items));
    }

    @GetMapping("/sync/commemorationDay")
    public AjaxResult syncCommemorationDay(
            @RequestParam String since,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String userId = SecurityUtils.getUserId();
        List<PxCommemorationDay> items = offlineSyncService.selectCommemorationDayIncremental(userId, since, offset, 50);
        return AjaxResult.success(buildSyncResult(items));
    }

    @GetMapping("/sync/card")
    public AjaxResult syncCard(
            @RequestParam String since,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String userId = SecurityUtils.getUserId();
        List<?> items = loversCardService.getCardByUserId();
        return AjaxResult.success(buildSyncResult(items));
    }

    @GetMapping("/sync/classification")
    public AjaxResult syncClassification(
            @RequestParam String since,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String userId = SecurityUtils.getUserId();
        List<PxBookkeepingClassification> items = offlineSyncService.selectClassificationIncremental(userId, since, offset, 50);
        return AjaxResult.success(buildSyncResult(items));
    }

    @GetMapping("/sync/account")
    public AjaxResult syncAccount(
            @RequestParam String since,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String userId = SecurityUtils.getUserId();
        List<PxBookkeepingAccount> items = offlineSyncService.selectAccountIncremental(userId, since, offset, 50);
        return AjaxResult.success(buildSyncResult(items));
    }

    // ──────────── 辅助方法 ────────────

    private static final SimpleDateFormat SYNC_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 构建增量同步响应
     * nextSince: 用于下次增量同步的时间游标（最后一条记录的 update_time）
     */
    private <T> Map<String, Object> buildSyncResult(List<T> items) {
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("hasMore", items.size() >= 50);

        // 计算下次同步的时间游标
        String nextSince = SYNC_DATE_FMT.format(new Date());
        if (!items.isEmpty()) {
            Object last = items.get(items.size() - 1);
            if (last instanceof BaseEntity) {
                Date updateTime = ((BaseEntity) last).getUpdateTime();
                if (updateTime != null) {
                    nextSince = SYNC_DATE_FMT.format(updateTime);
                }
            }
        }
        result.put("nextSince", nextSince);
        return result;
    }

    private Long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
