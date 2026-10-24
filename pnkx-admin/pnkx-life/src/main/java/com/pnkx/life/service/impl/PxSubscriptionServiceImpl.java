package com.pnkx.life.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.life.domain.po.PxSubscription;
import com.pnkx.life.mapper.PxSubscriptionMapper;
import com.pnkx.life.service.IPxSubscriptionService;
import com.pnkx.life.service.IPxLifeReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 订阅管理 Service 实现
 *
 * @author PHY
 * @date 2026/07/05
 */
@Service
public class PxSubscriptionServiceImpl implements IPxSubscriptionService {

    @Resource private ChargeExecutionService chargeExecutions;

    private static final Logger log = LoggerFactory.getLogger(PxSubscriptionServiceImpl.class);

    @Resource
    private PxSubscriptionMapper pxSubscriptionMapper;
    @Resource
    private SubscriptionChargeService subscriptionChargeService;
    @Resource
    private IPxLifeReminderService lifeReminderService;

    @Override
    public PxSubscription selectPxSubscriptionById(Long id) {
        return pxSubscriptionMapper.selectPxSubscriptionById(id);
    }

    @DataScopeSelf(onlySelf = true)
    @Override
    public List<PxSubscription> selectPxSubscriptionList(PxSubscription pxSubscription) {
        return pxSubscriptionMapper.selectPxSubscriptionList(pxSubscription);
    }


    @Override
    public int insertPxSubscription(PxSubscription pxSubscription) {
        if (StringUtils.isNotEmpty(pxSubscription.getClientUuid())) {
            PxSubscription existing = pxSubscriptionMapper.selectByClientUuid(
                    pxSubscription.getClientUuid());
            if (existing != null) {
                requireOwner(existing);
                pxSubscription.setId(existing.getId());
                return 1;
            }
        }
        validateSchedule(pxSubscription);
        pxSubscription.setLegacyResumePolicy(false);
        applyLifecycle(pxSubscription,null);
        pxSubscription.setPostingMode(ChargeInstanceService.mode(pxSubscription.getPostingMode(), "manual_confirm"));
        pxSubscription.setAnchorDay(SubscriptionSchedule.date(pxSubscription.getNextPaymentDate()).getDayOfMonth());
        pxSubscription.setCreateBy(SecurityUtils.getUserId());
        pxSubscription.setCreateTime(DateUtils.getNowDate());
        return pxSubscriptionMapper.insertPxSubscription(pxSubscription);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor=Exception.class)
    public int updatePxSubscription(PxSubscription pxSubscription) {
        var current=pxSubscriptionMapper.selectForCharge(pxSubscription.getId());
        requireOwner(current);
        boolean explicitDateChange=pxSubscription.getNextPaymentDate()!=null&&!SubscriptionSchedule.date(pxSubscription.getNextPaymentDate()).equals(SubscriptionSchedule.date(current.getNextPaymentDate()));
        applyLifecycle(pxSubscription,current);
        if (pxSubscription.getPostingMode() != null) ChargeInstanceService.mode(pxSubscription.getPostingMode(), "auto_record");
        if (pxSubscription.getNextPaymentDate() != null) {
            PxSubscription existing = pxSubscriptionMapper.selectPxSubscriptionById(pxSubscription.getId());
            if (explicitDateChange) {
                pxSubscription.setAnchorDay(SubscriptionSchedule.date(pxSubscription.getNextPaymentDate()).getDayOfMonth());
            } else {
                pxSubscription.setAnchorDay(existing.getAnchorDay());
            }
        } else {
            pxSubscription.setAnchorDay(null);
        }
        if (pxSubscription.getAmount() != null && pxSubscription.getAmount().signum() < 0) throw new ServiceException("金额不能小于 0");
        if (pxSubscription.getCycleInterval() != null && pxSubscription.getCycleInterval() < 1) throw new ServiceException("周期间隔必须大于 0");
        if (pxSubscription.getCycle() != null) SubscriptionSchedule.validateCycle(pxSubscription.getCycle());
        pxSubscription.setUpdateTime(DateUtils.getNowDate());
        int rows = pxSubscriptionMapper.updatePxSubscription(pxSubscription);
        if (Boolean.FALSE.equals(pxSubscription.getEnabled())) {
            lifeReminderService.unbindReminder("subscription", pxSubscription.getId(), SecurityUtils.getUserId());
        }
        return rows;
    }

    @Override
    public int deletePxSubscriptionByIds(Long[] ids) {
        for (Long id : ids) requireOwner(pxSubscriptionMapper.selectPxSubscriptionById(id));
        int rows = pxSubscriptionMapper.deletePxSubscriptionByIds(ids);
        for (Long id : ids) lifeReminderService.unbindReminder("subscription", id, SecurityUtils.getUserId());
        return rows;
    }

    @Override
    public int deletePxSubscriptionById(Long id) {
        requireOwner(pxSubscriptionMapper.selectPxSubscriptionById(id));
        int rows = pxSubscriptionMapper.deletePxSubscriptionById(id);
        lifeReminderService.unbindReminder("subscription", id, SecurityUtils.getUserId());
        return rows;
    }

