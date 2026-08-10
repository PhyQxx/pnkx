package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.domain.po.PxSubscription;
import com.pnkx.mapper.PxBookkeepingRecordMapper;
import com.pnkx.mapper.PxSubscriptionMapper;
import com.pnkx.service.IPxSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 订阅管理 Service 实现
 *
 * @author PHY
 * @date 2026/07/05
 */
@Service
public class PxSubscriptionServiceImpl implements IPxSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(PxSubscriptionServiceImpl.class);

    @Resource
    private PxSubscriptionMapper pxSubscriptionMapper;
    @Resource
    private PxBookkeepingRecordMapper pxBookkeepingRecordMapper;

    @Override
    public PxSubscription selectPxSubscriptionById(Long id) {
        return pxSubscriptionMapper.selectPxSubscriptionById(id);
    }

    @DataScopeSelf
    @Override
    public List<PxSubscription> selectPxSubscriptionList(PxSubscription pxSubscription) {
        return pxSubscriptionMapper.selectPxSubscriptionList(pxSubscription);
    }


    @Override
    public int insertPxSubscription(PxSubscription pxSubscription) {
        pxSubscription.setCreateTime(DateUtils.getNowDate());
        return pxSubscriptionMapper.insertPxSubscription(pxSubscription);
    }

    @Override
    public int updatePxSubscription(PxSubscription pxSubscription) {
        pxSubscription.setUpdateTime(DateUtils.getNowDate());
        return pxSubscriptionMapper.updatePxSubscription(pxSubscription);
    }

    @Override
    public int deletePxSubscriptionByIds(Long[] ids) {
        return pxSubscriptionMapper.deletePxSubscriptionByIds(ids);
    }

    @Override
    public int deletePxSubscriptionById(Long id) {
        return pxSubscriptionMapper.deletePxSubscriptionById(id);
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
     * 3. 防止极端情况：最多推进 12 次（跳过长期未处理的积压）
     */
    @Override
    public int generateDueRecords() {
        Date now = DateUtils.getNowDate();
        List<PxSubscription> dueList = pxSubscriptionMapper.selectDueSubscriptions(now);
        if (dueList.isEmpty()) {
            log.info("【订阅出账】无到期订阅");
            return 0;
        }
        int count = 0;
        for (PxSubscription sub : dueList) {
            try {
                LocalDate paymentDate = toLocalDate(sub.getNextPaymentDate());
                LocalDate today = LocalDate.now();
                int safeguard = 0;
                // 处理可能积压的多个周期（但限制 12 次防死循环）
                while (!paymentDate.isAfter(today) && safeguard < 12) {
                    generateOneRecord(sub, paymentDate);
                    paymentDate = advanceDate(paymentDate, sub.getCycle(), sub.getCycleInterval());
                    safeguard++;
                }
                // 更新下次扣费日期
                PxSubscription update = new PxSubscription();
                update.setId(sub.getId());
                update.setNextPaymentDate(java.sql.Date.valueOf(paymentDate));
                update.setUpdateTime(now);
                pxSubscriptionMapper.updatePxSubscription(update);
                count++;
                log.info("【订阅出账】{} 已出账，下次扣费推进至 {}", sub.getName(), paymentDate);
            } catch (Exception e) {
                log.error("【订阅出账】处理订阅失败 id={} name={}", sub.getId(), sub.getName(), e);
            }
        }
        log.info("【订阅出账】完成，共处理 {} 个订阅", count);
        return count;
    }

    /**
     * 生成单条记账记录
     */
    private void generateOneRecord(PxSubscription sub, LocalDate paymentDate) {
        PxBookkeepingRecord record = new PxBookkeepingRecord();
        record.setAccount(sub.getAccountId());
        record.setType(sub.getClassificationId());
        record.setMoney(sub.getAmount().toPlainString());
        record.setPayTime(java.sql.Date.valueOf(paymentDate));
        record.setDelFlag(false);
        record.setVersion("subscription_" + sub.getId() + "_" + paymentDate.toString());
        record.setClientUuid("sub_" + sub.getId() + "_" + paymentDate.toString());
        record.setCreateBy(sub.getCreateBy());
        record.setCreateTime(DateUtils.getNowDate());
        record.setRemark("订阅自动出账：" + sub.getName());
        pxBookkeepingRecordMapper.insertPxBookkeepingRecord(record);
    }

    /**
     * 按周期推进日期
     */
    private LocalDate advanceDate(LocalDate date, String cycle, Integer interval) {
        int n = interval == null ? 1 : interval;
        if (cycle == null) cycle = "monthly";
        switch (cycle) {
            case "daily":
                return date.plus(n, ChronoUnit.DAYS);
            case "weekly":
                return date.plus(n * 7L, ChronoUnit.DAYS);
            case "yearly":
                return date.plus(n, ChronoUnit.YEARS);
            case "monthly":
            default:
                return date.plus(n, ChronoUnit.MONTHS);
        }
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
