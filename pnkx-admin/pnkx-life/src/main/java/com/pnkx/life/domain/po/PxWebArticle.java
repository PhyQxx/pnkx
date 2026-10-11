package com.pnkx.life.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author PHY
 * @classname PxWebArticle
 * @description 网址收藏对象 px_web_article
 */
@Data
public class PxWebArticle extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 归一化后的原文链接
     */
    @Excel(name = "原文链接")
    private String url;

    /**
     * URL去重键(MD5)，解决超长链接唯一索引前缀碰撞
     */
    private String urlHash;

    /**
     * 站点域名
     */
    @Excel(name = "站点域名")
    private String domain;

    /**
     * 标题
     */
    @Excel(name = "标题")
    private String title;

    /**
     * 作者
     */
    @Excel(name = "作者")
    private String author;

    /**
     * 原文发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 站点图标
     */
    private String favicon;

    /**
     * 正文纯文本
     */
    private String contentText;

    /**
     * XSS清理后的正文HTML快照
     */
    private String contentHtml;

    /**
     * AI摘要
     */
    @Excel(name = "AI摘要")
    private String summary;

    /**
     * 标签，逗号分隔
     */
    @Excel(name = "标签")
    private String tags;

    /**
     * AI分类
     */
    @Excel(name = "分类")
    private String category;

    /**
     * 抓取状态：0待抓取 1成功 2失败
     */
    @Excel(name = "抓取状态")
    private String fetchStatus;

    /**
     * 抓取失败原因
     */
    private String fetchError;

    /**
     * AI分析状态：0待分析 1成功 2失败
     */
    @Excel(name = "AI分析状态")
    private String aiStatus;

    /**
     * AI分析失败原因
     */
    private String aiError;

    /**
     * 自动重试次数，上限3次
     */
    private Integer retryCount;

    /**
     * 分析所用模型
     */
    private String aiModel;

    /**
     * 已读标记：0未读 1已读
     */
    @Excel(name = "已读标记")
    private String readFlag;

    /**
     * 星标：0否 1是
     */
    private String starFlag;

    /**
     * 来源：1手动添加 2AI对话 3书签脚本 4RSS订阅
     */
    private String source;

    /**
     * 删除标志：0存在 1删除
     */
    private String delFlag;

    /**
     * 关键字（非持久化，匹配标题/摘要/链接）
     */
    private String keyword;

    /**
     * 标签筛选（非持久化，单个标签精确匹配）
     */
    private String tag;
}
