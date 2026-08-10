package com.pnkx.mapper;

import com.pnkx.domain.po.PxWallpaperFolder;

import java.util.List;

/**
 * @author PHY
 * @classname PxWallpaperFolderMapper
 * @description 壁纸文件夹Mapper
 */
public interface PxWallpaperFolderMapper {

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
     * 查询壁纸文件夹分页列表（轻量版，不含递归 CTE）。
     * 供后台分页表格使用：原 {@link #selectPxWallpaperFolderList} 使用 with recursive CTE，
     * PageHelper（MySQL 方言）对其分页拦截不稳定，故后台分页改走本方法。
     * 封面取文件夹自有 cover；壁纸数量用关联子查询按 folder 直接统计。
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
    int insertPxWallpaperFolder(PxWallpaperFolder pxWallpaperFolder);

    /**
     * 修改壁纸文件夹
     *
     * @param pxWallpaperFolder 壁纸文件夹
     * @return 结果
     */
    int updatePxWallpaperFolder(PxWallpaperFolder pxWallpaperFolder);

    /**
     * 删除壁纸文件夹
     *
     * @param id 壁纸文件夹ID
     * @return 结果
     */
    int deletePxWallpaperFolderById(Long id);

    /**
     * 批量删除壁纸文件夹
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deletePxWallpaperFolderByIds(Long[] ids);

    /**
     * 查询所有「未启用」文件夹及其全部子文件夹 id 集合（递归）。
     * 用于移动端排除停用文件夹下属的全部壁纸。
     *
     * @return 停用子树文件夹 id 集合
     */
    List<Long> selectDisabledFolderSubtreeIds();
}
