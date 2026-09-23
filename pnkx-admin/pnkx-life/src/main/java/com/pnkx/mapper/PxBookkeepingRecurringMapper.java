package com.pnkx.mapper;

import com.pnkx.domain.po.PxBookkeepingRecurring;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 周期记账规则 Mapper
 *
 * @author PHY
 * @date 2026-09-23
 */
public interface PxBookkeepingRecurringMapper {

    /**
     * 查询某用户的全部规则（联分类/账户名）
     *
     * @param createBy 用户ID
     * @return 规则列表
     */
    List<PxBookkeepingRecurring> selectRecurringList(@Param("createBy") String createBy);

    /**
     * 查询到期待执行的规则（enabled=1 且 next_run_date <= date）
     *
     * @param date 截止日期（含）
     * @return 规则列表
     */
    List<PxBookkeepingRecurring> selectDueList(@Param("date") Date date);

    /**
     * 新增规则
     */
    int insertRecurring(PxBookkeepingRecurring recurring);

    /**
     * 修改规则
     */
    int updateRecurring(PxBookkeepingRecurring recurring);

    /**
     * 执行后推进：更新下次执行日/上次执行日
     */
    int advanceRunDate(@Param("id") Long id,
                       @Param("nextRunDate") Date nextRunDate,
                       @Param("lastRunDate") Date lastRunDate);

    /**
     * 删除规则（物理删除）
     */
    int deleteRecurringById(@Param("id") Long id);
}
