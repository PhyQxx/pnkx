package com.pnkx.life.mapper;

import com.pnkx.life.domain.po.PxWebArticle;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author PHY
 * @classname PxWebArticleMapper
 * @description 网址收藏Mapper接口
 */
public interface PxWebArticleMapper {

    /**
     * 查询网址收藏
     *
     * @param id 网址收藏ID
     * @return 网址收藏
     */
    PxWebArticle selectPxWebArticleById(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 查询网址收藏列表（不含 content_html / content_text 大字段）
     *
     * @param pxWebArticle 查询条件（createBy 必填，按用户隔离）
     * @return 网址收藏集合
     */
    List<PxWebArticle> selectPxWebArticleList(PxWebArticle pxWebArticle);

    /**
     * 按去重键查询当前用户已收藏的记录（用于去重）
     *
     * @param urlHash URL 的 MD5 去重键
     * @param userId 用户ID
     * @return 已存在的记录，不存在返回 null
     */
    PxWebArticle selectPxWebArticleByUrl(@Param("urlHash") String urlHash, @Param("userId") String userId);

    /**
     * 查询当前用户的全部标签串（去重由 Java 侧完成）
     *
     * @param userId 用户ID
     * @return 非空 tags 字段集合
     */
    List<String> selectPxWebArticleTags(@Param("userId") String userId);

    /**
     * 新增网址收藏
     *
     * @param pxWebArticle 网址收藏
     * @return 结果
     */
    int insertPxWebArticle(PxWebArticle pxWebArticle);

    /**
     * 修改网址收藏（人工修正字段，where 校验归属）
     *
     * @param pxWebArticle 网址收藏（updateBy 为当前用户）
     * @return 结果
     */
    int updatePxWebArticle(PxWebArticle pxWebArticle);

    /**
     * 抓取结果回写（大字段与状态独立更新，避免人工编辑字段被覆盖）
     *
     * @param pxWebArticle 网址收藏
     * @return 结果
     */
    int updateFetchResult(PxWebArticle pxWebArticle);

    /**
     * AI 分析结果回写
     *
     * @param pxWebArticle 网址收藏
     * @return 结果
     */
    int updateAiResult(PxWebArticle pxWebArticle);

    /**
     * 重置抓取状态（重新抓取前调用）
     */
    int resetFetchStatus(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 重置 AI 分析状态（重新分析前调用）
     */
    int resetAiStatus(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 更新已读标记
     */
    int updateReadFlag(@Param("id") Long id, @Param("userId") String userId, @Param("readFlag") String readFlag);

    /**
     * 更新星标
     */
    int updateStarFlag(@Param("id") Long id, @Param("userId") String userId, @Param("starFlag") String starFlag);

    /**
     * 批量删除网址收藏（软删除）
     *
     * @param ids   需要删除的ID集合
     * @param userId 当前用户（归属校验）
     * @return 结果
     */
    int deletePxWebArticleByIds(@Param("ids") Long[] ids, @Param("userId") String userId);

    /**
     * 查询可自动重试的失败记录（抓取失败，或抓取成功但AI分析失败），
     * 且 update_time 早于 before（避免与刚失败的处理碰撞）、retry_count 未达上限
     *
     * @param before   最早失败时间边界
     * @param maxRetry 重试次数上限
     * @return 候选记录（含 id / create_by / fetch_status / ai_status）
     */
    List<PxWebArticle> selectRetryCandidates(@Param("before") Date before, @Param("maxRetry") int maxRetry);

    /**
     * 重试次数 +1（定时任务触发时调用）
     */
    int incrementRetryCount(@Param("id") Long id);

    /**
     * 查询当前用户全部收藏（导出用，含正文纯文本，按收藏时间正序）
     */
    List<PxWebArticle> selectPxWebArticleExport(@Param("userId") String userId);

    /**
     * 运营统计总览：总数/未读/星标/抓取与AI成败/来源分布（当前用户）
     */
    Map<String, Object> selectStatsOverview(@Param("userId") String userId);

    /**
     * 分类分布（当前用户）
     */
    List<Map<String, Object>> selectCategoryStats(@Param("userId") String userId);

    /**
     * 近 N 天收藏趋势（按日）
     */
    List<Map<String, Object>> selectDailyTrend(@Param("userId") String userId, @Param("days") int days);

    /**
     * 标签管理：查询当前用户含指定标签的所有行（id + tags）
     */
    List<PxWebArticle> selectRowsByTag(@Param("userId") String userId, @Param("tag") String tag);

    /**
     * 标签管理：整行替换 tags 字段（Java 侧完成拆分/重命名/去重后回写）
     */
    int updateTags(@Param("id") Long id, @Param("tags") String tags, @Param("updateBy") String updateBy);
}
