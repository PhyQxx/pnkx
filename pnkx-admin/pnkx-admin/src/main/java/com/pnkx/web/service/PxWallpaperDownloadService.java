package com.pnkx.web.service;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxWallpaper;
import com.pnkx.domain.po.PxWallpaperDownloadRecord;
import com.pnkx.domain.po.PxWallpaperShareRewardRecord;
import com.pnkx.mapper.PxWallpaperDownloadRecordMapper;
import com.pnkx.mapper.PxWallpaperShareRewardRecordMapper;
import com.pnkx.system.service.ISysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 壁纸下载配额服务
 * <p>
 * 从 PxClientWallpaperController 下沉的业务逻辑：
 * 每日下载限额（含分享奖励加成）、下载历史记录、分享奖励发放与防刷。
 * 配置键均存于 sys_config，读取失败或格式错误时回退默认值。
 *
 * @author phy
 */
@Service
public class PxWallpaperDownloadService {

    private static final Logger log = LoggerFactory.getLogger(PxWallpaperDownloadService.class);

    /**
     * 每用户每日下载上限配置键
     */
    public static final String DAILY_LIMIT_CONFIG_KEY = "sys.wallpaper.download.daily.limit";
    private static final int DEFAULT_DAILY_LIMIT = 50;

    /**
     * 每次分享奖励的下载次数配置键
     */
    public static final String SHARE_REWARD_CONFIG_KEY = "sys.wallpaper.download.share.reward";
    private static final int DEFAULT_SHARE_REWARD = 10;

    /**
     * 每日最多可分享获奖次数配置键（防刷，0 表示不限）
     */
    public static final String SHARE_DAILY_TIMES_CONFIG_KEY = "sys.wallpaper.download.share.daily.times";
    private static final int DEFAULT_SHARE_DAILY_TIMES = 3;

    /**
     * 下载达上限提醒文案配置键
     */
    public static final String DOWNLOAD_REMIND_CONFIG_KEY = "sys.wallpaper.download.remind.text";
    private static final String DEFAULT_DOWNLOAD_REMIND_TEXT =
            "今日下载次数已用完，分享小程序可获得更多下载次数哦～";

    @Resource
    private PxWallpaperDownloadRecordMapper pxWallpaperDownloadRecordMapper;

    @Resource
    private PxWallpaperShareRewardRecordMapper pxWallpaperShareRewardRecordMapper;

    @Resource
    private ISysConfigService sysConfigService;

    /**
     * 分享奖励发放结果
     */
    public enum ShareRewardResult {
        /** 发放成功 */
        SUCCESS,
        /** 今日分享获奖次数已用完 */
        LIMIT_REACHED,
        /** 写入失败 */
        FAILED
    }

    /**
     * 从系统参数获取每日下载上限，未配置或格式错误时取默认值
     */
    public int getDailyLimit() {
        return getIntConfig(DAILY_LIMIT_CONFIG_KEY, DEFAULT_DAILY_LIMIT);
    }

    /**
     * 从系统参数获取每次分享奖励的下载次数
     */
    public int getShareReward() {
        return getIntConfig(SHARE_REWARD_CONFIG_KEY, DEFAULT_SHARE_REWARD);
    }

    /**
     * 从系统参数获取每日最多可分享获奖次数（防刷，0 表示不限）
     */
    public int getShareDailyTimes() {
        return getIntConfig(SHARE_DAILY_TIMES_CONFIG_KEY, DEFAULT_SHARE_DAILY_TIMES);
    }

    /**
     * 获取下载达上限提醒文案，未配置或为空时取默认文案
     */
    public String getDownloadRemindText() {
        String text = sysConfigService.selectConfigByKey(DOWNLOAD_REMIND_CONFIG_KEY);
        return StringUtils.isNotEmpty(text) ? text : DEFAULT_DOWNLOAD_REMIND_TEXT;
    }

