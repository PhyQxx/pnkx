package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.redis.RedisCache;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxBookkeepingBudget;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.mapper.PxBookkeepingBudgetMapper;
import com.pnkx.service.IPxBookkeepingBudgetService;
import com.pnkx.service.ReminderPushChannel;
import com.pnkx.framework.web.service.DataPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.ArrayList;

/**
 * 记账预算服务实现
 *
 * @author PHY
 * @date 2026-09-23
 */
@Service
public class PxBookkeepingBudgetServiceImpl implements IPxBookkeepingBudgetService {

    private static final Logger log = LoggerFactory.getLogger(PxBookkeepingBudgetServiceImpl.class);

    /**
     * 月度总预算的分类ID约定值
     */
    private static final long TOTAL_BUDGET_TYPE_ID = 0L;

    @Resource
    private PxBookkeepingBudgetMapper budgetMapper;

    @Resource
    private ReminderPushChannel pushChannel;

    @Resource
    private RedisCache redisCache;

    @Resource
    private DataPermissionService dataPermissionService;

    /**
     * 预算提醒防重复缓存前缀（每用户每月每预算每天最多一条）
     */
    private static final String BUDGET_ALERT_KEY = "budget_alert:";

    /**
     * 预算使用率达到该阈值时预警
     */
    private static final int ALERT_THRESHOLD_PERCENT = 90;

    @Override
    public List<PxBookkeepingBudget> listBudgets(String month) {
        String validMonth = requireMonth(month);
        List<PxBookkeepingBudget> result = new ArrayList<>();
        for (Long userId : visibleBudgetUsers()) {
            PxBookkeepingBudget query = new PxBookkeepingBudget();
            query.setMonth(validMonth);
            query.setCreateBy(String.valueOf(userId));
            result.addAll(budgetMapper.selectBudgetList(query));
        }
        return result;
    }

    @Override
    public List<PxBookkeepingBudget> getBudgetStatus(String month) {
        String validMonth = requireMonth(month);
        List<PxBookkeepingBudget> result = new ArrayList<>();
        for (Long userId : visibleBudgetUsers()) result.addAll(getBudgetStatusFor(validMonth, String.valueOf(userId)));
        return result;
    }

    @Override
    public int saveBudget(PxBookkeepingBudget budget) {
        if (StringUtils.isEmpty(budget.getMonth()) || !budget.getMonth().matches("\\d{4}-\\d{2}")) {
            throw new ServiceException("预算月份格式应为 yyyy-MM");
        }
        if (budget.getAmount() == null || budget.getAmount().signum() <= 0) {
            throw new ServiceException("预算金额必须大于 0");
        }
        if (budget.getTypeId() == null) {
            budget.setTypeId(TOTAL_BUDGET_TYPE_ID);
        }
        String ownerId = StringUtils.isEmpty(budget.getCreateBy()) ? SecurityUtils.getUserId() : budget.getCreateBy();
        if (!dataPermissionService.canWrite(ownerId, "budget")) throw new ServiceException("预算不存在或无协作权限");
        budget.setCreateBy(ownerId);
        PxBookkeepingBudget existed = budgetMapper.selectByMonthAndType(budget);
        if (existed != null) {
            budget.setId(existed.getId());
            existed.setAmount(budget.getAmount());
            existed.setUpdateBy(SecurityUtils.getUserId());
            existed.setUpdateTime(DateUtils.getNowDate());
            return budgetMapper.updateBudget(existed);
        }
        budget.setCreateTime(DateUtils.getNowDate());
        return budgetMapper.insertBudget(budget);
    }

    @Override
    public int deleteBudget(Long id) {
        PxBookkeepingBudget existing = budgetMapper.selectBudgetById(id);
        if (existing == null || !dataPermissionService.canWrite(existing.getCreateBy(), "budget"))
            throw new ServiceException("预算不存在或无协作权限");
        return budgetMapper.deleteBudgetById(id, existing.getCreateBy());
    }

