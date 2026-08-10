package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxPhoto;
import com.pnkx.mapper.PxPhotoMapper;
import com.pnkx.service.IPxPhotoService;
import com.pnkx.common.utils.DateUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 相册Service业务层处理
 *
 * @author phy
 * @date 2021-02-05
 */
@Service
public class PxPhotoServiceImpl implements IPxPhotoService {
    @Resource
    private PxPhotoMapper pxPhotoMapper;

    /**
     * 获取相册列表
     *
     * @param params
     * @return
     */
    @Override
    public List<Map<String, Object>> getAlbumList(Map<String, Object> params) {
        return pxPhotoMapper.getAlbumList(params);
    }

    /**
     * 查询相册
     *
     * @param id 相册ID
     * @return 相册
     */
    @Override
    public PxPhoto selectPxPhotoById(String id) {
        return pxPhotoMapper.selectPxPhotoById(id);
    }

    /**
     * 查询相册列表
     *
     * @param pxPhoto 相册
     * @return 相册
     */
    @Override
    @DataScopeSelf
    public List<PxPhoto> selectPxPhotoList(PxPhoto pxPhoto) {
        return pxPhotoMapper.selectPxPhotoList(pxPhoto);
    }

    /**
     * 新增相册
     *
     * @param pxPhoto 相册
     * @return 结果
     */
    @Override
    public int insertPxPhoto(PxPhoto pxPhoto) {
        pxPhoto.setCreateTime(DateUtils.getNowDate());
        pxPhoto.setCreateBy(SecurityUtils.getUserId());
        return pxPhotoMapper.insertPxPhoto(pxPhoto);
    }

    /**
     * 修改相册
     *
     * @param pxPhoto 相册
     * @return 结果
     */
    @Override
    public int updatePxPhoto(PxPhoto pxPhoto) {
        pxPhoto.setUpdateBy(SecurityUtils.getUserName());
        pxPhoto.setUpdateTime(DateUtils.getNowDate());
        return pxPhotoMapper.updatePxPhoto(pxPhoto);
    }

    /**
     * 批量删除相册
     *
     * @param ids 需要删除的相册ID
     * @return 结果
     */
    @Override
    public int deletePxPhotoByIds(String[] ids) {
        return pxPhotoMapper.deletePxPhotoByIds(ids);
    }

    /**
     * 删除相册信息
     *
     * @param id 相册ID
     * @return 结果
     */
    @Override
    public int deletePxPhotoById(String id) {
        return pxPhotoMapper.deletePxPhotoById(id);
    }
}
