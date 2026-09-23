package com.pnkx.mapper;

import com.pnkx.domain.po.PxBookkeepingBudget;

import java.util.List;

/**
 * 记账预算 Mapper
 *
 * @author PHY
 * @date 2026-09-23
 */
public interface PxBookkeepingBudgetMapper {

    /**
     * 查询某用户某月的全部预算（联分类名）
     *
     * @param budget 查询条件（month + createBy 必填）
     * @return 预算列表
     */
    List<PxBookkeepingBudget> selectBudgetList(PxBookkeepingBudget budget);

    /**
     * 查询某用户某月的预算及实时使用状态（已用/剩余/百分比/是否超支）
     *
     * @param budget 查询条件（month + createBy 必填）
     * @return 带状态的预算列表
     */
    List<PxBookkeepingBudget> selectBudgetStatus(PxBookkeepingBudget budget);

    /**
     * 按月 + 分类 + 用户精确查询（保存前判重）
     *
     * @param budget 查询条件
     * @return 预算
     */
    PxBookkeepingBudget selectByMonthAndType(PxBookkeepingBudget budget);

    /**
     * 新增预算
     *
     * @param budget 预算
     * @return 结果
     */
    int insertBudget(PxBookkeepingBudget budget);

    /**
     * 修改预算金额
     *
     * @param budget 预算
     * @return 结果
     */
    int updateBudget(PxBookkeepingBudget budget);

    /**
     * 删除预算（物理删除，版本少无需软删）
     *
     * @param id 预算ID
     * @return 结果
     */
    int deleteBudgetById(Long id);
}
