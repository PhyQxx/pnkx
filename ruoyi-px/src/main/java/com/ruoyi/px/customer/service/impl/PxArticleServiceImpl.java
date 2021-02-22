package com.ruoyi.px.customer.service.impl;

import com.ruoyi.domain.po.PxArticle;
import com.ruoyi.domain.vo.PxArticleVo;
import com.ruoyi.px.admin.mapper.PxAdminArticleMapper;
import com.ruoyi.px.customer.mapper.PxArticleMapper;
import com.ruoyi.px.customer.service.IPxArticleService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    @Resource
    private PxAdminArticleMapper pxAdminArticleMapper;
    @Resource
    private RedisTemplate redisTemplate;

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
    public List<PxArticleVo> selectPxArticleList(PxArticle pxArticle)
    {
        String key = "pxArticleList" + pxArticle.toString().substring(pxArticle.toString().indexOf("[") + 1, pxArticle.toString().indexOf("]"));
        ValueOperations<String, List<PxArticleVo>> operations = redisTemplate.opsForValue();
        boolean hasKey = redisTemplate.hasKey(key);
        if (hasKey) {
            List<PxArticleVo> result= operations.get(key);
            return result;
        }
        List<PxArticleVo> result = pxAdminArticleMapper.selectPxArticleList(pxArticle);
        operations.set(key, result);
        return result;
    }

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

    /**
     * 获取文章类型列表
     * @param params
     * @return
     */
    @Override
    public Object getArticleTypeList(Map<String, Object> params) {
        return pxArticleMapper.getArticleTypeList(params);
    }
}