    /**
     * 登录用户今日下载是否已达上限（每日限额 + 分享奖励加成）。
     * 未登录（游客）不限额。
     */
    public boolean isDownloadLimitReached(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return false;
        }
        int todayCount = pxWallpaperDownloadRecordMapper.countTodayDownload(userId);
        return todayCount >= getDailyLimit() + getTodayRewardTotal(userId);
    }

    /**
     * 当前剩余下载次数（下限 0）
     */
    public int getRemainingQuota(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return 0;
        }
        int todayCount = pxWallpaperDownloadRecordMapper.countTodayDownload(userId);
        return Math.max(0, getDailyLimit() + getTodayRewardTotal(userId) - todayCount);
    }

    /**
     * 填充今日下载状态（供分享奖励等接口统一返回最新配额）
     */
    public void fillDownloadStatus(AjaxResult ajax, String userId, int reward) {
        int limit = getDailyLimit();
        int todayCount = StringUtils.isEmpty(userId) ? 0
                : pxWallpaperDownloadRecordMapper.countTodayDownload(userId);
        int todayReward = getTodayRewardTotal(userId);
        ajax.put("limit", limit);
        ajax.put("todayCount", todayCount);
        ajax.put("todayReward", todayReward);
        ajax.put("remaining", Math.max(0, limit + todayReward - todayCount));
        ajax.put("reward", reward);
        ajax.put("shareTimesUsed", getTodayShareTimes(userId));
        ajax.put("shareTimesLimit", getShareDailyTimes());
    }

    /**
     * 发放分享奖励：防刷校验 + 写入奖励记录
     *
     * @return 发放结果（SUCCESS 时本次奖励次数见 {@link #getShareReward()}）
     */
    public ShareRewardResult grantShareReward(String userId) {
        int shareTimesLimit = getShareDailyTimes();
        if (shareTimesLimit > 0 && getTodayShareTimes(userId) >= shareTimesLimit) {
            return ShareRewardResult.LIMIT_REACHED;
        }
        try {
            PxWallpaperShareRewardRecord record = new PxWallpaperShareRewardRecord();
            record.setRewardCount(getShareReward());
            record.setCreateBy(userId);
            record.setCreateTime(DateUtils.getNowDate());
            pxWallpaperShareRewardRecordMapper.insertShareReward(record);
            return ShareRewardResult.SUCCESS;
        } catch (Exception e) {
            log.error("记录分享奖励失败, userId={}", userId, e);
            return ShareRewardResult.FAILED;
        }
    }

    /**
     * 记录单条下载（未登录则跳过，不阻塞下载流程）
     */
    public void recordDownload(PxWallpaper wallpaper, String downloadType) {
        recordDownloadBatch(wallpaper == null ? null : Collections.singletonList(wallpaper), downloadType);
    }

    /**
     * 批量记录下载（未登录则跳过，不阻塞下载流程）。返回实际记录的张数。
     */
    public int recordDownloadBatch(List<PxWallpaper> wallpapers, String downloadType) {
        try {
            String userId = com.pnkx.common.utils.SecurityUtils.getUserId();
            // 下载接口匿名可访问，未登录时 getUserId() 返回空串，跳过记录避免写入空 create_by
            if (StringUtils.isEmpty(userId) || wallpapers == null || wallpapers.isEmpty()) {
                return 0;
            }
            java.util.Date now = DateUtils.getNowDate();
            List<PxWallpaperDownloadRecord> records = new ArrayList<>();
            for (PxWallpaper wp : wallpapers) {
                if (wp == null || wp.getUrl() == null || wp.getUrl().trim().isEmpty()) {
                    continue;
                }
                PxWallpaperDownloadRecord record = new PxWallpaperDownloadRecord();
                record.setItemId(wp.getId());
                record.setItemName(wp.getName());
                record.setItemThumbnail(wp.getThumbnail());
                record.setDownloadType(downloadType);
                record.setCreateBy(userId);
                record.setCreateTime(now);
                records.add(record);
            }
            if (!records.isEmpty()) {
                pxWallpaperDownloadRecordMapper.batchInsertDownloadRecord(records);
            }
            return records.size();
        } catch (Exception e) {
            log.warn("批量记录下载历史失败", e);
            return 0;
        }
    }

    /**
     * 查询某用户今日已通过分享获得的下载次数总和，未登录或异常返回 0
     */
    private int getTodayRewardTotal(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return 0;
        }
        try {
            Map<String, Object> map = pxWallpaperShareRewardRecordMapper.getTodayShareReward(userId);
            return numberFrom(map, "total");
        } catch (Exception e) {
            log.warn("查询今日分享奖励总和失败, userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 查询某用户今日已分享获奖的次数，未登录或异常返回 0
     */
    private int getTodayShareTimes(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return 0;
        }
        try {
            Map<String, Object> map = pxWallpaperShareRewardRecordMapper.getTodayShareReward(userId);
            return numberFrom(map, "times");
        } catch (Exception e) {
            log.warn("查询今日分享次数失败, userId={}", userId, e);
            return 0;
        }
    }

    private int numberFrom(Map<String, Object> map, String key) {
        if (map == null) {
            return 0;
        }
        Object value = map.get(key);
        return value == null ? 0 : ((Number) value).intValue();
    }

    private int getIntConfig(String configKey, int defaultValue) {
        String config = sysConfigService.selectConfigByKey(configKey);
        if (StringUtils.isNotEmpty(config)) {
            try {
                return Integer.parseInt(config);
            } catch (NumberFormatException e) {
                log.warn("配置 {} 格式错误: {}，回退默认值 {}", configKey, config, defaultValue);
            }
        }
        return defaultValue;
    }
}
