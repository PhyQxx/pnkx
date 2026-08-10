package com.pnkx.mapper;

import com.pnkx.common.core.domain.entity.SysDictData;
import com.pnkx.domain.po.PxArticle;
import com.pnkx.domain.vo.PxArticleVo;

import java.util.List;
import java.util.Map;

/**
 * 文章Mapper接口
 *
 * @author phy
 * @date 2021-01-26
 */
public interface PxArticleMapper {
    /**
     * 查询文章
     *
     * @param id 文章ID
     * @return 文章
     */
    PxArticleVo getArticleById(String id);

    /**
     * 查询上一篇文章
     *
     * @param id 文章ID
     * @return 文章
     */
    PxArticle getLastArticleById(String id);

    /**
     * 查询下一篇文章
     *
     * @param id 文章ID
     * @return 文章
     */
    PxArticle getNextArticleById(String id);

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
     * 删除文章
     *
     * @param id 文章ID
     * @return 结果
     */
    int deletePxArticleById(String id);

    /**
     * 批量删除文章
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deletePxArticleByIds(String[] ids);

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
     * 获取首页最热文章列表
     *
     * @return
     */
    List<Map<String, Object>> getHotArticle();

    /**
     * 更新访问次数
     *
     * @param articleId
     * @return
     */
    int updateVisits(String articleId);


    /**
     * 获取类型下文章数量
     * @param type
     * @return
     */
    Integer selectPxArticleNumberByType(String type);

    /**
     * 获取文章标签列表
     * @return
     */
    List<String> getLabelList();
}
