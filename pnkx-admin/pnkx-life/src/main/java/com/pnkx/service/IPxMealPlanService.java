package com.pnkx.service;

import com.pnkx.domain.po.PxMealPlan;

import java.util.List;

/**
 * @author PHY
 * @classname IPxMealPlanService
 * @data 2026/07/05
 * @description 餐饮计划Service接口
 */
public interface IPxMealPlanService {
    /**
     * 查询餐饮计划
     *
     * @param id 餐饮计划ID
     * @return 餐饮计划
     */
    public PxMealPlan selectPxMealPlanById(Long id);

    /**
     * 查询餐饮计划列表
     *
     * @param pxMealPlan 餐饮计划
     * @return 餐饮计划集合
     */
    public List<PxMealPlan> selectPxMealPlanList(PxMealPlan pxMealPlan);

    /**
     * 新增餐饮计划
     *
     * @param pxMealPlan 餐饮计划
     * @return 结果
     */
    public int insertPxMealPlan(PxMealPlan pxMealPlan);

    /**
     * 修改餐饮计划
     *
     * @param pxMealPlan 餐饮计划
     * @return 结果
     */
    public int updatePxMealPlan(PxMealPlan pxMealPlan);

    /**
     * 批量删除餐饮计划
     *
     * @param ids 需要删除的餐饮计划ID
     * @return 结果
     */
    public int deletePxMealPlanByIds(Long[] ids);

    /**
     * 删除餐饮计划信息
     *
     * @param id 餐饮计划ID
     * @return 结果
     */
    public int deletePxMealPlanById(Long id);

    /**
     * 按日期范围查询餐饮计划（周视图）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 餐饮计划集合
     */
    public List<PxMealPlan> selectByDateRange(String startDate, String endDate);

    /**
     * 把日期范围内餐饮计划关联菜谱的食材，批量写入指定购物清单
     *
     * @param listId    购物清单ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 结果
     */
    public int transferToShopping(Long listId, String startDate, String endDate);
}
