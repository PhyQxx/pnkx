package com.pnkx.mapper;

import com.pnkx.domain.po.PxCardRecord;
import com.pnkx.domain.po.PxCardUser;
import com.pnkx.domain.po.PxLoversCard;

import java.util.List;

/**
 * 情侣卡关联人员Mapper接口
 *
 * @author pnkx
 * @date 2022-05-22
 */
public interface PxCardUserMapper {
    /**
     * 查询情侣卡关联人员
     *
     * @param id 情侣卡关联人员ID
     * @return 情侣卡关联人员
     */
    public PxCardUser selectPxCardUserById(Long id);

    /**
     * 查询情侣卡关联人员列表
     *
     * @param pxCardUser 情侣卡关联人员
     * @return 情侣卡关联人员集合
     */
    public List<PxCardUser> selectPxCardUserList(PxCardUser pxCardUser);

    /**
     * 新增情侣卡关联人员
     *
     * @param pxCardUser 情侣卡关联人员
     * @return 结果
     */
    public int insertPxCardUser(PxCardUser pxCardUser);

    /**
     * 修改情侣卡关联人员
     *
     * @param pxCardUser 情侣卡关联人员
     * @return 结果
     */
    public int updatePxCardUser(PxCardUser pxCardUser);

    /**
     * 删除情侣卡关联人员
     *
     * @param id 情侣卡关联人员ID
     * @return 结果
     */
    public int deletePxCardUserById(Long id);

    /**
     * 批量删除情侣卡关联人员
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxCardUserByIds(Long[] ids);

    /**
     * 获取当前人的卡券
     * @param userId
     * @return
     */
    List<PxLoversCard> getCardByUserId(Long userId);

    /**
     * 使用卡券
     * @param pxCardRecord
     * @return
     */
    int useCard(PxCardRecord pxCardRecord);

    /**
     * 定期发放卡券
     * @return 结果
     */
    public int regularGrantCard();
}
