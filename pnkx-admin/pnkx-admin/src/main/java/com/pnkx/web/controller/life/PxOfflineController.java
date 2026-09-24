package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.BaseEntity;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.exception.ServiceException;
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
    private com.pnkx.mapper.PxExtendedOfflineMapper extendedOfflineMapper;

    private static final Map<String, String> EXTENDED_SYNC_TABLES = Map.ofEntries(
            Map.entry("shoppingList", "px_shopping_list"),
            Map.entry("shoppingItem", "px_shopping_item"),
            Map.entry("recipe", "px_recipe"),
            Map.entry("mealPlan", "px_meal_plan"),
            Map.entry("subscription", "px_subscription"),
            Map.entry("menstruation", "px_menstruation_record"),
            Map.entry("budget", "px_bookkeeping_budget"),
            Map.entry("recurring", "px_bookkeeping_recurring"),
            Map.entry("book", "px_book")
    );

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
            // PUT 操作：先校验记录归属，再映射完整业务字段。
            switch (tableName) {
                case "px_diary":
                    PxDiary diary = new PxDiary();
                    if (payload.get("id") != null) diary.setId(toLong(payload.get("id")));
                    requireOwner(diaryService.selectPxDiaryById(diary.getId()));
                    diary.setTitle((String) payload.get("title"));
                    diary.setMood((String) payload.get("mood"));
                    diary.setWeather((String) payload.get("weather"));
                    diary.setContent((String) payload.get("content"));
                    diary.setRichText((String) payload.get("richText"));
                    if (payload.get("date") != null) diary.setDate(com.pnkx.common.utils.DateUtils.parseDate(payload.get("date")));
                    diary.setRemark((String) payload.get("remark"));
                    diary.setUpdateBy(SecurityUtils.getUserId());
                    diaryService.updatePxDiary(diary);
                    break;
                case "px_todo":
                    PxToDo todo = new PxToDo();
                    if (payload.get("id") != null) todo.setId(toLong(payload.get("id")));
                    requireOwner(toDoService.selectPxToDoById(todo.getId()));
                    todo.setContent((String) payload.get("content"));
                    todo.setPerformer((String) payload.get("performer"));
                    if (payload.get("status") != null) todo.setStatus(Boolean.parseBoolean(payload.get("status").toString()));
                    todo.setLabel((String) payload.get("label"));
                    todo.setPlanStartTime((String) payload.get("planStartTime"));
                    todo.setPlanEndTime((String) payload.get("planEndTime"));
                    todo.setFinishBy((String) payload.get("finishBy"));
                    todo.setFinishTime((String) payload.get("finishTime"));
                    todo.setRemark((String) payload.get("remark"));
                    todo.setUpdateBy(SecurityUtils.getUserId());
                    toDoService.updatePxToDo(todo);
                    break;
                case "px_bookkeeping_record":
                    PxBookkeepingRecord record = new PxBookkeepingRecord();
                    if (payload.get("id") != null) record.setId(toLong(payload.get("id")));
                    requireOwner(bookkeepingRecordService.selectPxBookkeepingRecordById(record.getId()));
                    if (payload.get("account") != null) record.setAccount(toLong(payload.get("account")));
                    if (payload.get("otherAccount") != null) record.setOtherAccount(toLong(payload.get("otherAccount")));
                    if (payload.get("type") != null) record.setType(toLong(payload.get("type")));
                    record.setMoney((String) payload.get("money"));
                    record.setImages((String) payload.get("images"));
                    if (payload.get("payTime") != null) record.setPayTime(com.pnkx.common.utils.DateUtils.parseDate(payload.get("payTime")));
                    if (payload.get("commemorationDayId") != null) record.setCommemorationDayId(toLong(payload.get("commemorationDayId")));
                    record.setRemark((String) payload.get("remark"));
                    record.setUpdateBy(SecurityUtils.getUserId());
                    bookkeepingRecordService.updatePxBookkeepingRecord(record);
                    break;
                case "px_note":
                    PxNote note = new PxNote();
                    if (payload.get("id") != null) note.setId(toLong(payload.get("id")));
                    requireOwner(noteService.selectPxNoteById(note.getId()));
                    note.setTitle((String) payload.get("title"));
                    note.setContent((String) payload.get("content"));
                    note.setRichText((String) payload.get("richText"));
                    if (payload.get("folder") != null) note.setFolder(toLong(payload.get("folder")));
                    if (payload.get("order") != null) note.setOrder(Integer.parseInt(payload.get("order").toString()));
                    note.setRemark((String) payload.get("remark"));
                    note.setUpdateBy(SecurityUtils.getUserId());
                    noteService.updatePxNote(note);
                    break;
                case "px_commemoration_day":
                    PxCommemorationDay day = new PxCommemorationDay();
                    if (payload.get("id") != null) day.setId(toLong(payload.get("id")));
                    requireOwner(commemorationDayService.selectPxCommemorationDayById(day.getId()));
                    day.setName((String) payload.get("name"));
                    day.setIcon((String) payload.get("icon"));
                    if (payload.get("date") != null) day.setDate(com.pnkx.common.utils.DateUtils.parseDate(payload.get("date")));
                    Object repeat = payload.containsKey("repeat") ? payload.get("repeat") : payload.get("isRepeat");
                    if (repeat != null) day.setRepeat(Boolean.parseBoolean(repeat.toString()));
                    if (payload.get("orderNum") != null) day.setOrderNum(toLong(payload.get("orderNum")));
                    day.setRemark((String) payload.get("remark"));
                    day.setUpdateBy(SecurityUtils.getUserId());
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
                    requireOwner(diaryService.selectPxDiaryById(deleteId));
                    diaryService.deletePxDiaryById(deleteId);
                    break;
                case "px_todo":
                    requireOwner(toDoService.selectPxToDoById(deleteId));
                    toDoService.deletePxToDoById(deleteId);
                    break;
                case "px_bookkeeping_record":
                    requireOwner(bookkeepingRecordService.selectPxBookkeepingRecordById(deleteId));
                    bookkeepingRecordService.deletePxBookkeepingRecordById(deleteId);
                    break;
                case "px_note":
                    requireOwner(noteService.selectPxNoteById(deleteId));
                    noteService.deletePxNoteById(deleteId);
                    break;
                case "px_commemoration_day":
                    requireOwner(commemorationDayService.selectPxCommemorationDayById(deleteId));
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

    @GetMapping("/sync/extended/{module}")
    public AjaxResult syncExtended(@PathVariable String module,
                                   @RequestParam String since,
                                   @RequestParam(required = false, defaultValue = "0") Integer offset) {
        String table = EXTENDED_SYNC_TABLES.get(module);
        if (table == null) throw new ServiceException("不支持的同步模块");
        List<Map<String, Object>> items = extendedOfflineMapper.selectIncremental(
                table, SecurityUtils.getUserId(), since, Math.max(offset, 0), 50);
        Map<String, Object> result = buildSyncResult(items);
        if (!items.isEmpty()) {
            Object lastUpdated = items.get(items.size() - 1).get("update_time");
            if (lastUpdated == null) lastUpdated = items.get(items.size() - 1).get("create_time");
            if (lastUpdated instanceof Date) result.put("nextSince", SYNC_DATE_FMT.format((Date) lastUpdated));
        }
        return AjaxResult.success(result);
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

    private void requireOwner(BaseEntity entity) {
        if (entity == null || !Objects.equals(SecurityUtils.getUserId(), entity.getCreateBy())) {
            throw new ServiceException("无权同步该记录，记录不存在或不属于当前用户");
        }
    }
}
