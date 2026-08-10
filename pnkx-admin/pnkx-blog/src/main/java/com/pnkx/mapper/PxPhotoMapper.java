package com.pnkx.mapper;

import com.pnkx.domain.po.PxPhoto;

import java.util.List;
import java.util.Map;

/**
 * 相册Mapper接口
 *
 * @author phy
 * @date 2021-02-05
 */
public interface PxPhotoMapper {

    /**
     * 查询相册
     *
     * @param id 相册ID
     * @return 相册
     */
    PxPhoto selectPxPhotoById(String id);

    /**
     * 查询相册列表
     *
     * @param pxPhoto 相册
     * @return 相册集合
     */
    List<PxPhoto> selectPxPhotoList(PxPhoto pxPhoto);

    /**
     * 新增相册
     *
     * @param pxPhoto 相册
     * @return 结果
     */
    int insertPxPhoto(PxPhoto pxPhoto);

    /**
     * 修改相册
     *
     * @param pxPhoto 相册
     * @return 结果
     */
    int updatePxPhoto(PxPhoto pxPhoto);

    /**
     * 删除相册
     *
     * @param id 相册ID
     * @return 结果
     */
    int deletePxPhotoById(String id);

    /**
     * 批量删除相册
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deletePxPhotoByIds(String[] ids);

    /**
     * 获取相册列表
     *
     * @param params
     * @return
     */
    List<Map<String, Object>> getAlbumList(Map<String, Object> params);
}
