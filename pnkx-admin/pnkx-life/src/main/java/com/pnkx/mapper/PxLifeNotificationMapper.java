package com.pnkx.mapper;

import com.pnkx.domain.po.PxLifeNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author PHY
 * @classname PxLifeNotificationMapper
 * @date 2026/07/02
 * @description 提醒投递日志 Mapper
 */
@Mapper
public interface PxLifeNotificationMapper {

    /**
     * 新增投递日志
     *
     * @param notification 投递日志
     * @return 结果
     */
    int insertPxLifeNotification(PxLifeNotification notification);

    /**
     * 查询用户的投递日志（用于通知中心展示）
     *
     * @param userId 用户ID
     * @param status 状态（可空）
     * @return 投递日志集合
     */
    List<PxLifeNotification> selectByUser(@Param("userId") String userId,
                                          @Param("status") String status);

    /**
     * 统计用户未读通知数
     *
     * @param userId 用户ID
     * @return 未读数
     */
    int countUnread(@Param("userId") String userId);

    /**
     * 标记已读
     *
     * @param userId 用户ID
     * @param ids    通知ID数组（为空则全部标记已读）
     * @return 结果
     */
    int markRead(@Param("userId") String userId, @Param("ids") Long[] ids);

    int deleteByUser(@Param("userId") String userId, @Param("id") Long id);
}
