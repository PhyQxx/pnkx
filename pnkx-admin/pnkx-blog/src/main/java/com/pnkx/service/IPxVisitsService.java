package com.pnkx.service;

import com.pnkx.domain.po.PxVisits;

import java.util.List;

/**
 * 访客Service接口
 *
 * @author phy
 * @date 2021-10-30
 */
public interface IPxVisitsService {
    /**
     * 查询访客
     *
     * @param id 访客ID
     * @return 访客
     */
    PxVisits selectPxVisitsById(Long id);

    /**
     * 查询访客列表
     *
     * @param pxVisits 访客
     * @return 访客集合
     */
    List<PxVisits> selectPxVisitsList(PxVisits pxVisits);

    /**
     * 新增访客
     *
     * @param pxVisits 访客
     * @return 结果
     */
    int insertPxVisits(PxVisits pxVisits);

    /**
     * 修改访客
     *
     * @param pxVisits 访客
     * @return 结果
     */
    int updatePxVisits(PxVisits pxVisits);

    /**
     * 批量删除访客
     *
     * @param ids 需要删除的访客ID
     * @return 结果
     */
    int deletePxVisitsByIds(Long[] ids);

    /**
     * 删除访客信息
     *
     * @param id 访客ID
     * @return 结果
     */
    int deletePxVisitsById(Long id);
}
