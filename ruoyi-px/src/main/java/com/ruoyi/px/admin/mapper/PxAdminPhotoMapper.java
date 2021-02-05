package com.ruoyi.px.admin.mapper;

import com.ruoyi.domain.po.PxPhoto;

import java.util.List;

/**
 * 相册Mapper接口
 *
 * @author ruoyi
 * @date 2021-02-05
 */
public interface PxAdminPhotoMapper
{
    /**
     * 查询相册
     *
     * @param id 相册ID
     * @return 相册
     */
    public PxPhoto selectPxPhotoById(String id);

    /**
     * 查询相册列表
     *
     * @param pxPhoto 相册
     * @return 相册集合
     */
    public List<PxPhoto> selectPxPhotoList(PxPhoto pxPhoto);

    /**
     * 新增相册
     *
     * @param pxPhoto 相册
     * @return 结果
     */
    public int insertPxPhoto(PxPhoto pxPhoto);

    /**
     * 修改相册
     *
     * @param pxPhoto 相册
     * @return 结果
     */
    public int updatePxPhoto(PxPhoto pxPhoto);

    /**
     * 删除相册
     *
     * @param id 相册ID
     * @return 结果
     */
    public int deletePxPhotoById(String id);

    /**
     * 批量删除相册
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxPhotoByIds(String[] ids);
}
