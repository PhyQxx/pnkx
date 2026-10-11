package com.pnkx.life.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author PHY
 * @classname PxWebRssFeed
 * @description 网址收藏RSS订阅源 px_web_rss_feed
 */
@Data
public class PxWebRssFeed extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 订阅名称
     */
    private String name;

    /**
     * RSS/Atom 链接
     */
    private String url;

    /**
     * RSS链接去重键(MD5)
     */
    private String urlHash;

    /**
     * 是否启用：0停用 1启用
     */
    private String enabled;

    /**
     * 最近抓取时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastFetchTime;

    /**
     * 最近一轮新收录条数
     */
    private Integer lastFetchCount;

    /**
     * 最近抓取失败原因
     */
    private String fetchError;

    /**
     * 连续失败次数，达3次自动停用
     */
    private Integer consecutiveFailures;

    /**
     * 删除标志：0存在 1删除
     */
    private String delFlag;
}
