package com.pnkx.mapper;

import com.pnkx.domain.po.PxLoversCard;

import java.util.List;

/**
 * 情侣卡券Mapper接口
 *
 * @author pnkx
 * @date 2022-05-21
 */
public interface PxLoversCardMapper {
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
     * 删除情侣卡券
     *
     * @param id 情侣卡券ID
     * @return 结果
     */
    public int deletePxLoversCardById(Long id);

    /**
     * 批量删除情侣卡券
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxLoversCardByIds(Long[] ids);
    }
