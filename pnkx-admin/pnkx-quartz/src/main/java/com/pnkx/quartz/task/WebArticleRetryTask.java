package com.pnkx.quartz.task;

import com.pnkx.life.domain.po.PxWebArticle;
import com.pnkx.life.mapper.PxWebArticleMapper;
import com.pnkx.life.service.webarticle.PxWebArticleTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 网址收藏失败自动重试任务
 * <p>
 * 定时扫描抓取失败（fetch_status=2）或 AI 分析失败（ai_status=2）且
 * 10 分钟内状态无变化的收藏记录，自动重试（每条最多 3 次，超限需人工触发）。
 * 调度：sys_job 配置 invoke_target = webArticleRetryTask.execute（V1.6.0 已注册每 30 分钟）。
 *
 * @author PHY
 */
@Component("webArticleRetryTask")
public class WebArticleRetryTask {

    private static final Logger log = LoggerFactory.getLogger(WebArticleRetryTask.class);

    /**
     * 失败后至少间隔的冷却时间（分钟），避免与刚失败的处理碰撞
     */
    private static final int COOLDOWN_MINUTES = 10;

    /**
     * 单条记录的自动重试上限
     */
    private static final int MAX_RETRY = 3;

    /**
     * 单轮最多处理的记录数
     */
    private static final int BATCH_LIMIT = 20;

    @Resource
    private PxWebArticleMapper webArticleMapper;

    @Resource
    private PxWebArticleTaskService taskService;

    /**
     * 执行失败重试（由 quartz 调度，也可管理端手动触发）
     */
    public void execute() {
        Date before = new Date(System.currentTimeMillis() - COOLDOWN_MINUTES * 60_000L);
        List<PxWebArticle> candidates = webArticleMapper.selectRetryCandidates(before, MAX_RETRY);
        if (candidates.isEmpty()) {
            return;
        }
        int refetched = 0;
        int reanalyzed = 0;
        for (PxWebArticle article : candidates) {
            if (refetched + reanalyzed >= BATCH_LIMIT) {
                break;
            }
            webArticleMapper.incrementRetryCount(article.getId());
            if ("2".equals(article.getFetchStatus())) {
                webArticleMapper.resetFetchStatus(article.getId(), article.getCreateBy());
                taskService.fetchAndAnalyze(article.getId());
                refetched++;
            } else {
                webArticleMapper.resetAiStatus(article.getId(), article.getCreateBy());
                taskService.analyzeAsync(article.getId());
                reanalyzed++;
            }
        }
        log.info("网址收藏失败重试：本轮重新抓取 {} 条，重新分析 {} 条", refetched, reanalyzed);
    }
}
