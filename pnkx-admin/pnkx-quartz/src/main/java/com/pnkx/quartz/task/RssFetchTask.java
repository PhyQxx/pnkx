package com.pnkx.quartz.task;

import com.pnkx.life.service.IPxWebRssFeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * RSS 订阅自动收藏任务
 * <p>
 * 定时拉取全部启用的 RSS/Atom 订阅源，新条目自动收藏并触发 AI 整理
 * （去重由收藏表的 URL 唯一索引兜底）。
 * 调度：sys_job 配置 invoke_target = rssFetchTask.execute（V1.6.1 已注册每 30 分钟）。
 *
 * @author PHY
 */
@Component("rssFetchTask")
public class RssFetchTask {

    private static final Logger log = LoggerFactory.getLogger(RssFetchTask.class);

    @Resource
    private IPxWebRssFeedService rssFeedService;

    /**
     * 拉取全部启用的订阅源（由 quartz 调度，也可管理端手动触发）
     */
    public void execute() {
        int added = rssFeedService.fetchAllEnabledFeeds();
        if (added > 0) {
            log.info("RSS订阅自动收藏：本轮新收录 {} 条", added);
        }
    }
}
