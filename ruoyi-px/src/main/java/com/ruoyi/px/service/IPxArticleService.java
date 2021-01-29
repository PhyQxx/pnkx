package com.ruoyi.px.service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章表 服务类
 * </p>
 *
 * @author 裴浩宇
 * @since 2021-01-10
 */
public interface IPxArticleService {

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
}
