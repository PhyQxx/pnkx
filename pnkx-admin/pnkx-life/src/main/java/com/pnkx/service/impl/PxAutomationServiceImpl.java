package com.pnkx.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.*;
import com.pnkx.mapper.*;
import com.pnkx.service.IPxAutomationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PxAutomationServiceImpl implements IPxAutomationService {
    @Resource private PxAutomationMapper automationMapper;
    @Resource private PxToDoMapper todoMapper;
    @Resource private PxDiaryMapper diaryMapper;
    @Resource private PxShoppingListMapper shoppingListMapper;
    @Resource private PxMealPlanMapper mealPlanMapper;
    @Resource private PxRecipeIngredientMapper ingredientMapper;
    @Resource private PxShoppingItemMapper shoppingItemMapper;
    @Resource private PxSubscriptionMapper subscriptionMapper;
    @Resource private PxBookkeepingRecordMapper recordMapper;
    @Resource private PxBookMapper bookMapper;

    @Override public List<PxAutomationRule> listRules() { return automationMapper.selectRules(SecurityUtils.getUserId()); }

    @Override
    public List<JSONObject> templates() {
        return List.of(
                template("subscription_charge", "订阅扣费闭环", "subscription_due", "subscription_charge", "到期订阅自动记账并推进扣费日"),
                template("meal_to_shopping", "餐饮转购物", "schedule", "meal_to_shopping", "把计划内菜谱食材加入购物清单"),
                template("commemoration_gift", "纪念日礼物计划", "schedule", "create_todo", "提前创建礼物准备待办"),
                template("reading_plan", "阅读计划", "schedule", "create_reading_todo", "按目标日期创建阅读待办"),
                template("todo_to_diary", "待办转日记素材", "todo_completed", "todo_to_diary", "将已完成待办沉淀为日记草稿")
        );
    }

    private JSONObject template(String code, String name, String trigger, String action, String description) {
        JSONObject item = new JSONObject();
        item.put("code", code); item.put("name", name); item.put("triggerType", trigger);
        item.put("actionType", action); item.put("description", description);
        return item;
    }

    @Override
    public PxAutomationRule saveRule(PxAutomationRule rule) {
        String userId = SecurityUtils.getUserId();
        validateRule(rule);
        rule.setUpdateBy(userId);
        rule.setUpdateTime(DateUtils.getNowDate());
        rule.setVersion(UUID.randomUUID().toString());
        if (rule.getId() != null) {
            if (automationMapper.selectRule(rule.getId(), userId) == null) throw new ServiceException("规则不存在或无权操作");
            automationMapper.updateRule(rule);
            return rule;
        }
        if (rule.getClientUuid() == null || rule.getClientUuid().isBlank()) rule.setClientUuid(UUID.randomUUID().toString());
        PxAutomationRule existing = automationMapper.selectRuleByClientUuid(rule.getClientUuid(), userId);
        if (existing != null) return existing;
        rule.setCreateBy(userId); rule.setCreateTime(DateUtils.getNowDate());
        if (rule.getEnabled() == null) rule.setEnabled(true);
        automationMapper.insertRule(rule);
        return rule;
    }

    @Override public int deleteRule(Long id) { return automationMapper.deleteRule(id, SecurityUtils.getUserId()); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PxAutomationExecution execute(Long ruleId, boolean dryRun, JSONObject input) {
        return executeInternal(ruleId, dryRun, input == null ? new JSONObject() : input, null, null, SecurityUtils.getUserId());
    }

    @Override
    public PxAutomationExecution retry(Long executionId) {
        String userId = SecurityUtils.getUserId();
        PxAutomationExecution old = automationMapper.selectExecution(executionId, userId);
        if (old == null || !"failed".equals(old.getStatus())) throw new ServiceException("仅失败记录可以重放");
        JSONObject input = old.getInputJson() == null ? new JSONObject() : JSON.parseObject(old.getInputJson());
        return executeInternal(old.getRuleId(), false, input, old.getId(), old.getIdempotencyKey(), userId);
    }

    @Override public List<PxAutomationExecution> listExecutions(Long ruleId) { return automationMapper.selectExecutions(ruleId, SecurityUtils.getUserId()); }

    @Override
    public List<PxAutomationExecution> trigger(String triggerType, JSONObject input) {
        List<PxAutomationExecution> results = new ArrayList<>();
        String userId = SecurityUtils.getUserId();
        for (PxAutomationRule rule : automationMapper.selectRules(userId)) {
            if (Boolean.TRUE.equals(rule.getEnabled()) && triggerType.equals(rule.getTriggerType())) {
                results.add(executeInternal(rule.getId(), false, input == null ? new JSONObject() : input, null, null, userId));
            }
        }
        return results;
    }

    @Override
    @Scheduled(cron = "0 * * * * ?")
    public int executeDueRules() {
        int success = 0;
        Date now = DateUtils.getNowDate();
        for (PxAutomationRule rule : automationMapper.selectDueRules(now)) {
            JSONObject input = new JSONObject();
            input.put("eventKey", "schedule:" + rule.getNextRunTime().getTime());
            PxAutomationExecution result = executeInternal(rule.getId(), false, input, null, null, rule.getCreateBy());
            if ("success".equals(result.getStatus())) success++;
        }
        return success;
    }

    private PxAutomationExecution executeInternal(Long ruleId, boolean dryRun, JSONObject input, Long retryOf, String originalKey, String userId) {
        PxAutomationRule rule = automationMapper.selectRule(ruleId, userId);
        if (rule == null) throw new ServiceException("规则不存在或无权操作");
        if (!Boolean.TRUE.equals(rule.getEnabled()) && !dryRun) throw new ServiceException("规则已停用");
        String eventKey = input.getString("eventKey");
        if (eventKey == null || eventKey.isBlank()) eventKey = UUID.randomUUID().toString();
        String executionKey = originalKey == null ? "exec:" + ruleId + ":" + eventKey : "retry:" + originalKey + ":" + UUID.randomUUID();
        PxAutomationExecution existing = automationMapper.selectExecutionByKey(executionKey);
        if (existing != null) return existing;
        String writeKey = stableWriteKey(ruleId, eventKey, originalKey);
        JSONObject config = parse(rule.getActionConfig());
        JSONObject plan = buildPlan(rule, config, input, writeKey);
        PxAutomationExecution execution = startExecution(rule, dryRun, input, plan, executionKey, retryOf, userId);
        if (!matchesConditions(parse(rule.getConditionJson()), input)) {
            execution.setStatus("skipped"); execution.setResultJson("{\"message\":\"条件不满足\"}");
            finish(execution, null); return execution;
        }
        if (dryRun) {
            execution.setStatus("preview"); execution.setResultJson("{\"message\":\"试运行未写入数据\"}");
            finish(execution, null); return execution;
        }
        try {
            JSONObject result = runAction(rule, config, input, writeKey, userId);
            execution.setStatus("success"); execution.setResultJson(result.toJSONString());
            finish(execution, null);
            automationMapper.updateRunTime(ruleId, userId, DateUtils.getNowDate(), nextRun(rule));
            return execution;
        } catch (Exception e) {
            execution.setStatus("failed");
            finish(execution, e.getMessage());
            return execution;
        }
    }

    private PxAutomationExecution startExecution(PxAutomationRule rule, boolean dryRun, JSONObject input, JSONObject plan,
                                                  String key, Long retryOf, String userId) {
        PxAutomationExecution e = new PxAutomationExecution();
        e.setRuleId(rule.getId()); e.setIdempotencyKey(key); e.setStatus("running"); e.setDryRun(dryRun);
        e.setInputJson(input.toJSONString()); e.setPlanJson(plan.toJSONString()); e.setRetryOf(retryOf);
        e.setStartTime(DateUtils.getNowDate()); e.setCreateTime(DateUtils.getNowDate()); e.setCreateBy(userId);
        automationMapper.insertExecution(e); return e;
    }

    private void finish(PxAutomationExecution execution, String error) {
        execution.setErrorMsg(error); execution.setFinishTime(DateUtils.getNowDate());
        automationMapper.updateExecution(execution);
    }

    private JSONObject runAction(PxAutomationRule rule, JSONObject config, JSONObject input, String writeKey, String userId) {
        return switch (rule.getActionType()) {
            case "subscription_charge" -> subscriptionCharge(config, writeKey, userId);
            case "meal_to_shopping" -> mealToShopping(config, writeKey, userId);
            case "create_todo" -> createTodo(config, input, writeKey, userId, false);
            case "create_reading_todo" -> createTodo(config, input, writeKey, userId, true);
            case "todo_to_diary" -> todoToDiary(config, input, writeKey, userId);
            default -> throw new ServiceException("不支持的自动化动作: " + rule.getActionType());
        };
    }

    private JSONObject subscriptionCharge(JSONObject config, String writeKey, String userId) {
        Long id = config.getLong("subscriptionId");
        PxSubscription sub = id == null ? null : subscriptionMapper.selectPxSubscriptionById(id);
        if (sub == null || !userId.equals(sub.getCreateBy())) throw new ServiceException("订阅不存在或无权操作");
        PxBookkeepingRecord record = new PxBookkeepingRecord();
        record.setAccount(sub.getAccountId()); record.setType(sub.getClassificationId());
        record.setMoney(sub.getAmount().toPlainString()); record.setPayTime(DateUtils.getNowDate());
        record.setClientUuid(writeKey); record.setVersion(writeKey); record.setCreateBy(userId);
        record.setCreateTime(DateUtils.getNowDate()); record.setRemark("自动化订阅出账：" + sub.getName()); record.setDelFlag(false);
        PxBookkeepingRecord existing = recordMapper.selectByClientUuid(writeKey);
        boolean inserted = existing == null;
        if (inserted) recordMapper.insertPxBookkeepingRecord(record); else record.setId(existing.getId());
        if (inserted && sub.getNextPaymentDate() != null) {
            LocalDate next = sub.getNextPaymentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int interval = sub.getCycleInterval() == null ? 1 : sub.getCycleInterval();
            next = switch (Optional.ofNullable(sub.getCycle()).orElse("monthly")) {
                case "daily" -> next.plusDays(interval);
                case "weekly" -> next.plusWeeks(interval);
                case "yearly" -> next.plusYears(interval);
                default -> next.plusMonths(interval);
            };
            PxSubscription update = new PxSubscription(); update.setId(sub.getId());
            update.setNextPaymentDate(java.sql.Date.valueOf(next)); update.setUpdateTime(DateUtils.getNowDate());
            subscriptionMapper.updatePxSubscription(update);
        }
        return result("recordId", record.getId());
    }

    private JSONObject mealToShopping(JSONObject config, String writeKey, String userId) {
        Long listId = config.getLong("listId");
        PxShoppingList list = listId == null ? null : shoppingListMapper.selectPxShoppingListById(listId);
        if (list == null || !userId.equals(list.getCreateBy())) throw new ServiceException("购物清单不存在或无权操作");
        String start = Optional.ofNullable(config.getString("startDate")).orElse(LocalDate.now().toString());
        String end = Optional.ofNullable(config.getString("endDate")).orElse(LocalDate.now().plusDays(6).toString());
        List<PxShoppingItem> items = new ArrayList<>();
        int index = 0;
        for (PxMealPlan plan : mealPlanMapper.selectByDateRangeForUser(start, end, userId)) {
            if (plan.getRecipeId() == null) continue;
            for (PxRecipeIngredient ingredient : ingredientMapper.selectByRecipeId(plan.getRecipeId())) {
                PxShoppingItem item = new PxShoppingItem(); item.setListId(listId); item.setName(ingredient.getName());
                item.setQuantity(ingredient.getQuantity()); item.setClassificationId(ingredient.getClassificationId());
                item.setChecked(false); item.setAddedFromMeal(true); item.setCreateBy(userId); item.setCreateTime(DateUtils.getNowDate());
                item.setClientUuid(UUID.nameUUIDFromBytes((writeKey + ":" + index++).getBytes(StandardCharsets.UTF_8)).toString());
                items.add(item);
            }
        }
        int inserted = 0;
        for (PxShoppingItem item : items) if (shoppingItemMapper.selectByClientUuid(item.getClientUuid()) == null) inserted += shoppingItemMapper.insertPxShoppingItem(item);
        return result("inserted", inserted);
    }

    private JSONObject createTodo(JSONObject config, JSONObject input, String writeKey, String userId, boolean reading) {
        PxToDo existing = todoMapper.selectByClientUuid(writeKey);
        if (existing != null) return result("todoId", existing.getId());
        String content = config.getString("content");
        if (reading) {
            Long bookId = config.getLong("bookId");
            PxBook book = bookId == null ? null : bookMapper.selectBookById(bookId, userId);
            if (book == null) throw new ServiceException("书籍不存在或无权操作");
            content = "阅读《" + book.getTitle() + "》";
        }
        if (content == null || content.isBlank()) content = input.getString("title");
        if (content == null || content.isBlank()) throw new ServiceException("待办内容不能为空");
        PxToDo todo = new PxToDo(); todo.setContent(content); todo.setStatus(false); todo.setPerformer(userId);
        todo.setPlanEndTime(config.getString("dueDate")); todo.setLabel(config.getString("label"));
        todo.setClientUuid(writeKey); todo.setVersion(writeKey); todo.setCreateBy(userId); todo.setCreateTime(DateUtils.getNowDate());
        todoMapper.insertPxToDo(todo); return result("todoId", todo.getId());
    }

    private JSONObject todoToDiary(JSONObject config, JSONObject input, String writeKey, String userId) {
        PxDiary existing = diaryMapper.selectByClientUuid(writeKey);
        if (existing != null) return result("diaryId", existing.getId());
        Long todoId = Optional.ofNullable(input.getLong("todoId")).orElse(config.getLong("todoId"));
        PxToDo todo = todoId == null ? null : todoMapper.selectPxToDoById(todoId);
        if (todo == null || !userId.equals(todo.getCreateBy()) || !Boolean.TRUE.equals(todo.getStatus())) throw new ServiceException("已完成待办不存在或无权操作");
        PxDiary diary = new PxDiary(); diary.setTitle("完成：" + todo.getContent()); diary.setContent(todo.getRemark() == null ? todo.getContent() : todo.getContent() + "\n" + todo.getRemark());
        diary.setDate(DateUtils.getNowDate()); diary.setClientUuid(writeKey); diary.setVersion(writeKey);
        diary.setCreateBy(userId); diary.setCreateTime(DateUtils.getNowDate()); diary.setDelFlag(0);
        diaryMapper.insertPxDiary(diary); return result("diaryId", diary.getId());
    }

    private JSONObject buildPlan(PxAutomationRule rule, JSONObject config, JSONObject input, String writeKey) {
        JSONObject plan = new JSONObject(); plan.put("rule", rule.getName()); plan.put("action", rule.getActionType());
        plan.put("conditions", parse(rule.getConditionJson())); plan.put("config", config); plan.put("input", input); plan.put("writeKey", writeKey); return plan;
    }
    private boolean matchesConditions(JSONObject conditions, JSONObject input) {
        if (conditions == null || conditions.isEmpty()) return true;
        if (Boolean.FALSE.equals(conditions.getBoolean("enabled"))) return false;
        JSONObject equals = conditions.getJSONObject("inputEquals");
        if (equals != null) {
            for (String key : equals.keySet()) if (!Objects.equals(equals.get(key), input.get(key))) return false;
        }
        List<Integer> days = conditions.getJSONArray("daysOfWeek") == null ? null
                : conditions.getJSONArray("daysOfWeek").toJavaList(Integer.class);
        return days == null || days.isEmpty() || days.contains(LocalDate.now().getDayOfWeek().getValue());
    }
    private JSONObject parse(String value) { return value == null || value.isBlank() ? new JSONObject() : JSON.parseObject(value); }
    static String stableWriteKey(Long ruleId, String eventKey, String originalExecutionKey) {
        String source = originalExecutionKey == null ? eventKey
                : originalExecutionKey.replaceFirst("^exec:" + ruleId + ":", "");
        return "auto:" + ruleId + ":" + source;
    }
    private Date nextRun(PxAutomationRule rule) {
        if (!"schedule".equals(rule.getTriggerType())) return rule.getNextRunTime();
        JSONObject trigger = parse(rule.getTriggerConfig());
        int minutes = Math.max(1, Optional.ofNullable(trigger.getInteger("intervalMinutes")).orElse(1440));
        LocalDateTime base = LocalDateTime.now();
        return Date.from(base.plusMinutes(minutes).atZone(ZoneId.systemDefault()).toInstant());
    }
    private JSONObject result(String key, Object value) { JSONObject r = new JSONObject(); r.put(key, value); return r; }
    private void validateRule(PxAutomationRule rule) {
        if (rule.getName() == null || rule.getName().isBlank()) throw new ServiceException("规则名称不能为空");
        if (rule.getActionType() == null || rule.getActionType().isBlank()) throw new ServiceException("动作类型不能为空");
        if (rule.getTriggerType() == null || rule.getTriggerType().isBlank()) rule.setTriggerType("manual");
        parse(rule.getTriggerConfig()); parse(rule.getConditionJson()); parse(rule.getActionConfig());
    }
}
