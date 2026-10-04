package com.pnkx.life.service;

import com.pnkx.life.domain.po.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 离线同步 Service
 *
 * 提供：
 * 1. 幂等写入 — 根据 clientUuid 去重，已存在则跳过并返回已有 ID
 * 2. 增量查询 — 根据 since 时间 + userId 查询变更数据
 *
 * @author PHY
 */
@Service
public interface IPxOfflineSyncService {

    // ──────────── 日记 ────────────

    /**
     * 日记幂等新增
     * @return 插入后的 ID（已存在则返回已有 ID）
     */
    public Long insertDiaryIdempotent(Map<String, Object> payload, String clientUuid);

    /**
     * 日记增量查询
     */
    public List<PxDiary> selectDiaryIncremental(String userId, String since, int offset, int limit);

    // ──────────── 待办 ────────────

    public Long insertToDoIdempotent(Map<String, Object> payload, String clientUuid);

    public List<PxToDo> selectToDoIncremental(String userId, String since, int offset, int limit);

    // ──────────── 记账 ────────────

    public Long insertBookkeepingRecordIdempotent(Map<String, Object> payload, String clientUuid);

    public List<PxBookkeepingRecord> selectRecordIncremental(String userId, String since, int offset, int limit);

    // ──────────── 笔记 ────────────

    public Long insertNoteIdempotent(Map<String, Object> payload, String clientUuid);

    public List<PxNote> selectNoteIncremental(String userId, String since, int offset, int limit);

    // ──────────── 纪念日 ────────────

    public Long insertCommemorationDayIdempotent(Map<String, Object> payload, String clientUuid);

    public List<PxCommemorationDay> selectCommemorationDayIncremental(String userId, String since, int offset, int limit);

    // ──────────── 记账分类（只读） ────────────

    public List<PxBookkeepingClassification> selectClassificationIncremental(String userId, String since, int offset, int limit);

    // ──────────── 记账账户（只读） ────────────

    public List<PxBookkeepingAccount> selectAccountIncremental(String userId, String since, int offset, int limit);

    // ──────────── 扩展模块（购物/菜谱/订阅/经期/预算/周期记账/读书等，只读） ────────────

    /**
     * 扩展模块增量查询（模块名→表名的白名单映射在实现层维护，防止动态表名注入）
     *
     * @param module 模块名（shoppingList/shoppingItem/recipe/mealPlan/subscription/menstruation/budget/recurring/book）
     * @param userId 用户ID
     * @param since  增量游标时间
     * @param offset 偏移
     * @param limit  条数上限
     * @return 原始行数据
     * @throws com.pnkx.common.exception.ServiceException 模块名不在白名单时
     */
    public List<Map<String, Object>> selectExtendedIncremental(String module, String userId, String since, int offset, int limit);

    // ──────────── Map → Entity 转换 ────────────
}
