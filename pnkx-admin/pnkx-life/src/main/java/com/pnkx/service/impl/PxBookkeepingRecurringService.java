package com.pnkx.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.domain.po.PxBookkeepingRecurring;
import com.pnkx.mapper.PxBookkeepingRecordMapper;
import com.pnkx.mapper.PxBookkeepingRecurringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 周期记账服务：规则 CRUD 与到期生成
 *
 * @author PHY
 * @date 2026-09-23
 */
@Service
public class PxBookkeepingRecurringService {

    private static final Logger log = LoggerFactory.getLogger(PxBookkeepingRecurringService.class);

    /**
     * 频率：每月
     */
    private static final String FREQ_MONTH = "month";

    /**
     * 频率：每周
     */
    private static final String FREQ_WEEK = "week";

    @Resource
    private PxBookkeepingRecurringMapper recurringMapper;

    @Resource
    private PxBookkeepingRecordMapper recordMapper;

    /**
     * 当前用户的规则列表
     */
    public List<PxBookkeepingRecurring> listMine() {
        return recurringMapper.selectRecurringList(SecurityUtils.getUserId());
    }

    /**
     * 新增规则（下次执行日自动计算：不早于明天）
     */
    public int add(PxBookkeepingRecurring recurring) {
        validate(recurring);
        recurring.setCreateBy(SecurityUtils.getUserId());
        recurring.setCreateTime(DateUtils.getNowDate());
        recurring.setEnabled(true);
        LocalDate next = computeNextRunDate(recurring, LocalDate.now());
        // 首次执行不早于明天，避免当天重复
        if (next.isBefore(LocalDate.now().plusDays(1))) {
            next = computeNextRunDate(recurring, next);
        }
        recurring.setNextRunDate(toDate(next));
        return recurringMapper.insertRecurring(recurring);
    }

    /**
     * 修改规则（重算下次执行日）
     */
    public int update(PxBookkeepingRecurring recurring) {
        if (recurring.getId() == null) {
            throw new ServiceException("规则ID不能为空");
        }
        validate(recurring);
        recurring.setUpdateBy(SecurityUtils.getUserId());
        return recurringMapper.updateRecurring(recurring);
    }

    /**
     * 启停规则。重新启用时不重置 next_run_date：
     * 停用期间到期的账（如房租）会在下次任务执行时补记一笔，符合固定支出的实际语义
     */
    public int toggle(Long id, boolean enabled) {
        PxBookkeepingRecurring rule = new PxBookkeepingRecurring();
        rule.setId(id);
        rule.setEnabled(enabled);
        return recurringMapper.updateRecurring(rule);
    }

    /**
     * 删除规则
     */
    public int delete(Long id) {
        return recurringMapper.deleteRecurringById(id);
    }

    /**
     * 执行到期的周期规则：生成记账记录并推进下次执行日。
     * 由定时任务调用（无登录上下文，记录归属取规则的 create_by）。
     *
     * @return 本次生成的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int executeDue() {
        Date today = DateUtils.getNowDate();
        List<PxBookkeepingRecurring> dueList = recurringMapper.selectDueList(today);
        if (dueList.isEmpty()) {
            return 0;
        }
        LocalDate todayDate = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int generated = 0;
        for (PxBookkeepingRecurring rule : dueList) {
            try {
                PxBookkeepingRecord record = buildRecord(rule);
                recordMapper.insertPxBookkeepingRecord(record);
                LocalDate next = computeNextRunDate(rule, todayDate);
                recurringMapper.advanceRunDate(rule.getId(), toDate(next), today);
                generated++;
            } catch (Exception e) {
                // 单条规则失败不影响其余规则
                log.error("周期记账规则执行失败: {} ({})", rule.getName(), rule.getId(), e);
            }
        }
        log.info("周期记账执行完成：{} 条规则到期，生成 {} 条记录", dueList.size(), generated);
        return generated;
    }

    /**
     * 由规则构造一条记账记录（payTime 取执行日，remark 追加周期标记）
     */
    private PxBookkeepingRecord buildRecord(PxBookkeepingRecurring rule) {
        PxBookkeepingRecord record = new PxBookkeepingRecord();
        // PxBookkeepingRecord 无独立收支字段：type=0 即转账语义，收支由分类的 type_difference 表达
        record.setType("2".equals(rule.getTypeDifference()) ? 0L : rule.getType());
        record.setAccount(rule.getAccount());
        record.setOtherAccount(rule.getOtherAccount());
        record.setMoney(rule.getMoney().toPlainString());
        record.setCreateBy(rule.getCreateBy());
        record.setCreateTime(DateUtils.getNowDate());
        record.setPayTime(DateUtils.getNowDate());
        record.setRemark(StringUtils.isEmpty(rule.getRemark())
                ? String.format("【周期·%s】%s", FREQ_MONTH.equals(rule.getFrequency()) ? "每月" : "每周", rule.getName())
                : rule.getRemark() + String.format("（周期·%s）", FREQ_MONTH.equals(rule.getFrequency()) ? "每月" : "每周"));
        return record;
    }

    /**
     * 计算基准日之后（不含基准日）的下一个执行日
     */
    private LocalDate computeNextRunDate(PxBookkeepingRecurring rule, LocalDate base) {
        if (FREQ_WEEK.equals(rule.getFrequency())) {
            int diff = rule.getDayNumber() - base.getDayOfWeek().getValue();
            if (diff <= 0) {
                diff += 7;
            }
            return base.plusDays(diff);
        }
        // month：下一个月的 dayNumber（dayNumber 已限 1-28，无月末溢出问题）
        LocalDate nextMonth = base.plusMonths(1).withDayOfMonth(rule.getDayNumber());
        return nextMonth;
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private void validate(PxBookkeepingRecurring rule) {
        if (StringUtils.isEmpty(rule.getName())) {
            throw new ServiceException("规则名称不能为空");
        }
        if (!FREQ_MONTH.equals(rule.getFrequency()) && !FREQ_WEEK.equals(rule.getFrequency())) {
            throw new ServiceException("频率仅支持 month（每月）/ week（每周）");
        }
        int day = rule.getDayNumber() == null ? 0 : rule.getDayNumber();
        if (FREQ_MONTH.equals(rule.getFrequency()) && (day < 1 || day > 28)) {
            throw new ServiceException("每月执行日仅支持 1-28 号");
        }
        if (FREQ_WEEK.equals(rule.getFrequency()) && (day < 1 || day > 7)) {
            throw new ServiceException("每周执行日为 1-7（周一=1）");
        }
        if (rule.getMoney() == null || rule.getMoney().signum() <= 0) {
            throw new ServiceException("金额必须大于 0");
        }
    }
}
