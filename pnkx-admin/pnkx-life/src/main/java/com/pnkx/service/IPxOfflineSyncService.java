package com.pnkx.service;

import com.pnkx.domain.po.*;
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

    // ──────────── Map → Entity 转换 ────────────
}
