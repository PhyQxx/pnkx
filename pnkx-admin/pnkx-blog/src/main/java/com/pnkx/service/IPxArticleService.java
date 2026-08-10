package com.pnkx.service;

import com.pnkx.common.core.domain.entity.SysDictData;
import com.pnkx.domain.po.PxArticle;
import com.pnkx.domain.po.PxArticleType;
import com.pnkx.domain.vo.PxArticleTypeVo;
import com.pnkx.domain.vo.PxArticleVo;

import java.util.List;
import java.util.Map;

/**
 * 文章Service接口
 *
 * @author phy
 * @date 2021-01-26
 */
public interface IPxArticleService {
    /**
     * 获取文章根据ID
     *
     * @param id 参数
     * @return 文章列表
     */
    PxArticleVo getArticleById(String id);

    /**
     * 查询文章列表
     *
     * @param pxArticle 文章
     * @return 文章集合
     */
    List<PxArticleVo> selectPxArticleList(PxArticle pxArticle);

    /**
     * 新增文章
     *
     * @param pxArticle 文章
     * @return 结果
     */
    Integer insertPxArticle(PxArticleVo pxArticle);

    /**
     * 修改文章
     *
     * @param pxArticle 文章
     * @return 结果
     */
    int updatePxArticle(PxArticleVo pxArticle);

    /**
     * 批量删除文章
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    int deletePxArticleByIds(String[] ids);

    /**
     * 删除文章信息
     *
     * @param id 文章ID
     * @return 结果
     */
    int deletePxArticleById(String id);

    /**
     * 校验字典项标签、键值唯一性
     *
     * @param dictData
     * @return
     */
    Integer dictDataCheckUniqueness(SysDictData dictData);

    /**
     * 查询文章列表不包含内容
     *
     * @param pxArticle
     * @return
     */
    List<PxArticleVo> selectPxArticleNotContent(PxArticleVo pxArticle);

    /**
     * 查询文章列表不包含内容
     *
     * @param pxArticle
     * @return
     */
    List<PxArticleVo> selectPxArticleOrdinaryContent(PxArticleVo pxArticle);

    /**
     * 获取首页最热、随机文章
     *
     * @return
     */
    List<Map<String, Object>> getHotArticle();

    /**
     * 文章按类型分组
     * @return 结果
     */
    List<PxArticleType> getArticleListGroupByType();


    /**
     * 获取类型下文章数量
     * @return
     */
    List<PxArticleTypeVo> selectPxArticleByType(SysDictData dictData);

    /**
     * 获取文章标签列表
     * @return
     */
    List<String> getLabelList();

    /**
     * 获取文章URL列表
     * @return
     */
    String getArticleUrls();
}
