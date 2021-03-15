package com.ruoyi.px.customer.mapper;

import com.ruoyi.domain.po.PxArticle;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章表 Mapper 接口
 * </p>
 *
 * @author 裴浩宇
 * @since 2021-01-10
 */
@Mapper
public interface PxArticleMapper {

    /**
     * 获取文章列表
     * @param params 参数
     * @return 文章列表
     */
    List<Map<String, Object>> getArticleList(Map<String, Object> params);

    /**
     * 获取文章分类分组数据
     * @param params 参数
     * @return 文章分类分组数据
     */
    List<Map<String, Object>> getArticleTypeNumber(Map<String, Object> params);

    /**
     * 根据id获取文章留言列表数据
     * @param params 参数
     * @return 文章留言列表数据
     */
    List<Map<String, Object>> getLeaveMessageByArticleId(Map<String, Object> params);

    /**
     * 留言
     * @param params 参数
     * @return 留言结果
     */
    Integer addMessage(Map<String, Object> params);


    /**
     * 根据字典项type获取列表
     * @param params
     * @return
     */
    List<Map<String, Object>> getArticleTypeList(Map<String, Object> params);

    /**
     * 更新访问次数
     * @param articleId
     * @return
     */
    int updateVisits(String articleId);

    /**
     * 更新访客表
     * @param articleId
     * @return
     */
    int insertVisits(String articleId);

    /**
     * 获取首页最热文章列表
     * @return
     */
    List<Map<String, Object>> getHotArticle();
}
