package com.pnkx.service.impl;

import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.*;
import com.pnkx.mapper.*;
import com.pnkx.service.IPxOfflineSyncService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
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
public class PxOfflineSyncServiceImpl implements IPxOfflineSyncService {

    private static final Logger logger = LoggerFactory.getLogger(PxOfflineSyncServiceImpl.class);

    @Resource
    private PxDiaryMapper diaryMapper;

    @Resource
    private PxToDoMapper toDoMapper;

    @Resource
    private PxBookkeepingRecordMapper bookkeepingRecordMapper;

    @Resource
    private PxNoteMapper noteMapper;

    @Resource
    private PxCommemorationDayMapper commemorationDayMapper;

    @Resource
    private PxBookkeepingClassificationMapper bookkeepingClassificationMapper;

    @Resource
    private PxBookkeepingAccountMapper bookkeepingAccountMapper;

    // ──────────── 日记 ────────────

    /**
     * 日记幂等新增
     * @return 插入后的 ID（已存在则返回已有 ID）
     */
    @Override
    public Long insertDiaryIdempotent(Map<String, Object> payload, String clientUuid) {
        PxDiary existing = diaryMapper.selectByClientUuid(clientUuid);
        if (existing != null) {
            logger.debug("日记 clientUuid={} 已存在, id={}", clientUuid, existing.getId());
            return existing.getId();
        }
        PxDiary diary = mapToDiary(payload);
        diary.setClientUuid(clientUuid);
        diary.setCreateBy(SecurityUtils.getUserId());
        diary.setCreateTime(new Date());
        diaryMapper.insertPxDiary(diary);
        return diary.getId();
    }

    /**
     * 日记增量查询
     */
    @Override
    public List<PxDiary> selectDiaryIncremental(String userId, String since, int offset, int limit) {
        return diaryMapper.selectIncremental(userId, since, offset, limit);
    }

    // ──────────── 待办 ────────────

    @Override
    public Long insertToDoIdempotent(Map<String, Object> payload, String clientUuid) {
        PxToDo existing = toDoMapper.selectByClientUuid(clientUuid);
        if (existing != null) {
            return existing.getId();
        }
        PxToDo todo = mapToTodo(payload);
        todo.setClientUuid(clientUuid);
        todo.setCreateBy(SecurityUtils.getUserId());
        todo.setCreateTime(new Date());
        toDoMapper.insertPxToDo(todo);
        return todo.getId();
    }

    @Override
    public List<PxToDo> selectToDoIncremental(String userId, String since, int offset, int limit) {
        return toDoMapper.selectIncremental(userId, since, offset, limit);
    }

    // ──────────── 记账 ────────────

    @Override
    public Long insertBookkeepingRecordIdempotent(Map<String, Object> payload, String clientUuid) {
        PxBookkeepingRecord existing = bookkeepingRecordMapper.selectByClientUuid(clientUuid);
        if (existing != null) {
            return existing.getId();
        }
        PxBookkeepingRecord record = mapToBookkeepingRecord(payload);
        record.setClientUuid(clientUuid);
        record.setCreateBy(SecurityUtils.getUserId());
        record.setCreateTime(new Date());
        bookkeepingRecordMapper.insertPxBookkeepingRecord(record);
        return record.getId();
    }

    @Override
    public List<PxBookkeepingRecord> selectRecordIncremental(String userId, String since, int offset, int limit) {
        return bookkeepingRecordMapper.selectIncremental(userId, since, offset, limit);
    }

    // ──────────── 笔记 ────────────

    @Override
    public Long insertNoteIdempotent(Map<String, Object> payload, String clientUuid) {
        PxNote existing = noteMapper.selectByClientUuid(clientUuid);
        if (existing != null) {
            return existing.getId();
        }
        PxNote note = mapToNote(payload);
        note.setClientUuid(clientUuid);
        note.setCreateBy(SecurityUtils.getUserId());
        note.setCreateTime(new Date());
        noteMapper.insertPxNote(note);
        return note.getId();
    }

    @Override
    public List<PxNote> selectNoteIncremental(String userId, String since, int offset, int limit) {
        return noteMapper.selectIncremental(userId, since, offset, limit);
    }

    // ──────────── 纪念日 ────────────

