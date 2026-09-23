package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.constant.HttpStatus;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxBookkeepingClassification;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.domain.po.PxCommemorationDay;
import com.pnkx.mapper.PxBookkeepingClassificationMapper;
import com.pnkx.mapper.PxBookkeepingRecordMapper;
import com.pnkx.service.IPxBookkeepingRecordService;
import com.pnkx.service.IPxCommemorationDayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author PHY
 * @classname PxBookkeepingRecordServiceImpl
 * @data 2021/11/18 0018 14:36
 * @description 描述
 */
@Service
public class PxBookkeepingRecordServiceImpl implements IPxBookkeepingRecordService {
    private static final Logger logger = LoggerFactory.getLogger(PxBookkeepingRecordServiceImpl.class);
    @Resource
    private PxBookkeepingRecordMapper pxBookkeepingRecordMapper;
    @Resource
    private PxBookkeepingClassificationMapper classificationMapper;
    @Resource
    private PxBookkeepingAiService aiService;
    @Resource
    private PxBookkeepingBudgetServiceImpl budgetService;
    @Resource
    private IPxCommemorationDayService commemorationDayService;

    /**
     * 礼物类支出自动关联纪念日的匹配窗口（天数）
     */
    private static final long COMMEMORATION_MATCH_WINDOW_DAYS = 30L;

    @Override
    public PxBookkeepingRecord selectPxBookkeepingRecordById(Long id) {
        return pxBookkeepingRecordMapper.selectPxBookkeepingRecordById(id);
    }

    @Override
    public int insertPxBookkeepingRecord(PxBookkeepingRecord pxBookkeepingRecord) {
        pxBookkeepingRecord.setCreateTime(DateUtils.getNowDate());
        pxBookkeepingRecord.setCreateBy(SecurityUtils.getUserId());
        // 礼物类支出联动纪念日：用户未手动指定时，自动匹配消费时间附近最近的纪念日
        autoMatchCommemorationDay(pxBookkeepingRecord);
        int rows = pxBookkeepingRecordMapper.insertPxBookkeepingRecord(pxBookkeepingRecord);
        // 预算超支/接近超支实时提醒（同步执行内部已全量 try-catch，绝不影响记账）
        if (rows > 0) {
            budgetService.checkBudgetAlertAfterRecord(pxBookkeepingRecord);
        }
        return rows;
    }

    /**
     * 礼物类支出自动关联纪念日。
     * 仅当用户未手动指定 commemorationDayId，且消费分类名含"礼物"时触发：
     * 在消费时间前后 {@value #COMMEMORATION_MATCH_WINDOW_DAYS} 天内，选取日期最近的纪念日关联。
     *
     * @param record 记账记录
     */
    private void autoMatchCommemorationDay(PxBookkeepingRecord record) {
        PxBookkeepingClassification classification =
                record.getType() == null ? null
                        : classificationMapper.selectPxBookkeepingClassificationById(record.getType());
        autoMatchCommemorationDay(record, classification,
                commemorationDayService.getCommemorationDayList(new PxCommemorationDay()));
    }

