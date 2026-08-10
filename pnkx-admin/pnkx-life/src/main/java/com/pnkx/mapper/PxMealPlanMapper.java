package com.pnkx.mapper;

import com.pnkx.domain.po.PxMealPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 餐饮计划 Mapper
 *
 * @author PHY
 * @date 2026/07/05
 */
@Mapper
public interface PxMealPlanMapper {
    PxMealPlan selectPxMealPlanById(Long id);

    List<PxMealPlan> selectPxMealPlanList(PxMealPlan pxMealPlan);

    /** 按日期范围查询（周视图用） */
    List<PxMealPlan> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    int insertPxMealPlan(PxMealPlan pxMealPlan);

    int updatePxMealPlan(PxMealPlan pxMealPlan);

    int deletePxMealPlanById(Long id);

    int deletePxMealPlanByIds(Long[] ids);
}
