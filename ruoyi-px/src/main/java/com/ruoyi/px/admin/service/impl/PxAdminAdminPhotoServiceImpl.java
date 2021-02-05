package com.ruoyi.px.admin.service.impl;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.domain.po.PxPhoto;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.px.admin.mapper.PxAdminPhotoMapper;
import com.ruoyi.px.admin.service.IPxAdminPhotoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 相册Service业务层处理
 *
 * @author ruoyi
 * @date 2021-02-05
 */
@Service
public class PxAdminAdminPhotoServiceImpl implements IPxAdminPhotoService
{
    @Resource
    private PxAdminPhotoMapper pxAdminPhotoMapper;
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
        return pxAdminPhotoMapper.selectPxPhotoById(id);
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
        return pxAdminPhotoMapper.selectPxPhotoList(pxPhoto);
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
        return pxAdminPhotoMapper.insertPxPhoto(pxPhoto);
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
        return pxAdminPhotoMapper.updatePxPhoto(pxPhoto);
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
        return pxAdminPhotoMapper.deletePxPhotoByIds(ids);
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
        return pxAdminPhotoMapper.deletePxPhotoById(id);
    }
}
