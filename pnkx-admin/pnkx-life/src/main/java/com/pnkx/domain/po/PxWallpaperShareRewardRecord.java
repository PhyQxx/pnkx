package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 壁纸分享小程序奖励下载次数记录对象 px_wallpaper_share_reward_record
 * <p>
 * 与真实下载记录(px_wallpaper_download_record)隔离，避免污染每日下载次数
 * 统计(countTodayDownload)和下载统计报表。
 *
 * @author pnkx
 */
@Data
public class PxWallpaperShareRewardRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 本次奖励的下载次数
     */
    private Integer rewardCount;
}
