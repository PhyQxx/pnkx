package com.pnkx.life.service;

import com.pnkx.life.domain.po.PxWebArticle;

import java.util.List;
import java.util.Map;

/**
 * @author PHY
 * @classname IPxWebArticleService
 * @description 网址收藏Service接口
 */
public interface IPxWebArticleService {

    /**
     * 查询网址收藏详情（含正文快照，按当前用户隔离）
     *
     * @param id 主键
     * @return 网址收藏
     */
    PxWebArticle selectPxWebArticleById(Long id);

    /**
     * 查询网址收藏列表（当前用户数据，不含正文大字段）
     *
     * @param pxWebArticle 查询条件
     * @return 网址收藏集合
     */
    List<PxWebArticle> selectPxWebArticleList(PxWebArticle pxWebArticle);

    /**
     * 收藏网址：归一化、去重、入库并触发异步抓取+AI分析（来源默认为手动添加）
     *
     * @param url    原始链接
     * @param remark 备注（可为空）
     * @return 已存在或新建的记录（抓取/分析异步进行中）
     */
    PxWebArticle addArticleByUrl(String url, String remark);

    /**
     * 收藏网址（指定来源，归属当前登录用户）
     *
     * @param source 来源：1手动添加 2AI对话 3书签脚本 4RSS订阅
     */
    PxWebArticle addArticleByUrl(String url, String remark, String source);

    /**
     * 收藏网址（指定来源与归属用户）
     *
     * @param source 来源：1手动添加 2AI对话 3书签脚本 4RSS订阅
     * @param userId 归属用户ID（定时任务等无登录态场景必须显式传入）
     */
    PxWebArticle addArticleByUrl(String url, String remark, String source, String userId);

    /**
     * 人工修正（标题/摘要/标签/分类/已读/星标/备注）
     */
    PxWebArticle updatePxWebArticle(PxWebArticle pxWebArticle);

    /**
     * 批量删除（软删除，仅限本人数据）
     */
    int deletePxWebArticleByIds(Long[] ids);

    /**
     * 更新已读标记
     */
    boolean updateReadFlag(Long id, String readFlag);

    /**
     * 更新星标
     */
    boolean updateStarFlag(Long id, String starFlag);

    /**
     * 重新抓取正文并重新 AI 分析
     */
    boolean retryFetch(Long id);

    /**
     * 仅重新 AI 分析（正文不变）
     */
    boolean retryAnalyze(Long id);

    /**
     * 当前用户全部标签（去重，用于筛选下拉）
     */
    List<String> selectTags();

    /**
     * 导出当前用户全部收藏为 Markdown 文档
     */
    String exportMarkdown();

    /**
     * 运营统计（总览/分类分布/近30天趋势，当前用户）
     */
    Map<String, Object> selectStats();

    /**
     * 标签统计（标签名 + 文章数，当前用户）
     */
    List<Map<String, Object>> selectTagStats();

    /**
     * 标签重命名（新名已存在时自动合并去重）
     *
     * @return 受影响文章数
     */
    int renameTag(String oldTag, String newTag);

    /**
     * 从所有文章移除指定标签
     *
     * @return 受影响文章数
     */
    int deleteTag(String tag);
}
