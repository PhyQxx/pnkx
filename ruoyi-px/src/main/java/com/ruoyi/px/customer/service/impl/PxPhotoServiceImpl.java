package com.ruoyi.px.customer.service.impl;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.domain.po.PxPhoto;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.px.customer.mapper.PxPhotoMapper;
import com.ruoyi.px.customer.service.IPxPhotoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 相册Service业务层处理
 *
 * @author ruoyi
 * @date 2021-02-05
 */
@Service
public class PxPhotoServiceImpl implements IPxPhotoService
{
    @Resource
    private PxPhotoMapper pxPhotoMapper;
    @Resource
    private TokenService tokenService;

    /**
     * 查询相册
     *
     * @param id 相册ID
     * @return 相册
     */
    @Override
    public PxPhoto selectPxPhotoById(String id)
    {
        return pxPhotoMapper.selectPxPhotoById(id);
    }

    /**
     * 查询相册列表
     *
     * @param pxPhoto 相册
     * @return 相册
     */
    @Override
    public List<PxPhoto> selectPxPhotoList(PxPhoto pxPhoto)
    {
        return pxPhotoMapper.selectPxPhotoList(pxPhoto);
    }

    /**
     * 新增相册
     *
     * @param pxPhoto 相册
     * @return 结果
     */
    @Override
    public int insertPxPhoto(PxPhoto pxPhoto)
    {
        pxPhoto.setCreateTime(DateUtils.getNowDate());
        LoginUser loginUser=tokenService.getLoginUser(ServletUtils.getRequest());
        pxPhoto.setCreateBy(loginUser.getUser().getUserId().toString());
        return pxPhotoMapper.insertPxPhoto(pxPhoto);
    }

    /**
     * 修改相册
     *
     * @param pxPhoto 相册
     * @return 结果
     */
    @Override
    public int updatePxPhoto(PxPhoto pxPhoto)
    {
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
    public int deletePxPhotoByIds(String[] ids)
    {
        return pxPhotoMapper.deletePxPhotoByIds(ids);
    }

    /**
     * 删除相册信息
     *
     * @param id 相册ID
     * @return 结果
     */
    @Override
    public int deletePxPhotoById(String id)
    {
        return pxPhotoMapper.deletePxPhotoById(id);
    }

    /**
     * 获取相册列表
     * @param params
     * @return
     */
    @Override
    public List<Map<String, Object>> getAlbumList(Map<String, Object> params) {
        return pxPhotoMapper.getAlbumList(params);
    }
}
