package com.ruoyi.admin.service;

import com.ruoyi.px.domain.PxArticle;

import java.util.List;

/**
 * 文章Service接口
 *
 * @author ruoyi
 * @date 2021-01-26
 */
public interface IPxAdminArticleService
{
    /**
     * 查询文章
     *
     * @param id 文章ID
     * @return 文章
     */
    public PxArticle selectPxArticleById(String id);

    /**
     * 查询文章列表
     *
     * @param pxArticle 文章
     * @return 文章集合
     */
    public List<PxArticle> selectPxArticleList(PxArticle pxArticle);

    /**
     * 新增文章
     *
     * @param pxArticle 文章
     * @return 结果
     */
    public int insertPxArticle(PxArticle pxArticle);

    /**
     * 修改文章
     *
     * @param pxArticle 文章
     * @return 结果
     */
    public int updatePxArticle(PxArticle pxArticle);

    /**
     * 批量删除文章
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    public int deletePxArticleByIds(String[] ids);

    /**
     * 删除文章信息
     *
     * @param id 文章ID
     * @return 结果
     */
    public int deletePxArticleById(String id);
}
