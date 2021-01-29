package com.ruoyi.px.service.impl;

import com.ruoyi.px.mapper.PxArticleMapper;
import com.ruoyi.px.service.IPxArticleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author 裴浩宇
 * @since 2021-01-10
 */
@Service
public class PxArticleServiceImpl implements IPxArticleService {

    @Resource
    PxArticleMapper pxArticleMapper;

    /**
     * 获取文章列表
     * @param params 参数
     * @return 文章列表
     */
    @Override
    public List<Map<String, Object>> getArticleList(Map<String, Object> params) {
        return pxArticleMapper.getArticleList(params);
    }

    /**
     * 获获取文章分类分组数据
     * @param params 参数
     * @return 文章分类分组数据
     */
    @Override
    public List<Map<String, Object>> getArticleTypeNumber(Map<String, Object> params) {
        return pxArticleMapper.getArticleTypeNumber(params);
    }

    /**
     * 根据id获取文章留言列表数据
     * @param params 参数
     * @return 文章留言列表数据
     */
    @Override
    public List<Map<String, Object>> getLeaveMessageByArticleId(Map<String, Object> params) {
        return pxArticleMapper.getLeaveMessageByArticleId(params);
    }

    /**
     * 留言
     * @param params 参数
     * @return 留言结果
     */
    @Override
    public Integer addMessage(Map<String, Object> params) {
        return pxArticleMapper.addMessage(params);
    }
}
