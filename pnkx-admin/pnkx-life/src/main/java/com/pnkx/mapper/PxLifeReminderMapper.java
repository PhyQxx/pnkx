package com.pnkx.mapper;

import com.pnkx.domain.po.PxLifeReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author PHY
 * @classname PxLifeReminderMapper
 * @date 2026/07/02
 * @description 统一提醒配置 Mapper
 */
@Mapper
public interface PxLifeReminderMapper {

    /**
     * 查询提醒配置
     *
     * @param id 主键
     * @return 提醒配置
     */
    PxLifeReminder selectPxLifeReminderById(Long id);

    /**
     * 查询提醒配置列表
     *
     * @param pxLifeReminder 提醒配置
     * @return 提醒配置集合
     */
    List<PxLifeReminder> selectPxLifeReminderList(PxLifeReminder pxLifeReminder);

    /**
     * 新增提醒配置
     *
     * @param pxLifeReminder 提醒配置
     * @return 结果
     */
    int insertPxLifeReminder(PxLifeReminder pxLifeReminder);

    /**
     * 修改提醒配置
     *
     * @param pxLifeReminder 提醒配置
     * @return 结果
     */
    int updatePxLifeReminder(PxLifeReminder pxLifeReminder);

    /**
     * 删除提醒配置（软删除）
     *
     * @param id 主键
     * @return 结果
     */
    int deletePxLifeReminderById(Long id);

    /**
     * 批量删除提醒配置（软删除）
     *
     * @param ids 主键数组
     * @return 结果
     */
    int deletePxLifeReminderByIds(Long[] ids);

    /**
     * 根据来源类型与来源实体ID查询提醒配置（查重）
     *
     * @param sourceType 来源类型
     * @param sourceId   来源实体ID
     * @param userId     接收用户ID
     * @return 提醒配置
     */
    PxLifeReminder selectBySource(@Param("sourceType") String sourceType,
                                  @Param("sourceId") Long sourceId,
                                  @Param("userId") String userId);

    /**
     * 扫描到期需要触发的提醒
     * <p>
     * 条件：启用 + 未删除 + remind_time <= now + 来源事件未超过停止提醒天数
     *
     * @param now            当前时间
     * @param stopDaysAfter  来源事件过期停止提醒天数
     * @return 到期提醒集合
     */
    List<PxLifeReminder> selectDueReminders(@Param("now") Date now,
                                            @Param("stopDaysAfter") int stopDaysAfter);

    /**
     * 更新上次触发时间（防重发）
     *
     * @param id     提醒配置ID
     * @param time   触发时间
     * @return 结果
     */
    int updateLastTriggeredTime(@Param("id") Long id, @Param("time") Date time);

    /**
     * 根据客户端唯一标识查询（幂等去重）
     */
    PxLifeReminder selectByClientUuid(@Param("clientUuid") String clientUuid);

    /**
     * 增量查询（离线同步用）
     */
    List<PxLifeReminder> selectIncremental(@Param("createBy") String createBy,
                                           @Param("since") String since,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);
}
