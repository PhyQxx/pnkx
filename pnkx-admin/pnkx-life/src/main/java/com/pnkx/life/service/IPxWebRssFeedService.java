package com.pnkx.life.service;

import com.pnkx.life.domain.po.PxWebRssFeed;

import java.util.List;

/**
 * @author PHY
 * @classname IPxWebRssFeedService
 * @description 网址收藏RSS订阅Service接口
 */
public interface IPxWebRssFeedService {

    /**
     * 查询订阅列表（当前用户）
     */
    List<PxWebRssFeed> selectPxWebRssFeedList(PxWebRssFeed pxWebRssFeed);

    /**
     * 新增订阅（URL 去重）
     */
    PxWebRssFeed insertPxWebRssFeed(PxWebRssFeed pxWebRssFeed);

    /**
     * 修改订阅（名称/链接/启用状态/备注）
     */
    boolean updatePxWebRssFeed(PxWebRssFeed pxWebRssFeed);

    /**
     * 批量删除订阅（软删除）
     */
    int deletePxWebRssFeedByIds(Long[] ids);

    /**
     * 立即抓取单个订阅（当前用户），返回新收录条数
     */
    int fetchFeedNow(Long feedId);

    /**
     * 抓取全部启用的订阅（定时任务调用），返回本轮新收录总条数
     */
    int fetchAllEnabledFeeds();
}
