package com.pnkx.service;


import com.pnkx.domain.po.PxCardRecord;
import com.pnkx.domain.po.PxLoversCard;
import com.pnkx.domain.vo.PxCardRecordVo;

import java.util.List;

/**
 * 情侣卡券Service接口
 *
 * @author pnkx
 * @date 2022-05-21
 */
public interface IPxLoversCardService {
    /**
     * 查询情侣卡券
     *
     * @param id 情侣卡券ID
     * @return 情侣卡券
     */
    public PxLoversCard selectPxLoversCardById(Long id);

    /**
     * 查询情侣卡券列表
     *
     * @param pxLoversCard 情侣卡券
     * @return 情侣卡券集合
     */
    public List<PxLoversCard> selectPxLoversCardList(PxLoversCard pxLoversCard);

    /**
     * 查询情侣卡使用记录
     *
     * @param id 情侣卡使用记录ID
     * @return 情侣卡使用记录
     */
    public PxCardRecordVo selectPxCardRecordById(Long id);
    /**
     * 查询情侣卡券使用记录列表
     *
     * @param pxCardRecordVo 情侣卡券使用记录
     * @return 情侣卡券使用记录集合
     */
    public List<PxCardRecordVo> selectPxLoversCardRecordList(PxCardRecordVo pxCardRecordVo);

    /**
     * 新增情侣卡券
     *
     * @param pxLoversCard 情侣卡券
     * @return 结果
     */
    public int insertPxLoversCard(PxLoversCard pxLoversCard);

    /**
     * 修改情侣卡券
     *
     * @param pxLoversCard 情侣卡券
     * @return 结果
     */
    public int updatePxLoversCard(PxLoversCard pxLoversCard);

    /**
     * 批量删除情侣卡券
     *
     * @param ids 需要删除的情侣卡券ID
     * @return 结果
     */
    public int deletePxLoversCardByIds(Long[] ids);

    /**
     * 删除情侣卡券信息
     *
     * @param id 情侣卡券ID
     * @return 结果
     */
    public int deletePxLoversCardById(Long id);

    /**
     * 获取当前人的卡券
     * @return
     */
    List<PxLoversCard> getCardByUserId();

    /**
     * 使用卡券
     * @param pxCardRecord
     * @return
     */
    int useCard(PxCardRecord pxCardRecord);

    /**
     * 卡券确认使用
     * @param pxCardRecord
     * @return
     */
    int confirmCard(PxCardRecord pxCardRecord);

    /**
     * 使用卡券评分
     * @param pxCardRecord
     * @return
     */
    int scoreCard(PxCardRecord pxCardRecord);

    /**
     * 获取待处理的卡券
     * @return
     */
    List<PxCardRecordVo> getToDoCard();
}
