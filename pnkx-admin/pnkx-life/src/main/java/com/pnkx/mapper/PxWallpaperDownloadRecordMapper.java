package com.pnkx.mapper;

import java.util.List;

import com.pnkx.domain.po.PxWallpaperDownloadRecord;

/**
 * 壁纸下载记录 Mapper接口
 *
 * @author pnkx
 */
public interface PxWallpaperDownloadRecordMapper {
    /**
     * 我的下载记录列表（按 create_by 过滤，最新优先）
     *
     * @param record createBy
     * @return 下载记录集合
     */
    public List<PxWallpaperDownloadRecord> selectMyDownloadList(PxWallpaperDownloadRecord record);

    /**
     * 管理端：下载记录中出现过的用户（用于用户筛选下拉）
     *
     * @return 用户集合（userId, nickName）
     */
    public List<java.util.Map<String, Object>> selectDownloadRecordUsers();

    /**
     * 批量新增下载记录
     *
     * @param records 下载记录列表
     * @return 影响行数
     */
    public int batchInsertDownloadRecord(List<PxWallpaperDownloadRecord> records);

    /**
     * 统计：按日期分组的下载数量
     */
    public List<java.util.Map<String, Object>> selectDownloadStatsByDate(java.util.Map<String, Object> params);

    /**
     * 统计：按文件夹分组的下载数量
     */
    public List<java.util.Map<String, Object>> selectDownloadStatsByFolder(java.util.Map<String, Object> params);

    /**
     * 统计：按用户分组的下载次数（用户汇总）
     */
    public List<java.util.Map<String, Object>> selectDownloadStatsByUser(java.util.Map<String, Object> params);

    /**
     * 统计：按用户+日期分组的下载次数明细
     */
    public List<java.util.Map<String, Object>> selectDownloadStatsByUserDate(java.util.Map<String, Object> params);

    /**
     * 统计某用户当日的下载次数
     *
     * @param userId 用户ID
     * @return 当日下载次数
     */
    public int countTodayDownload(String userId);
}