    private void applyLifecycle(PxSubscription input,PxSubscription old) {
        String state=input.getLifecycle();
        if(state==null)state=input.getEnabled()==null?(old==null?"active":old.getLifecycle()):(Boolean.TRUE.equals(input.getEnabled())?"active":"paused");
        if(state==null)state="active";
        if(!java.util.Set.of("active","paused","cancelled").contains(state))throw new ServiceException("无效订阅状态");
        if(old!=null&&"cancelled".equals(old.getLifecycle())&&!"cancelled".equals(state))throw new ServiceException("已终止订阅不可恢复，请新建规则");
        input.setLifecycle(state);input.setEnabled("active".equals(state));
        if(old!=null&&"active".equals(state)&&!Boolean.TRUE.equals(old.getEnabled())&&!Boolean.TRUE.equals(old.getLegacyResumePolicy())){
            var due=SubscriptionSchedule.date(input.getNextPaymentDate()==null?old.getNextPaymentDate():input.getNextPaymentDate());
            var today=LocalDate.now(SubscriptionSchedule.ZONE);
            int guard=0;while(!due.isAfter(today)&&guard++<100000)due=SubscriptionSchedule.next(due,input.getCycle()==null?old.getCycle():input.getCycle(),input.getCycleInterval()==null?old.getCycleInterval():input.getCycleInterval(),old.getAnchorDay());
            if(!due.isAfter(today))throw new ServiceException("恢复日期超出自动计算范围，请指定下次日期");
            input.setNextPaymentDate(java.sql.Date.valueOf(due));
        }
    }

    private void requireOwner(PxSubscription entity) {
        if (entity == null || !Objects.equals(SecurityUtils.getUserId(), entity.getCreateBy())) {
            throw new ServiceException("记录不存在或无权操作");
        }
    }

    @Override
    public BigDecimal monthlyNormalized(PxSubscription sub) {
        if (sub == null || sub.getAmount() == null) return BigDecimal.ZERO;
        int interval = sub.getCycleInterval() == null ? 1 : sub.getCycleInterval();
        String cycle = sub.getCycle() == null ? "monthly" : sub.getCycle();
        switch (cycle) {
            case "daily":
                return sub.getAmount().multiply(BigDecimal.valueOf(30)).divide(BigDecimal.valueOf(interval), 2, RoundingMode.HALF_UP);
            case "weekly":
                return sub.getAmount().multiply(BigDecimal.valueOf(4.33)).divide(BigDecimal.valueOf(interval), 2, RoundingMode.HALF_UP);
            case "yearly":
                return sub.getAmount().divide(BigDecimal.valueOf(12L * interval), 2, RoundingMode.HALF_UP);
            case "monthly":
            default:
                return sub.getAmount().divide(BigDecimal.valueOf(interval), 2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public BigDecimal yearlyPredicted(PxSubscription sub) {
        if (sub == null || sub.getAmount() == null) return BigDecimal.ZERO;
        int interval = sub.getCycleInterval() == null ? 1 : sub.getCycleInterval();
        String cycle = sub.getCycle() == null ? "monthly" : sub.getCycle();
        switch (cycle) {
            case "daily":
                return sub.getAmount().multiply(BigDecimal.valueOf(365)).divide(BigDecimal.valueOf(interval), 2, RoundingMode.HALF_UP);
            case "weekly":
                return sub.getAmount().multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(interval), 2, RoundingMode.HALF_UP);
            case "monthly":
                return sub.getAmount().multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(interval), 2, RoundingMode.HALF_UP);
            case "yearly":
            default:
                return sub.getAmount().divide(BigDecimal.valueOf(interval), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * 调度入口：扫描到期订阅，自动生成记账条目并推进下次扣费日期。
     * <p>
     * 对每个 next_payment_date <= today 的启用订阅：
     * 1. 生成一条 px_bookkeeping_record 支出记录
     * 2. 推进 next_payment_date 到下一个周期
     * 3. 单轮最多处理 100 期，剩余积压保留至下轮
     */
    @Override
    public int generateDueRecords() {
        LocalDate today = LocalDate.now(SubscriptionSchedule.ZONE);
        List<PxSubscription> dueList = pxSubscriptionMapper.selectDueSubscriptions(java.sql.Date.valueOf(today));
        int count = 0;
        for (PxSubscription sub : dueList) {
            LocalDate due = SubscriptionSchedule.date(sub.getNextPaymentDate());
            try {
                // 每一期单独事务；失败不推进，积压超过上限保留至下轮。
                for (int i = 0; i < 100 && !due.isAfter(today); i++) {
                    SubscriptionChargeService.ChargeResult result = subscriptionChargeService.charge(sub.getId(), sub.getCreateBy(), due, today);
                    if (result == null) break;
                    if (result.created()) count++;
                    due = result.nextDate();
                }
            } catch (Exception e) {
                chargeExecutions.failed(sub.getCreateBy(),"subscription",sub.getId(),due);
                log.error("订阅出账失败 id={}", sub.getId(), e);
            }
        }
        return count;
    }

    private void validateSchedule(PxSubscription sub) {
        if (sub.getNextPaymentDate() == null) throw new ServiceException("下次扣费日期不能为空");
        if (sub.getCycleInterval() != null && sub.getCycleInterval() < 1) throw new ServiceException("周期间隔必须大于 0");
        SubscriptionSchedule.validateCycle(sub.getCycle());
        if (sub.getAmount() == null || sub.getAmount().signum() < 0) throw new ServiceException("金额不能小于 0");
    }
}
