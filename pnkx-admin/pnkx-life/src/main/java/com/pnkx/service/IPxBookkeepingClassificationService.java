package com.pnkx.service;


import com.pnkx.domain.po.PxBookkeepingClassification;

import java.util.List;

public interface IPxBookkeepingClassificationService {
    /**
     * 查询账本分类
     *
     * @param id 账本分类ID
     * @return 账本分类
     */
    public PxBookkeepingClassification selectPxBookkeepingClassificationById(Long id);

    /**
     * 查询账本分类列表
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 账本分类集合
     */
    public List<PxBookkeepingClassification> selectPxBookkeepingClassificationList(PxBookkeepingClassification pxBookkeepingClassification);

    /**
     * 新增账本分类
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 结果
     */
    public int insertPxBookkeepingClassification(PxBookkeepingClassification pxBookkeepingClassification);

    /**
     * 修改账本分类
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 结果
     */
    public int updatePxBookkeepingClassification(PxBookkeepingClassification pxBookkeepingClassification);

    /**
     * 批量删除账本分类
     *
     * @param ids 需要删除的账本分类ID
     * @return 结果
     */
    public int deletePxBookkeepingClassificationByIds(Long[] ids);

    /**
     * 删除账本分类信息
     *
     * @param id 账本分类ID
     * @return 结果
     */
    public int deletePxBookkeepingClassificationById(Long id);

    /**
     * 查询最近使用分类列表
     *
     * @param pxBookkeepingClassification 分类
     * @return 分类列表
     */
    List<PxBookkeepingClassification> getLatelyTypeList(PxBookkeepingClassification pxBookkeepingClassification);
}
