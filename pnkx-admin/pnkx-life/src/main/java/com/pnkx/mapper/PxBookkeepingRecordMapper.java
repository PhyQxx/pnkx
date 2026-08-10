package com.pnkx.mapper;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.domain.po.PxBookkeepingRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author PHY
 * @classname PxBookkeepingRecordMapper
 * @data 2021/11/18 0018 14:38
 * @description 描述
 */
public interface PxBookkeepingRecordMapper {
    /**
     * 查询账本记录
     *
     * @param id 账本记录ID
     * @return 账本记录
     */
    public PxBookkeepingRecord selectPxBookkeepingRecordById(Long id);

    /**
     * 查询账本记录列表
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 账本记录集合
     */
    public List<PxBookkeepingRecord> selectPxBookkeepingRecordList(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 查询账本记录收入
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 账本记录集合
     */
    public Double getInflowMoney(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 查询账本记录支出
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 账本记录集合
     */
    public Double getFlowOutMoney(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 新增账本记录
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 结果
     */
    public int insertPxBookkeepingRecord(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 修改账本记录
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 结果
     */
    public int updatePxBookkeepingRecord(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 删除账本记录
     *
     * @param id 账本记录ID
     * @return 结果
     */
    public int deletePxBookkeepingRecordById(Long id);

    /**
     * 批量删除账本记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxBookkeepingRecordByIds(Long[] ids);

    /**
     * 批量删除账本记录
     *
     * @param types 需要删除的分类ID
     * @return 结果
     */
    public int deletePxBookkeepingRecordByTypes(Long[] types);

    /**
     * 批量删除账本记录
     *
     * @param accountId 需要删除的账户ID
     * @return 结果
     */
    public int deletePxBookkeepingRecordByAccountId(Long accountId);

    /**
     * 获取AI分析结果
     *
     * @param pxBookkeepingRecord 账本记录
     * @return 结果
     */
    List<JSONObject> getNaturalLanguageList(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 根据客户端唯一标识查询记账记录（幂等去重）
     */
    PxBookkeepingRecord selectByClientUuid(@Param("clientUuid") String clientUuid);

    /**
     * 增量查询记账记录（离线同步用）
     */
    List<PxBookkeepingRecord> selectIncremental(@Param("createBy") String createBy, @Param("since") String since, @Param("offset") int offset, @Param("limit") int limit);
}
