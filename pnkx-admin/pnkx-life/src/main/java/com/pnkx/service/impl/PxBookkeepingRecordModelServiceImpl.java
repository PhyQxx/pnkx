package com.pnkx.service.impl;


import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.ServletUtils;
import com.pnkx.domain.po.PxBookkeepingRecordModel;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.mapper.PxBookkeepingRecordModelMapper;
import com.pnkx.service.IPxBookkeepingRecordModelService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 账本记录模板Service业务层处理
 *
 * @author pnkx
 * @date 2021-12-08
 */
@Service
public class PxBookkeepingRecordModelServiceImpl implements IPxBookkeepingRecordModelService {
    @Resource
    private PxBookkeepingRecordModelMapper pxBookkeepingRecordModelMapper;

    /**
     * 查询账本记录模板
     *
     * @param id 账本记录模板ID
     * @return 账本记录模板
     */
    @Override
    public PxBookkeepingRecordModel selectPxBookkeepingRecordModelById(Long id) {
        return pxBookkeepingRecordModelMapper.selectPxBookkeepingRecordModelById(id);
    }

    /**
     * 查询账本记录模板列表
     *
     * @param pxBookkeepingRecordModel 账本记录模板
     * @return 账本记录模板
     */
    @Override
    @DataScopeSelf(alias = "r")
    public List<PxBookkeepingRecordModel> selectPxBookkeepingRecordModelList(PxBookkeepingRecordModel pxBookkeepingRecordModel) {
        return pxBookkeepingRecordModelMapper.selectPxBookkeepingRecordModelList(pxBookkeepingRecordModel);
    }


    /**
     * 新增账本记录模板
     *
     * @param pxBookkeepingRecordModel 账本记录模板
     * @return 结果
     */
    @Override
    public int insertPxBookkeepingRecordModel(PxBookkeepingRecordModel pxBookkeepingRecordModel) {
        pxBookkeepingRecordModel.setCreateTime(DateUtils.getNowDate());
        pxBookkeepingRecordModel.setCreateBy(SecurityUtils.getUserId());
        return pxBookkeepingRecordModelMapper.insertPxBookkeepingRecordModel(pxBookkeepingRecordModel);
    }

    /**
     * 修改账本记录模板
     *
     * @param pxBookkeepingRecordModel 账本记录模板
     * @return 结果
     */
    @Override
    public int updatePxBookkeepingRecordModel(PxBookkeepingRecordModel pxBookkeepingRecordModel) {
        pxBookkeepingRecordModel.setUpdateTime(DateUtils.getNowDate());
        pxBookkeepingRecordModel.setCreateBy(SecurityUtils.getUserId());
        return pxBookkeepingRecordModelMapper.updatePxBookkeepingRecordModel(pxBookkeepingRecordModel);
    }

    /**
     * 批量删除账本记录模板
     *
     * @param ids 需要删除的账本记录模板ID
     * @return 结果
     */
    @Override
    public int deletePxBookkeepingRecordModelByIds(Long[] ids) {
        return pxBookkeepingRecordModelMapper.deletePxBookkeepingRecordModelByIds(ids);
    }

    /**
     * 删除账本记录模板信息
     *
     * @param id 账本记录模板ID
     * @return 结果
     */
    @Override
    public int deletePxBookkeepingRecordModelById(Long id) {
        return pxBookkeepingRecordModelMapper.deletePxBookkeepingRecordModelById(id);
    }
}