    /**
     * 记账后的预算预警检查（异步执行，不阻塞记账）：
     * 总预算与该笔分类预算中，超支或使用率≥90% 时推送一次实时提醒（同一天同一预算不重复）
     *
     * @param record 刚写入的记账记录
     */
    public void checkBudgetAlertAfterRecord(PxBookkeepingRecord record) {
        if (record == null || record.getPayTime() == null) {
            return;
        }
        String month = DateUtils.parseDateToStr("yyyy-MM", record.getPayTime());
        String userId = record.getCreateBy();
        if (StringUtils.isEmpty(userId)) {
            return;
        }
        try {
            List<PxBookkeepingBudget> statusList = getBudgetStatusFor(month, userId);
            for (PxBookkeepingBudget budget : statusList) {
                // 只检查总预算与该笔分类相关的预算
                boolean related = budget.getTypeId() == TOTAL_BUDGET_TYPE_ID
                        || (record.getType() != null && record.getType().equals(budget.getTypeId()));
                if (!related) {
                    continue;
                }
                if (Boolean.TRUE.equals(budget.getExceeded())) {
                    pushBudgetAlertOnce(userId, month, budget, "exceeded");
                } else if (shouldWarn(budget)) {
                    pushBudgetAlertOnce(userId, month, budget, "warning");
                }
            }
        } catch (Exception e) {
            log.warn("预算预警检查失败（不影响记账）, userId={}", userId, e);
        }
    }

    /**
     * 推送一次预算提醒（当天同预算同级别已推送则跳过）
     */
    private void pushBudgetAlertOnce(String userId, String month, PxBookkeepingBudget budget, String level) {
        String key = BUDGET_ALERT_KEY + userId + ":" + month + ":" + budget.getTypeId() + ":" + level;
        if (redisCache.getCacheObject(key) != null) {
            return;
        }
        JSONObject payload = new JSONObject();
        payload.put("type", "budget_alert");
        payload.put("sourceType", "budget");
        payload.put("sourceId", budget.getId());
        boolean exceeded = "exceeded".equals(level);
        payload.put("title", exceeded ? "预算超支提醒" : "预算预警");
        payload.put("content", exceeded
                ? String.format("「%s」已超支：本月已用 %s / 预算 %s 元", budget.getTypeName(), budget.getUsed(), budget.getAmount())
                : String.format("「%s」本月已用 %s%%（%s / %s 元）", budget.getTypeName(), budget.getPercent(), budget.getUsed(), budget.getAmount()));
        payload.put("sendTime", DateUtils.dateTimeNow());
        pushChannel.push(userId, payload.toJSONString());
        // 当天有效：次日自然过期，可再次提醒
        redisCache.setCacheObject(key, 1, 1, TimeUnit.DAYS);
    }

    /**
     * 指定用户的预算状态（供无登录上下文的调用方使用）
     */
    private List<PxBookkeepingBudget> getBudgetStatusFor(String month, String userId) {
        PxBookkeepingBudget query = new PxBookkeepingBudget();
        query.setMonth(month);
        query.setCreateBy(userId);
        List<PxBookkeepingBudget> list = budgetMapper.selectBudgetStatus(query);
        list.forEach(b -> b.setExceeded(b.getUsed() != null
                && b.getUsed().compareTo(b.getAmount()) > 0));
        return list;
    }

    private String requireMonth(String month) {
        if (StringUtils.isEmpty(month) || !month.matches("\\d{4}-\\d{2}")) {
            throw new ServiceException("月份格式应为 yyyy-MM");
        }
        return month;
    }

    private List<Long> visibleBudgetUsers() {
        List<Long> ids = dataPermissionService.getVisibleUserIds("budget");
        return ids == null || ids.isEmpty() ? List.of(Long.valueOf(SecurityUtils.getUserId())) : ids;
    }

    static boolean shouldWarn(PxBookkeepingBudget budget) {
        return budget != null && !Boolean.TRUE.equals(budget.getExceeded())
                && budget.getPercent() != null && budget.getPercent() >= ALERT_THRESHOLD_PERCENT;
    }
}