    @Override
    public Long insertCommemorationDayIdempotent(Map<String, Object> payload, String clientUuid) {
        PxCommemorationDay existing = commemorationDayMapper.selectByClientUuid(clientUuid);
        if (existing != null) {
            return existing.getId();
        }
        PxCommemorationDay day = mapToCommemorationDay(payload);
        day.setClientUuid(clientUuid);
        day.setCreateBy(SecurityUtils.getUserId());
        day.setCreateTime(new Date());
        commemorationDayMapper.insertPxCommemorationDay(day);
        return day.getId();
    }

    @Override
    public List<PxCommemorationDay> selectCommemorationDayIncremental(String userId, String since, int offset, int limit) {
        return commemorationDayMapper.selectIncremental(userId, since, offset, limit);
    }

    // ──────────── 记账分类（只读） ────────────

    @Override
    public List<PxBookkeepingClassification> selectClassificationIncremental(String userId, String since, int offset, int limit) {
        return bookkeepingClassificationMapper.selectIncremental(userId, since, offset, limit);
    }

    // ──────────── 记账账户（只读） ────────────

    @Override
    public List<PxBookkeepingAccount> selectAccountIncremental(String userId, String since, int offset, int limit) {
        return bookkeepingAccountMapper.selectIncremental(userId, since, offset, limit);
    }

    // ──────────── Map → Entity 转换 ────────────

    private PxDiary mapToDiary(Map<String, Object> p) {
        PxDiary d = new PxDiary();
        if (p.get("title") != null) d.setTitle((String) p.get("title"));
        if (p.get("mood") != null) d.setMood((String) p.get("mood"));
        if (p.get("weather") != null) d.setWeather((String) p.get("weather"));
        if (p.get("content") != null) d.setContent((String) p.get("content"));
        if (p.get("richText") != null) d.setRichText((String) p.get("richText"));
        if (p.get("remark") != null) d.setRemark((String) p.get("remark"));
        return d;
    }

    private PxToDo mapToTodo(Map<String, Object> p) {
        PxToDo t = new PxToDo();
        if (p.get("content") != null) t.setContent((String) p.get("content"));
        if (p.get("performer") != null) t.setPerformer((String) p.get("performer"));
        if (p.get("planStartTime") != null) t.setPlanStartTime((String) p.get("planStartTime"));
        if (p.get("planEndTime") != null) t.setPlanEndTime((String) p.get("planEndTime"));
        if (p.get("status") != null) t.setStatus(Boolean.parseBoolean(p.get("status").toString()));
        if (p.get("label") != null) t.setLabel((String) p.get("label"));
        return t;
    }

    private PxBookkeepingRecord mapToBookkeepingRecord(Map<String, Object> p) {
        PxBookkeepingRecord r = new PxBookkeepingRecord();
        if (p.get("account") != null) r.setAccount(toLong(p.get("account")));
        if (p.get("otherAccount") != null) r.setOtherAccount(toLong(p.get("otherAccount")));
        if (p.get("type") != null) r.setType(toLong(p.get("type")));
        if (p.get("money") != null) r.setMoney((String) p.get("money"));
        if (p.get("payTime") != null) r.setPayTime(DateUtils.parseDate(p.get("payTime")));
        if (p.get("commemorationDayId") != null) {
            r.setCommemorationDayId(toLong(p.get("commemorationDayId")));
        }
        if (p.get("remark") != null) r.setRemark((String) p.get("remark"));
        return r;
    }

    private PxNote mapToNote(Map<String, Object> p) {
        PxNote n = new PxNote();
        if (p.get("title") != null) n.setTitle((String) p.get("title"));
        if (p.get("content") != null) n.setContent((String) p.get("content"));
        if (p.get("richText") != null) n.setRichText((String) p.get("richText"));
        if (p.get("folder") != null) n.setFolder(toLong(p.get("folder")));
        if (p.get("remark") != null) n.setRemark((String) p.get("remark"));
        return n;
    }

    private PxCommemorationDay mapToCommemorationDay(Map<String, Object> p) {
        PxCommemorationDay c = new PxCommemorationDay();
        if (p.get("name") != null) c.setName((String) p.get("name"));
        if (p.get("icon") != null) c.setIcon((String) p.get("icon"));
        if (p.get("isRepeat") != null) c.setRepeat(Boolean.parseBoolean(p.get("isRepeat").toString()));
        if (p.get("remark") != null) c.setRemark((String) p.get("remark"));
        return c;
    }

    private Long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
