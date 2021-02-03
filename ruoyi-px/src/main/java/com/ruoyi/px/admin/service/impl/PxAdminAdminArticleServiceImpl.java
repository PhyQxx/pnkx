package com.ruoyi.px.admin.service.impl;

import java.util.List;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.domain.po.PxArticle;
import com.ruoyi.framework.web.service.TokenService;
import org.springframework.stereotype.Service;
import com.ruoyi.px.admin.mapper.PxAdminArticleMapper;
import com.ruoyi.px.admin.service.IPxAdminArticleService;

import javax.annotation.Resource;

/**
 * 文章Service业务层处理
 *
 * @author ruoyi
 * @date 2021-01-26
 */
@Service
public class PxAdminAdminArticleServiceImpl implements IPxAdminArticleService
{
    @Resource
    private PxAdminArticleMapper pxAdminArticleMapper;
    @Resource
    private TokenService tokenService;
    /**
     * 查询文章
     *
     * @param id 文章ID
     * @return 文章
     */
    @Override
    public PxArticle selectPxArticleById(String id)
    {
        return pxAdminArticleMapper.selectPxArticleById(id);
    }

    /**
     * 查询文章列表
     *
     * @param pxArticle 文章
     * @return 文章
     */
    @Override
    public List<PxArticle> selectPxArticleList(PxArticle pxArticle)
    {
        LoginUser loginUser=tokenService.getLoginUser(ServletUtils.getRequest());
        pxArticle.setCreateBy(loginUser.getUser().getUserId().toString());
        return pxAdminArticleMapper.selectPxArticleList(pxArticle);
    }

    /**
     * 新增文章
     *
     * @param pxArticle 文章
     * @return 结果
     */
    @Override
    public int insertPxArticle(PxArticle pxArticle)
    {
        pxArticle.setCreateTime(DateUtils.getNowDate());
        LoginUser loginUser=tokenService.getLoginUser(ServletUtils.getRequest());
        pxArticle.setCreateBy(loginUser.getUser().getUserId().toString());
        return pxAdminArticleMapper.insertPxArticle(pxArticle);
    }

    /**
     * 修改文章
     *
     * @param pxArticle 文章
     * @return 结果
     */
    @Override
    public int updatePxArticle(PxArticle pxArticle)
    {
        pxArticle.setUpdateTime(DateUtils.getNowDate());
        LoginUser loginUser=tokenService.getLoginUser(ServletUtils.getRequest());
        pxArticle.setUpdateBy(loginUser.getUser().getUserId().toString());
        return pxAdminArticleMapper.updatePxArticle(pxArticle);
    }

    /**
     * 批量删除文章
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    @Override
    public int deletePxArticleByIds(String[] ids)
    {
        return pxAdminArticleMapper.deletePxArticleByIds(ids);
    }

    /**
     * 删除文章信息
     *
     * @param id 文章ID
     * @return 结果
     */
    @Override
    public int deletePxArticleById(String id)
    {
        return pxAdminArticleMapper.deletePxArticleById(id);
    }
}
