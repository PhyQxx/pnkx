package com.pnkx.web.service;

import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.PxLikeRecord;
import com.pnkx.domain.po.PxWallpaper;
import com.pnkx.mapper.PxLikeRecordMapper;
import com.pnkx.service.IPxWallpaperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

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
}
