package com.pnkx.service;

import com.pnkx.domain.po.PxWallpaperFolder;

import java.util.List;

/**
 * @author PHY
 * @classname IPxWallpaperFolderService
 * @description 壁纸文件夹Service接口
 */
public interface IPxWallpaperFolderService {

    /**
     * 查询壁纸文件夹
     *
     * @param id 壁纸文件夹ID
     * @return 壁纸文件夹
     */
    PxWallpaperFolder selectPxWallpaperFolderById(Long id);

    /**
     * 查询壁纸文件夹列表
     *
     * @param pxWallpaperFolder 壁纸文件夹
     * @return 壁纸文件夹集合
     */
    List<PxWallpaperFolder> selectPxWallpaperFolderList(PxWallpaperFolder pxWallpaperFolder);

    /**
     * 查询壁纸文件夹分页列表（轻量版，不含递归 CTE，供后台分页表格使用）
     *
     * @param pxWallpaperFolder 壁纸文件夹
     * @return 壁纸文件夹集合
     */
    List<PxWallpaperFolder> selectPxWallpaperFolderPage(PxWallpaperFolder pxWallpaperFolder);

    /**
     * 新增壁纸文件夹
     *
     * @param pxWallpaperFolder 壁纸文件夹
     * @return 结果
     */
    PxWallpaperFolder insertPxWallpaperFolder(PxWallpaperFolder pxWallpaperFolder);

    /**
     * 修改壁纸文件夹
     *
     * @param pxWallpaperFolder 壁纸文件夹
     * @return 结果
     */
    PxWallpaperFolder updatePxWallpaperFolder(PxWallpaperFolder pxWallpaperFolder);

    /**
     * 批量删除壁纸文件夹
     *
     * @param ids 需要删除的壁纸文件夹ID
     * @return 结果
     */
    int deletePxWallpaperFolderByIds(Long[] ids);

    /**
     * 删除壁纸文件夹信息
     *
     * @param id 壁纸文件夹ID
     * @return 结果
     */
    int deletePxWallpaperFolderById(Long id);

    /**
     * 查询所有「未启用」文件夹及其全部子文件夹 id 集合（递归）。
     * 用于移动端排除停用文件夹下属的全部壁纸。
     *
     * @return 停用子树文件夹 id 集合
     */
    List<Long> selectDisabledFolderSubtreeIds();
}
