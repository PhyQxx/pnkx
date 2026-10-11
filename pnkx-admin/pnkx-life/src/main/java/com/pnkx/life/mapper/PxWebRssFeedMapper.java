package com.pnkx.life.mapper;

import com.pnkx.life.domain.po.PxWebRssFeed;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author PHY
 * @classname PxWebRssFeedMapper
 * @description 网址收藏RSS订阅Mapper接口
 */
public interface PxWebRssFeedMapper {

    /**
     * 查询订阅列表（按用户隔离）
     */
    List<PxWebRssFeed> selectPxWebRssFeedList(PxWebRssFeed pxWebRssFeed);

    /**
     * 查询订阅详情
     */
    PxWebRssFeed selectPxWebRssFeedById(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 查询全部启用的订阅（定时任务用，不限用户）
     */
    List<PxWebRssFeed> selectEnabledFeeds();

    /**
     * 按去重键查询当前用户的订阅（用于新增去重）
     */
    PxWebRssFeed selectPxWebRssFeedByHash(@Param("urlHash") String urlHash, @Param("userId") String userId);

    /**
     * 新增订阅
     */
    int insertPxWebRssFeed(PxWebRssFeed pxWebRssFeed);

    /**
     * 修改订阅（where 校验归属）
     */
    int updatePxWebRssFeed(PxWebRssFeed pxWebRssFeed);

    /**
     * 批量软删除
     */
    int deletePxWebRssFeedByIds(@Param("ids") Long[] ids, @Param("userId") String userId);

    /**
     * 抓取结果回写（last_fetch_time / last_fetch_count / fetch_error）
     */
    int updateFetchResult(PxWebRssFeed pxWebRssFeed);
}
