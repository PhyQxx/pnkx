package com.pnkx.service;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.domain.po.PxBookkeepingRecord;

import java.util.List;

/**
 * @author PHY
 * @classname IPxBookkeepingRecordService
 * @data 2021/11/18 0018 14:35
 * @description 描述
 */
public interface IPxBookkeepingRecordService {
    /**
     * 查询账本记录
     *
     * @param id 账本记录ID
     * @return 账本记录
     */
    public PxBookkeepingRecord selectPxBookkeepingRecordById(Long id);

    /**
     * 查询账本记录列表（分页，依赖 ThreadLocal 拦截器分页）
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 分页数据
     */
    public TableDataInfo selectPxBookkeepingRecordList(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 查询账本记录列表（不分页，供全局检索等场景使用）
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 账本记录集合
     */
    public List<PxBookkeepingRecord> selectPxBookkeepingRecordAll(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 新增账本记录
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 结果
     */
    public int insertPxBookkeepingRecord(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 批量新增账本记录
     *
     * @param list 账本记录列表
     * @return 结果
     */
    public int insertBatchRecord(List<PxBookkeepingRecord> list);

    /**
     * 修改账本记录
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 结果
     */
    public int updatePxBookkeepingRecord(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 批量删除账本记录
     *
     * @param ids 需要删除的账本记录ID
     * @return 结果
     */
    public int deletePxBookkeepingRecordByIds(Long[] ids);

    /**
     * 删除账本记录信息
     *
     * @param id 账本记录ID
     * @return 结果
     */
    public int deletePxBookkeepingRecordById(Long id);

    /**
     * AI分析记账
     * @return 结果
     */
    JSONObject aiAnalysis();

    /**
     * AI分析记账（带开关）
     * @param isAll 是否分析所有人的记录
     * @return 结果
     */
    JSONObject aiAnalysis(Boolean isAll);

    /**
     * AI账单分析（流式思考过程）
     * @param onChunk 每当有思考内容更新时回调
     * @param onError 发生错误时的回调
     */
    void aiAnalysisStream(java.util.function.Consumer<String> onChunk, Runnable onError);

    /**
     * AI账单分析（流式思考过程，带开关）
     * @param isAll 是否分析所有人的记录
     * @param onChunk 每当有思考内容更新时回调
     * @param onError 发生错误时的回调
     */
    void aiAnalysisStream(Boolean isAll, java.util.function.Consumer<String> onChunk, Runnable onError);

    /**
     * AI解析自然语言为记账数据
     * @param text 用户输入的自然语言
     * @return 解析后的记账数据JSON
     */
    JSONObject aiParse(String text);

    /**
     * AI批量解析自然语言为多条记账数据
     * @param text 用户输入的多行自然语言
     * @return 解析后的记账数据列表JSON
     */
    JSONObject aiBatchParse(String text);

    /**
     * AI解析自然语言（流式思考过程）
     * @param text 用户输入的自然语言
     * @param onChunk 每当有思考内容更新时回调
     */
    void aiParseStream(String text, java.util.function.Consumer<String> onChunk, Runnable onError);
}
