package com.pnkx.service.impl;

import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxWallpaper;
import com.pnkx.mapper.PxWallpaperMapper;
import com.pnkx.service.IPxWallpaperService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxWallpaperServiceImpl
 * @description 壁纸Service业务层处理
 */
@Service
public class PxWallpaperServiceImpl implements IPxWallpaperService {

    @Resource
    private PxWallpaperMapper pxWallpaperMapper;

    /**
     * 查询壁纸
     *
     * @param id 壁纸ID
     * @return 壁纸
     */
    @Override
    public PxWallpaper selectPxWallpaperById(Long id) {
        return pxWallpaperMapper.selectPxWallpaperById(id);
    }

    /**
     * 查询壁纸列表
     * 壁纸为公共资源，不做数据权限过滤，所有用户均可见
     *
     * @param pxWallpaper 壁纸
     * @return 壁纸集合
     */
    @Override
    public List<PxWallpaper> selectPxWallpaperList(PxWallpaper pxWallpaper) {
        return pxWallpaperMapper.selectPxWallpaperList(pxWallpaper);
    }


    /**
     * 根据 id 集合查询壁纸（用于批量下载）
     *
     * @param ids 壁纸ID集合
     * @return 壁纸集合
     */
    @Override
    public List<PxWallpaper> selectPxWallpaperByIds(Long[] ids) {
        return pxWallpaperMapper.selectPxWallpaperByIds(ids);
    }

    /**
     * 查询某文件夹子树下的全部壁纸（递归包含所有子文件夹）
     *
     * @param folderId 文件夹ID
     * @return 壁纸集合
     */
    @Override
    public List<PxWallpaper> selectPxWallpaperByFolderSubtree(Long folderId) {
        return pxWallpaperMapper.selectPxWallpaperByFolderSubtree(folderId);
    }

    /**
     * 点赞数自增/自减
     *
     * @param id    壁纸ID
     * @param delta 增量（+1 / -1）
     */
    @Override
    public void updateLikeCount(Long id, int delta) {
        pxWallpaperMapper.updateLikeCount(id, delta);
    }

    /**
     * 新增壁纸
     *
     * @param pxWallpaper 壁纸
     * @return 结果
     */
    @Override
    public PxWallpaper insertPxWallpaper(PxWallpaper pxWallpaper) {
        pxWallpaper.setCreateTime(DateUtils.getNowDate());
        pxWallpaper.setCreateBy(SecurityUtils.getUserId());
        pxWallpaper.setUpdateTime(DateUtils.getNowDate());
        pxWallpaper.setUpdateBy(SecurityUtils.getUserId());
        pxWallpaperMapper.insertPxWallpaper(pxWallpaper);
        return pxWallpaper;
    }

    /**
     * 修改壁纸
     *
     * @param pxWallpaper 壁纸
     * @return 结果
     */
    @Override
    public PxWallpaper updatePxWallpaper(PxWallpaper pxWallpaper) {
        pxWallpaper.setUpdateTime(DateUtils.getNowDate());
        pxWallpaper.setUpdateBy(SecurityUtils.getUserId());
        pxWallpaperMapper.updatePxWallpaper(pxWallpaper);
        return pxWallpaper;
    }

    /**
     * 批量删除壁纸
     *
     * @param ids 需要删除的壁纸ID
     * @return 结果
     */
    @Override
    public int deletePxWallpaperByIds(Long[] ids) {
        return pxWallpaperMapper.deletePxWallpaperByIds(ids);
    }

    /**
     * 删除壁纸信息
     *
     * @param id 壁纸ID
     * @return 结果
     */
    @Override
    public int deletePxWallpaperById(Long id) {
        return pxWallpaperMapper.deletePxWallpaperById(id);
    }
}
