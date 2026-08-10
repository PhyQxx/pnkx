package com.pnkx.mapper;

import com.pnkx.domain.po.PxWallpaper;

import java.util.List;

/**
 * @author PHY
 * @classname PxWallpaperMapper
 * @description 壁纸Mapper
 */
public interface PxWallpaperMapper {

    /**
     * 查询壁纸
     *
     * @param id 壁纸ID
     * @return 壁纸
     */
    PxWallpaper selectPxWallpaperById(Long id);

    /**
     * 查询壁纸列表
     *
     * @param pxWallpaper 壁纸
     * @return 壁纸集合
     */
    List<PxWallpaper> selectPxWallpaperList(PxWallpaper pxWallpaper);

    /**
     * 根据 id 集合查询壁纸（用于批量下载）
     *
     * @param ids 壁纸ID集合
     * @return 壁纸集合
     */
    List<PxWallpaper> selectPxWallpaperByIds(Long[] ids);

    /**
     * 查询某文件夹子树下的全部壁纸（递归包含所有子文件夹）
     * 用于下载整个文件夹。
     *
     * @param folderId 文件夹ID
     * @return 壁纸集合
     */
    List<PxWallpaper> selectPxWallpaperByFolderSubtree(Long folderId);

    /**
     * 点赞数自增/自减
     *
     * @param id    壁纸ID
     * @param delta 增量（+1 点赞，-1 取消）
     * @return 影响行数
     */
    int updateLikeCount(@org.apache.ibatis.annotations.Param("id") Long id,
                        @org.apache.ibatis.annotations.Param("delta") int delta);

    /**
     * 新增壁纸
     *
     * @param pxWallpaper 壁纸
     * @return 结果
     */
    int insertPxWallpaper(PxWallpaper pxWallpaper);

    /**
     * 修改壁纸
     *
     * @param pxWallpaper 壁纸
     * @return 结果
     */
    int updatePxWallpaper(PxWallpaper pxWallpaper);

    /**
     * 删除壁纸
     *
     * @param id 壁纸ID
     * @return 结果
     */
    int deletePxWallpaperById(Long id);

    /**
     * 批量删除壁纸
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deletePxWallpaperByIds(Long[] ids);
}
