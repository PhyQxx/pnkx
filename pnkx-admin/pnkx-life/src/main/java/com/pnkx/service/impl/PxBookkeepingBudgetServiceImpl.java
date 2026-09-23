package com.pnkx.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxBookkeepingBudget;
import com.pnkx.mapper.PxBookkeepingBudgetMapper;
import com.pnkx.service.IPxBookkeepingBudgetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

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

    @Override
    public List<PxBookkeepingBudget> listBudgets(String month) {
        PxBookkeepingBudget query = new PxBookkeepingBudget();
        query.setMonth(requireMonth(month));
        query.setCreateBy(SecurityUtils.getUserId());
        return budgetMapper.selectBudgetList(query);
    }

    @Override
    public List<PxBookkeepingBudget> getBudgetStatus(String month) {
        PxBookkeepingBudget query = new PxBookkeepingBudget();
        query.setMonth(requireMonth(month));
        query.setCreateBy(SecurityUtils.getUserId());
        List<PxBookkeepingBudget> list = budgetMapper.selectBudgetStatus(query);
        list.forEach(b -> b.setExceeded(b.getUsed() != null
                && b.getUsed().compareTo(b.getAmount()) > 0));
        return list;
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
        budget.setCreateBy(SecurityUtils.getUserId());
        PxBookkeepingBudget existed = budgetMapper.selectByMonthAndType(budget);
        if (existed != null) {
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
        return budgetMapper.deleteBudgetById(id);
    }

    private String requireMonth(String month) {
        if (StringUtils.isEmpty(month) || !month.matches("\\d{4}-\\d{2}")) {
            throw new ServiceException("月份格式应为 yyyy-MM");
        }
        return month;
    }
}