    /**
     * 同上，但分类与纪念日列表由调用方预加载传入，避免批量导入时逐条查库（N+1）
     *
     * @param record         记账记录
     * @param classification record.type 对应的分类（可为 null）
     * @param days           纪念日全量列表（可为 null 表示无数据）
     */
    private void autoMatchCommemorationDay(PxBookkeepingRecord record,
                                           PxBookkeepingClassification classification,
                                           List<PxCommemorationDay> days) {
        if (record.getCommemorationDayId() != null) {
            // 用户已手动指定，尊重用户选择
            return;
        }
        if (record.getType() == null || record.getPayTime() == null) {
            return;
        }
        // 判断分类名是否含"礼物"
        if (classification == null || classification.getTypeName() == null
                || !classification.getTypeName().contains("礼物")) {
            return;
        }
        // 查找消费时间附近最近的纪念日
        if (days == null || days.isEmpty()) {
            return;
        }
        Instant payInstant = record.getPayTime().toInstant();
        PxCommemorationDay nearest = null;
        long minDiff = Long.MAX_VALUE;
        for (PxCommemorationDay day : days) {
            if (day.getDate() == null || Boolean.TRUE.equals(day.getDelFlag())) {
                continue;
            }
            // 重复型纪念日取今年的日期比较
            Instant dayInstant = day.getDate().toInstant();
            if (Boolean.TRUE.equals(day.getRepeat())) {
                dayInstant = dayInstant.atZone(ZoneId.systemDefault())
                        .withYear(record.getPayTime().toInstant().atZone(ZoneId.systemDefault()).getYear())
                        .toInstant();
            }
            long diff = Math.abs(Duration.between(payInstant, dayInstant).toDays());
            if (diff <= COMMEMORATION_MATCH_WINDOW_DAYS && diff < minDiff) {
                minDiff = diff;
                nearest = day;
            }
        }
        if (nearest != null) {
            record.setCommemorationDayId(nearest.getId());
            logger.info("记账联动纪念日：支出自动关联「{}」（距离 {} 天）", nearest.getName(), minDiff);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBatchRecord(List<PxBookkeepingRecord> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        String userId = SecurityUtils.getUserId();
        // 预加载分类与纪念日，避免逐条查库（原实现每条记录 3 次查询）
        Map<Long, PxBookkeepingClassification> classificationMap = classificationMapper
                .selectPxBookkeepingClassificationList(new PxBookkeepingClassification())
                .stream()
                .collect(Collectors.toMap(PxBookkeepingClassification::getId, c -> c, (a, b) -> a));
        List<PxCommemorationDay> days = commemorationDayService.getCommemorationDayList(new PxCommemorationDay());
        int rows = 0;
        for (PxBookkeepingRecord record : list) {
            record.setCreateTime(now);
            record.setCreateBy(userId);
            autoMatchCommemorationDay(record, classificationMap.get(record.getType()), days);
            rows += pxBookkeepingRecordMapper.insertPxBookkeepingRecord(record);
        }
        return rows;
    }

    @DataScopeSelf(alias = "r")
    @Override
    public TableDataInfo selectPxBookkeepingRecordList(PxBookkeepingRecord pxBookkeepingRecord) {
        List<PxBookkeepingRecord> list = pxBookkeepingRecordMapper.selectPxBookkeepingRecordList(pxBookkeepingRecord);
        // 主列表查询完成后立即保存分页总数并清理 ThreadLocal。后面的收支合计也是
        // SELECT；若继续携带分页上下文，分页拦截器会用聚合查询的 1 行结果覆盖 total。
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PxBookkeepingRecord> page =
                com.pnkx.common.core.controller.BaseController.getPage();
        long total = page != null ? page.getTotal() : list.size();
        com.pnkx.common.core.controller.BaseController.clearPage();
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg(pxBookkeepingRecordMapper.getInflowMoney(pxBookkeepingRecord) + "," + pxBookkeepingRecordMapper.getFlowOutMoney(pxBookkeepingRecord));
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }

    @DataScopeSelf(alias = "r")
    @Override
    public List<PxBookkeepingRecord> selectPxBookkeepingRecordAll(PxBookkeepingRecord pxBookkeepingRecord) {
        return pxBookkeepingRecordMapper.selectPxBookkeepingRecordList(pxBookkeepingRecord);
    }

    @Override
    public int updatePxBookkeepingRecord(PxBookkeepingRecord pxBookkeepingRecord) {
        pxBookkeepingRecord.setUpdateTime(DateUtils.getNowDate());
        pxBookkeepingRecord.setUpdateBy(SecurityUtils.getUserId());
        return pxBookkeepingRecordMapper.updatePxBookkeepingRecord(pxBookkeepingRecord);
    }

    @Override
    public int deletePxBookkeepingRecordByIds(Long[] ids) {
        return pxBookkeepingRecordMapper.deletePxBookkeepingRecordByIds(ids);
    }

    @Override
    public int deletePxBookkeepingRecordById(Long id) {
        return pxBookkeepingRecordMapper.deletePxBookkeepingRecordById(id);
    }

    @Override
    public JSONObject aiAnalysis() {
        return aiService.aiAnalysis(false);
    }

    @Override
    public JSONObject aiAnalysis(Boolean isAll) {
        return aiService.aiAnalysis(isAll);
    }

    @Override
    public void aiAnalysisStream(java.util.function.Consumer<String> onChunk, Runnable onError) {
        aiService.aiAnalysisStream(false, onChunk, onError);
    }

    @Override
    public void aiAnalysisStream(Boolean isAll, java.util.function.Consumer<String> onChunk, Runnable onError) {
        aiService.aiAnalysisStream(isAll, onChunk, onError);
    }

    @Override
    public JSONObject aiParse(String text) {
        return aiService.aiParse(text);
    }

    @Override
    public void aiParseStream(String text, java.util.function.Consumer<String> onChunk, Runnable onError) {
        aiService.aiParseStream(text, onChunk, onError);
    }

    @Override
    public JSONObject aiBatchParse(String text) {
        return aiService.aiBatchParse(text);
    }
}
