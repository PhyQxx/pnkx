package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.constant.UserConstants;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxCardRecord;
import com.pnkx.domain.po.PxCardUser;
import com.pnkx.domain.po.PxLoversCard;
import com.pnkx.domain.vo.PxCardRecordVo;
import com.pnkx.mapper.PxCardRecordMapper;
import com.pnkx.mapper.PxCardUserMapper;
import com.pnkx.mapper.PxLoversCardMapper;
import com.pnkx.service.IPxLoversCardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 情侣卡券Service业务层处理
 *
 * @author pnkx
 * @date 2022-05-21
 */
@Service
public class PxLoversCardServiceImpl implements IPxLoversCardService {
    @Resource
    private PxLoversCardMapper pxLoversCardMapper;
    @Resource
    private PxCardUserMapper pxCardUserMapper;
    @Resource
    private PxCardRecordMapper pxCardRecordMapper;

    /**
     * 查询情侣卡券
     *
     * @param id 情侣卡券ID
     * @return 情侣卡券
     */
    @Override
    public PxLoversCard selectPxLoversCardById(Long id) {
        return pxLoversCardMapper.selectPxLoversCardById(id);
    }

    /**
     * 查询情侣卡券列表
     *
     * @param pxLoversCard 情侣卡券
     * @return 情侣卡券
     */
    @Override
    @DataScopeSelf
    public List<PxLoversCard> selectPxLoversCardList(PxLoversCard pxLoversCard) {
        return pxLoversCardMapper.selectPxLoversCardList(pxLoversCard);
    }


    /**
     * 查询情侣卡使用记录
     *
     * @param id 情侣卡使用记录ID
     * @return 情侣卡使用记录
     */
    @Override
    public PxCardRecordVo selectPxCardRecordById(Long id) {
        return pxCardRecordMapper.selectPxCardRecordById(id);
    }

    /**
     * 查询情侣卡券使用记录列表
     *
     * @param pxCardRecordVo 情侣卡券使用记录
     * @return 情侣卡券使用记录
     */
    @DataScopeSelf(alias = "r")
    @Override
    public List<PxCardRecordVo> selectPxLoversCardRecordList(PxCardRecordVo pxCardRecordVo) {
        return pxCardRecordMapper.selectPxCardRecordList(pxCardRecordVo);
    }


    /**
     * 新增情侣卡券
     *
     * @param pxLoversCard 情侣卡券
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int insertPxLoversCard(PxLoversCard pxLoversCard) {
        int result = pxLoversCardMapper.insertPxLoversCard(pxLoversCard);
        pxLoversCard.setCreateBy(SecurityUtils.getUserId());
        pxLoversCard.setCreateTime(DateUtils.getNowDate());
        // 新增男关联
        PxCardUser pxCardUserMan = new PxCardUser();
        pxCardUserMan.setCardId(pxLoversCard.getId());
        pxCardUserMan.setUserId(UserConstants.MAN_USER_ID);
        pxCardUserMan.setCardNumber(pxLoversCard.getNumber());
        pxCardUserMapper.insertPxCardUser(pxCardUserMan);
        // 新增女关联
        PxCardUser pxCardUserWoman = new PxCardUser();
        pxCardUserWoman.setCardId(pxLoversCard.getId());
        pxCardUserWoman.setUserId(UserConstants.WOMAN_USER_ID);
        pxCardUserWoman.setCardNumber(pxLoversCard.getNumber());
        pxCardUserMapper.insertPxCardUser(pxCardUserWoman);
        return result;
    }

    /**
     * 修改情侣卡券
     *
     * @param pxLoversCard 情侣卡券
     * @return 结果
     */
    @Override
    public int updatePxLoversCard(PxLoversCard pxLoversCard) {
        pxLoversCard.setUpdateBy(SecurityUtils.getUserName());
        pxLoversCard.setUpdateTime(DateUtils.getNowDate());
        return pxLoversCardMapper.updatePxLoversCard(pxLoversCard);
    }

    /**
     * 批量删除情侣卡券
     *
     * @param ids 需要删除的情侣卡券ID
     * @return 结果
     */
    @Override
    public int deletePxLoversCardByIds(Long[] ids) {
        return pxLoversCardMapper.deletePxLoversCardByIds(ids);
    }

    /**
     * 删除情侣卡券信息
     *
     * @param id 情侣卡券ID
     * @return 结果
     */
    @Override
    public int deletePxLoversCardById(Long id) {
        return pxLoversCardMapper.deletePxLoversCardById(id);
    }

    /**
     * 获取当前人的卡券
     * @return
     */
    @Override
    public List<PxLoversCard> getCardByUserId() {
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        return pxCardUserMapper.getCardByUserId(userId);
    }

    /**
     * 使用卡券
     * @param pxCardRecord
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int useCard(PxCardRecord pxCardRecord) {
        pxCardRecord.setUserId(SecurityUtils.getLoginUser().getUser().getUserId());
        pxCardRecord.setCreateBy(SecurityUtils.getUserId());
        pxCardRecord.setCreateTime(DateUtils.getNowDate());
        pxCardRecordMapper.insertPxCardRecord(pxCardRecord);
        return pxCardUserMapper.useCard(pxCardRecord);
    }

    /**
     * 卡券确认使用
     * @param pxCardRecord
     * @return
     */
    @Override
    public int confirmCard(PxCardRecord pxCardRecord) {
        pxCardRecord.setConfirm(true);
        pxCardRecord.setConfirmTime(DateUtils.getNowDate());
        return pxCardRecordMapper.updatePxCardRecord(pxCardRecord);
    }

    /**
     * 使用卡券评分
     * @param pxCardRecord
     * @return
     */
    @Override
    public int scoreCard(PxCardRecord pxCardRecord) {
        pxCardRecord.setScoreTime(DateUtils.getNowDate());
        return pxCardRecordMapper.updatePxCardRecord(pxCardRecord);
    }

    /**
     * 获取待处理的卡券
     * @return
     */
    @Override
    public List<PxCardRecordVo> getToDoCard() {
        PxCardRecordVo pxCardRecordVo = new PxCardRecordVo();
        pxCardRecordVo.setUserId(SecurityUtils.getLoginUser().getUser().getUserId());
        return pxCardRecordMapper.getToDoCard(pxCardRecordVo);
    }
}
