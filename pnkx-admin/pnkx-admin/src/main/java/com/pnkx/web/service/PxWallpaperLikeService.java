package com.pnkx.web.service;

import com.pnkx.common.utils.DateUtils;
import com.pnkx.blog.domain.po.PxLikeRecord;
import com.pnkx.life.domain.po.PxWallpaper;
import com.pnkx.blog.mapper.PxLikeRecordMapper;
import com.pnkx.life.service.IPxWallpaperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 壁纸点赞服务
 * <p>
 * 点赞记录(px_like_record, pnkx-blog)与壁纸计数(px_wallpaper, pnkx-life)分属两个业务模块，
 * 事务编排放在依赖两者的 web 层完成；同一数据源，本地事务即可保证两步写一致。
 * 幂等由数据库唯一索引 uk_like_user_item(item_id, type, create_by) 兜底（V1.4.5）。
 *
 * @author phy
 */
@Service
public class PxWallpaperLikeService {

    private static final Logger log = LoggerFactory.getLogger(PxWallpaperLikeService.class);

    /**
     * 点赞记录类型：壁纸点赞
     */
    public static final String WALLPAPER_LIKE_TYPE = "3";

    @Resource
    private PxLikeRecordMapper pxLikeRecordMapper;

    @Resource
    private IPxWallpaperService pxWallpaperService;

    /**
     * 切换壁纸点赞态（已赞取消 / 未赞点赞）
     *
     * @param wallpaperId 壁纸ID
     * @param userId      当前用户
     * @return 切换后的点赞态：true 已赞，false 已取消
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long wallpaperId, String userId) {
        PxLikeRecord param = new PxLikeRecord();
        param.setItemId(wallpaperId);
        param.setType(WALLPAPER_LIKE_TYPE);
        param.setCreateBy(userId);
        PxLikeRecord existed = pxLikeRecordMapper.selectLikeByUser(param);
        if (existed != null) {
            pxLikeRecordMapper.deleteLikeByUser(param);
            pxWallpaperService.updateLikeCount(wallpaperId, -1);
            return false;
        }
        PxWallpaper wp = pxWallpaperService.selectPxWallpaperById(wallpaperId);
        param.setCreateTime(DateUtils.getNowDate());
        if (wp != null) {
            param.setItemName(wp.getName());
            param.setItemThumbnail(wp.getThumbnail());
        }
        try {
            pxLikeRecordMapper.insertPxLikeRecord(param);
        } catch (DuplicateKeyException e) {
            // 并发重复点赞由唯一索引兜底：视为已赞，不再累加计数
            log.info("并发点赞被唯一索引拦截，壁纸ID：{}，用户：{}", wallpaperId, userId);
            return true;
        }
        pxWallpaperService.updateLikeCount(wallpaperId, 1);
        return true;
    }

    /**
     * 指定用户是否已点赞指定壁纸
     */
    public boolean isLiked(Long wallpaperId, String userId) {
        PxLikeRecord param = new PxLikeRecord();
        param.setItemId(wallpaperId);
        param.setType(WALLPAPER_LIKE_TYPE);
        param.setCreateBy(userId);
        return pxLikeRecordMapper.selectLikeByUser(param) != null;
    }

    /**
     * 我的壁纸点赞记录（分页数据源）
     */
    public List<PxLikeRecord> selectMyLikes(String userId) {
        PxLikeRecord param = new PxLikeRecord();
        param.setType(WALLPAPER_LIKE_TYPE);
        param.setCreateBy(userId);
        return pxLikeRecordMapper.selectMyRecordList(param);
    }

    /**
     * 管理端：分页查询壁纸点赞记录（JOIN 壁纸表取名称缩略图，可按用户筛选）
     */
    public List<PxLikeRecord> selectAllLikes(String createBy) {
        PxLikeRecord param = new PxLikeRecord();
        param.setType(WALLPAPER_LIKE_TYPE);
        param.setCreateBy(createBy);
        return pxLikeRecordMapper.selectRecordListWithWallpaper(param);
    }

    /**
     * 管理端：点赞记录中出现过的用户（用户筛选下拉数据源）
     */
    public List<Map<String, Object>> selectLikeRecordUsers() {
        PxLikeRecord param = new PxLikeRecord();
        param.setType(WALLPAPER_LIKE_TYPE);
        return pxLikeRecordMapper.selectRecordUsers(param);
    }

    /**
     * 管理端：点赞记录按日期趋势统计
     */
    public List<Map<String, Object>> selectLikeStatsByDate(String beginTime, String endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("likeType", WALLPAPER_LIKE_TYPE);
        params.put("beginTime", beginTime);
        params.put("endTime", endTime);
        return pxLikeRecordMapper.selectRecordStatsByDate(params);
    }

    /**
     * 管理端：点赞记录按文件夹分布统计
     */
    public List<Map<String, Object>> selectLikeStatsByFolder() {
        Map<String, Object> params = new HashMap<>();
        params.put("likeType", WALLPAPER_LIKE_TYPE);
        return pxLikeRecordMapper.selectRecordStatsByFolder(params);
    }
}
