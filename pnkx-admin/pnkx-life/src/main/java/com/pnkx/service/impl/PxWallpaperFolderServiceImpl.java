package com.pnkx.service.impl;

import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxWallpaperFolder;
import com.pnkx.mapper.PxWallpaperFolderMapper;
import com.pnkx.service.IPxWallpaperFolderService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxWallpaperFolderServiceImpl
 * @description 壁纸文件夹Service业务层处理
 */
@Service
public class PxWallpaperFolderServiceImpl implements IPxWallpaperFolderService {

    @Resource
    private PxWallpaperFolderMapper pxWallpaperFolderMapper;

    /**
     * 查询壁纸文件夹
     *
     * @param id 壁纸文件夹ID
     * @return 壁纸文件夹
     */
    @Override
    public PxWallpaperFolder selectPxWallpaperFolderById(Long id) {
        return pxWallpaperFolderMapper.selectPxWallpaperFolderById(id);
    }

    /**
     * 查询壁纸文件夹列表
     *
     * @param pxWallpaperFolder 壁纸文件夹
     * @return 壁纸文件夹集合
     */
    @Override
    public List<PxWallpaperFolder> selectPxWallpaperFolderList(PxWallpaperFolder pxWallpaperFolder) {
        return pxWallpaperFolderMapper.selectPxWallpaperFolderList(pxWallpaperFolder);
    }


    /**
     * 查询壁纸文件夹分页列表（轻量版，不含递归 CTE）
     * 壁纸文件夹为公共资源，不做数据权限过滤
     */
    @Override
    public List<PxWallpaperFolder> selectPxWallpaperFolderPage(PxWallpaperFolder pxWallpaperFolder) {
        return pxWallpaperFolderMapper.selectPxWallpaperFolderPage(pxWallpaperFolder);
    }


    /**
     * 新增壁纸文件夹
     *
     * @param pxWallpaperFolder 壁纸文件夹
     * @return 结果
     */
    @Override
    public PxWallpaperFolder insertPxWallpaperFolder(PxWallpaperFolder pxWallpaperFolder) {
        pxWallpaperFolder.setCreateTime(DateUtils.getNowDate());
        pxWallpaperFolder.setCreateBy(SecurityUtils.getUserId());
        pxWallpaperFolder.setUpdateTime(DateUtils.getNowDate());
        pxWallpaperFolder.setUpdateBy(SecurityUtils.getUserId());
        pxWallpaperFolderMapper.insertPxWallpaperFolder(pxWallpaperFolder);
        return pxWallpaperFolder;
    }

    /**
     * 修改壁纸文件夹
     *
     * @param pxWallpaperFolder 壁纸文件夹
     * @return 结果
     */
    @Override
    public PxWallpaperFolder updatePxWallpaperFolder(PxWallpaperFolder pxWallpaperFolder) {
        pxWallpaperFolder.setUpdateTime(DateUtils.getNowDate());
        pxWallpaperFolder.setUpdateBy(SecurityUtils.getUserId());
        pxWallpaperFolderMapper.updatePxWallpaperFolder(pxWallpaperFolder);
        return pxWallpaperFolder;
    }

    /**
     * 批量删除壁纸文件夹
     *
     * @param ids 需要删除的壁纸文件夹ID
     * @return 结果
     */
    @Override
    public int deletePxWallpaperFolderByIds(Long[] ids) {
        return pxWallpaperFolderMapper.deletePxWallpaperFolderByIds(ids);
    }

    /**
     * 删除壁纸文件夹信息
     *
     * @param id 壁纸文件夹ID
     * @return 结果
     */
    @Override
    public int deletePxWallpaperFolderById(Long id) {
        return pxWallpaperFolderMapper.deletePxWallpaperFolderById(id);
    }

    /**
     * 查询所有「未启用」文件夹及其全部子文件夹 id 集合（递归）。
     *
     * @return 停用子树文件夹 id 集合
     */
    @Override
    public List<Long> selectDisabledFolderSubtreeIds() {
        return pxWallpaperFolderMapper.selectDisabledFolderSubtreeIds();
    }
}
