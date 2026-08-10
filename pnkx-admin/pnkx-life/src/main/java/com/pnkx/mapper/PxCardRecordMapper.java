package com.pnkx.mapper;


import com.pnkx.domain.po.PxCardRecord;
import com.pnkx.domain.vo.PxCardRecordVo;

import java.util.List;

/**
 * 情侣卡使用记录Mapper接口
 *
 * @author pnkx
 * @date 2022-05-22
 */
public interface PxCardRecordMapper {
    /**
     * 查询情侣卡使用记录
     *
     * @param id 情侣卡使用记录ID
     * @return 情侣卡使用记录
     */
    public PxCardRecordVo selectPxCardRecordById(Long id);

    /**
     * 查询情侣卡使用记录列表
     *
     * @param pxCardRecordVo 情侣卡使用记录
     * @return 情侣卡使用记录集合
     */
    public List<PxCardRecordVo> selectPxCardRecordList(PxCardRecordVo pxCardRecordVo);

    /**
     * 新增情侣卡使用记录
     *
     * @param pxCardRecord 情侣卡使用记录
     * @return 结果
     */
    public int insertPxCardRecord(PxCardRecord pxCardRecord);

    /**
     * 修改情侣卡使用记录
     *
     * @param pxCardRecord 情侣卡使用记录
     * @return 结果
     */
    public int updatePxCardRecord(PxCardRecord pxCardRecord);

    /**
     * 删除情侣卡使用记录
     *
     * @param id 情侣卡使用记录ID
     * @return 结果
     */
    public int deletePxCardRecordById(Long id);

    /**
     * 批量删除情侣卡使用记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxCardRecordByIds(Long[] ids);

    /**
     * 获取待处理的卡券
     * @param pxCardRecordVo
     * @return
     */
    List<PxCardRecordVo> getToDoCard(PxCardRecordVo pxCardRecordVo);
}
