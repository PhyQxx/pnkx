package com.pnkx.mapper;

import com.pnkx.domain.po.PxWallpaperShareRewardRecord;

import java.util.Map;

/**
 * 壁纸分享小程序奖励下载次数记录 Mapper接口
 *
 * @author pnkx
 */
public interface PxWallpaperShareRewardRecordMapper {

    /**
     * 统计某用户当日的分享奖励情况
     *
     * @param userId 用户ID
     * @return Map：times=今日已分享获奖次数；total=今日奖励的下载次数总和
     */
    public Map<String, Object> getTodayShareReward(String userId);

    /**
     * 新增一条分享奖励记录
     *
     * @param record 奖励记录
     * @return 影响行数
     */
    public int insertShareReward(PxWallpaperShareRewardRecord record);
}
