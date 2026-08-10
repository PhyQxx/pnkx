package com.pnkx.service;

import com.pnkx.domain.po.PxMenstruationRecord;

import java.util.List;

/**
 * 姨妈记录Service接口
 *
 * @author pnkx
 * @date 2021-12-03
 */
public interface IPxMenstruationRecordService {
    /**
     * 查询姨妈记录
     *
     * @param id 姨妈记录ID
     * @return 姨妈记录
     */
    public PxMenstruationRecord selectPxMenstruationRecordById(Long id);

    /**
     * 查询姨妈记录列表
     *
     * @param pxMenstruationRecord 姨妈记录
     * @return 姨妈记录集合
     */
    public List<PxMenstruationRecord> selectPxMenstruationRecordList(PxMenstruationRecord pxMenstruationRecord);

    /**
     * 查询姨妈记录列表
     *
     * @param pxMenstruationRecord 姨妈记录
     * @return 姨妈记录集合
     */
    public List<PxMenstruationRecord> getPxMenstruationRecordList(PxMenstruationRecord pxMenstruationRecord);

    /**
     * 新增姨妈记录
     *
     * @param pxMenstruationRecord 姨妈记录
     * @return 结果
     */
    public int insertPxMenstruationRecord(PxMenstruationRecord pxMenstruationRecord);

    /**
     * 修改姨妈记录
     *
     * @param pxMenstruationRecord 姨妈记录
     * @return 结果
     */
    public int updatePxMenstruationRecord(PxMenstruationRecord pxMenstruationRecord);

    /**
     * 批量删除姨妈记录
     *
     * @param ids 需要删除的姨妈记录ID
     * @return 结果
     */
    public int deletePxMenstruationRecordByIds(Long[] ids);

    /**
     * 删除姨妈记录信息
     *
     * @param id 姨妈记录ID
     * @return 结果
     */
    public int deletePxMenstruationRecordById(Long id);

    /**
     * APP首页获取姨妈提醒列表
     * @param pxMenstruationRecord
     * @return
     */
    List<PxMenstruationRecord> selectMenstruationRecordList(PxMenstruationRecord pxMenstruationRecord);

    /**
     * 获取最后一次姨妈开始的记录
     * @return
     */
    PxMenstruationRecord getLastStartDate();
}
