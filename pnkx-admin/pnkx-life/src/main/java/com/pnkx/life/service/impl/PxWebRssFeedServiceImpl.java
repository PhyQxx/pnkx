package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.life.domain.po.PxWebRssFeed;
import com.pnkx.life.mapper.PxWebRssFeedMapper;
import com.pnkx.life.service.IPxWebRssFeedService;
import com.pnkx.life.service.IPxWebArticleService;
import com.pnkx.life.service.webarticle.WebArticleUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxWebRssFeedServiceImpl
 * @description 网址收藏RSS订阅Service业务层处理
 * <p>
 * 拉取 RSS/Atom 源中的新条目，复用网址收藏链路（addArticleByUrl，
 * 幂等去重 + 异步抓取正文 + AI 整理），来源标记为 RSS（source=4）。
 * 注意：定时任务线程无登录态，收藏归属必须显式使用订阅创建者（feed.createBy）。
 * 连续失败 3 次的订阅自动停用，避免无效轮询（管理端可看到停用状态后手动恢复）。
 */
@Service
public class PxWebRssFeedServiceImpl implements IPxWebRssFeedService {

    private static final Logger log = LoggerFactory.getLogger(PxWebRssFeedServiceImpl.class);

    /**
     * 每个订阅源单轮最多收录的条数（RSS 通常新的在前）
     */
    private static final int MAX_ITEMS_PER_FEED = 20;

    /**
     * 连续失败达此次数自动停用订阅
     */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    @Resource
    private PxWebRssFeedMapper pxWebRssFeedMapper;

    @Resource
    private IPxWebArticleService webArticleService;

    @Override
    public List<PxWebRssFeed> selectPxWebRssFeedList(PxWebRssFeed pxWebRssFeed) {
        pxWebRssFeed.setCreateBy(SecurityUtils.getUserId());
        return pxWebRssFeedMapper.selectPxWebRssFeedList(pxWebRssFeed);
    }

    @Override
    public PxWebRssFeed insertPxWebRssFeed(PxWebRssFeed pxWebRssFeed) {
        if (StringUtils.isEmpty(pxWebRssFeed.getUrl())) {
            throw new ServiceException("RSS 链接不能为空");
        }
        String userId = SecurityUtils.getUserId();
        String urlHash = WebArticleUtils.urlHash(pxWebRssFeed.getUrl().trim());
        PxWebRssFeed existing = pxWebRssFeedMapper.selectPxWebRssFeedByHash(urlHash, userId);
        if (existing != null) {
            throw new ServiceException("该 RSS 链接已订阅过");
        }
        pxWebRssFeed.setUrlHash(urlHash);
        pxWebRssFeed.setCreateBy(userId);
        pxWebRssFeed.setCreateTime(DateUtils.getNowDate());
        pxWebRssFeed.setEnabled(StringUtils.isEmpty(pxWebRssFeed.getEnabled()) ? "1" : pxWebRssFeed.getEnabled());
        pxWebRssFeedMapper.insertPxWebRssFeed(pxWebRssFeed);
        return pxWebRssFeed;
    }

    @Override
    public boolean updatePxWebRssFeed(PxWebRssFeed pxWebRssFeed) {
        pxWebRssFeed.setUpdateBy(SecurityUtils.getUserId());
        pxWebRssFeed.setUpdateTime(DateUtils.getNowDate());
        return pxWebRssFeedMapper.updatePxWebRssFeed(pxWebRssFeed) > 0;
    }

    @Override
    public int deletePxWebRssFeedByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return pxWebRssFeedMapper.deletePxWebRssFeedByIds(ids, SecurityUtils.getUserId());
    }

    @Override
    public int fetchFeedNow(Long feedId) {
        PxWebRssFeed feed = pxWebRssFeedMapper.selectPxWebRssFeedById(feedId, SecurityUtils.getUserId());
        if (feed == null) {
            throw new ServiceException("订阅不存在");
        }
        return fetchFeed(feed);
    }

    @Override
    public int fetchAllEnabledFeeds() {
        List<PxWebRssFeed> feeds = pxWebRssFeedMapper.selectEnabledFeeds();
        int total = 0;
        for (PxWebRssFeed feed : feeds) {
            try {
                total += fetchFeed(feed);
            } catch (Exception e) {
                log.error("RSS订阅抓取异常, feed: {}", feed.getName(), e);
            }
        }
        return total;
    }

    /**
     * 抓取单个订阅源：解析 XML 提取条目链接，按旧到新逐条收录（去重由唯一索引兜底）
     */
    private int fetchFeed(PxWebRssFeed feed) {
        PxWebRssFeed result = new PxWebRssFeed();
        result.setId(feed.getId());
        result.setLastFetchTime(DateUtils.getNowDate());
        result.setUpdateTime(DateUtils.getNowDate());
        int added = 0;
        if (WebArticleUtils.isInternalUrl(feed.getUrl())) {
            log.warn("RSS订阅抓取被拒绝（内网/保留地址）, feed: {}, url: {}", feed.getName(), feed.getUrl());
            result.setFetchError("不允许订阅内网或保留地址");
            result.setConsecutiveFailures((feed.getConsecutiveFailures() == null ? 0 : feed.getConsecutiveFailures()) + 1);
            pxWebRssFeedMapper.updateFetchResult(result);
            return 0;
        }
        try {
            Document doc = Jsoup.connect(feed.getUrl())
                    .userAgent(UA)
                    .timeout(10_000)
                    .ignoreContentType(true)
                    .parser(Parser.xmlParser())
                    .get();
            List<String> links = WebArticleUtils.parseRssLinks(doc);
            // RSS 列表新的在前，倒序后按旧到新收录，保证收藏时间顺序自然；
            // 归属显式传订阅创建者（定时线程无登录态）
            for (int i = Math.min(links.size(), MAX_ITEMS_PER_FEED) - 1; i >= 0; i--) {
                String link = links.get(i);
                if (StringUtils.isEmpty(link) || !link.startsWith("http")) {
                    continue;
                }
                try {
                    // 已收藏的会命中 URL 去重直接返回，不会重复入库
                    webArticleService.addArticleByUrl(link, "RSS：" + feed.getName(), "4", feed.getCreateBy());
                    added++;
                } catch (Exception e) {
                    // 单条失败不影响其余条目（常见：URL 归一化失败）
                    log.debug("RSS条目收藏跳过, feed: {}, link: {}, 原因: {}", feed.getName(), link, e.getMessage());
                }
            }
            result.setLastFetchCount(added);
            result.setFetchError(null);
            result.setConsecutiveFailures(0);
        } catch (Exception e) {
            log.warn("RSS订阅抓取失败, feed: {}, 原因: {}", feed.getName(), e.getMessage());
            int failures = (feed.getConsecutiveFailures() == null ? 0 : feed.getConsecutiveFailures()) + 1;
            result.setConsecutiveFailures(failures);
            result.setFetchError(WebArticleUtils.truncate(
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), 1000));
            if (failures >= MAX_CONSECUTIVE_FAILURES) {
                result.setEnabled("0");
                log.error("RSS订阅连续失败 {} 次已自动停用, feed: {}", failures, feed.getName());
            }
        }
        pxWebRssFeedMapper.updateFetchResult(result);
        return added;
    }
}
