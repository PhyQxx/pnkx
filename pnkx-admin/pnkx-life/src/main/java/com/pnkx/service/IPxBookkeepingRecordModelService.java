package com.pnkx.service;


import com.pnkx.domain.po.PxBookkeepingRecordModel;

import java.util.List;

/**
 * 账本记录模板Service接口
 *
 * @author pnkx
 * @date 2021-12-08
 */
public interface IPxBookkeepingRecordModelService {
    /**
     * 查询账本记录模板
     *
     * @param id 账本记录模板ID
     * @return 账本记录模板
     */
    public PxBookkeepingRecordModel selectPxBookkeepingRecordModelById(Long id);

    /**
     * 查询账本记录模板列表
     *
     * @param pxBookkeepingRecordModel 账本记录模板
     * @return 账本记录模板集合
     */
    public List<PxBookkeepingRecordModel> selectPxBookkeepingRecordModelList(PxBookkeepingRecordModel pxBookkeepingRecordModel);

    /**
     * 新增账本记录模板
     *
     * @param pxBookkeepingRecordModel 账本记录模板
     * @return 结果
     */
    public int insertPxBookkeepingRecordModel(PxBookkeepingRecordModel pxBookkeepingRecordModel);

    /**
     * 修改账本记录模板
     *
     * @param pxBookkeepingRecordModel 账本记录模板
     * @return 结果
     */
    public int updatePxBookkeepingRecordModel(PxBookkeepingRecordModel pxBookkeepingRecordModel);

    /**
     * 批量删除账本记录模板
     *
     * @param ids 需要删除的账本记录模板ID
     * @return 结果
     */
    public int deletePxBookkeepingRecordModelByIds(Long[] ids);

    /**
     * 删除账本记录模板信息
     *
     * @param id 账本记录模板ID
     * @return 结果
     */
    public int deletePxBookkeepingRecordModelById(Long id);
}
