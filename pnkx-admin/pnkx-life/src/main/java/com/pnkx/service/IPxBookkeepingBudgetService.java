package com.pnkx.service;

import com.pnkx.domain.po.PxBookkeepingBudget;

import java.util.List;

/**
 * 记账预算服务
 *
 * @author PHY
 * @date 2026-09-23
 */
public interface IPxBookkeepingBudgetService {

    /**
     * 查询某月预算配置列表
     *
     * @param month yyyy-MM
     * @return 预算列表
     */
    List<PxBookkeepingBudget> listBudgets(String month);

    /**
     * 查询某月预算使用状态（含已用/剩余/百分比/是否超支）
     *
     * @param month yyyy-MM
     * @return 带状态的预算列表
     */
    List<PxBookkeepingBudget> getBudgetStatus(String month);

    /**
     * 保存预算（同月同分类已存在则更新金额）
     *
     * @param budget 预算（month/typeId/amount 必填）
     * @return 结果
     */
    int saveBudget(PxBookkeepingBudget budget);

    /**
     * 删除预算
     *
     * @param id 预算ID
     * @return 结果
     */
    int deleteBudget(Long id);
}
