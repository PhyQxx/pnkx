package com.ruoyi.px.admin.mapper;

import java.util.List;

import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.domain.po.PxArticle;
import com.ruoyi.domain.vo.PxArticleVo;

/**
 * 文章Mapper接口
 *
 * @author ruoyi
 * @date 2021-01-26
 */
public interface PxAdminArticleMapper
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
    public List<PxArticleVo> selectPxArticleList(PxArticle pxArticle);

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
     * 删除文章
     *
     * @param id 文章ID
     * @return 结果
     */
    public int deletePxArticleById(String id);

    /**
     * 批量删除文章
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxArticleByIds(String[] ids);

    /**
     * 校验字典项标签、键值唯一性
     * @param dictData
     * @return
     */
    Integer dictDataCheckUniqueness(SysDictData dictData);
}
