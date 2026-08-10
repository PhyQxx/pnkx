package com.pnkx.mapper;

import com.pnkx.domain.po.PxVisits;

import java.util.List;

/**
 * 访客Mapper接口
 *
 * @author phy
 * @date 2021-10-30
 */
public interface PxVisitsMapper {
    /**
     * 查询访客
     *
     * @param id 访客ID
     * @return 访客
     */
    public PxVisits selectPxVisitsById(Long id);

    /**
     * 查询访客列表
     *
     * @param pxVisits 访客
     * @return 访客集合
     */
    public List<PxVisits> selectPxVisitsList(PxVisits pxVisits);

    /**
     * 新增访客
     *
     * @param pxVisits 访客
     * @return 结果
     */
    public int insertPxVisits(PxVisits pxVisits);

    /**
     * 修改访客
     *
     * @param pxVisits 访客
     * @return 结果
     */
    public int updatePxVisits(PxVisits pxVisits);

    /**
     * 删除访客
     *
     * @param id 访客ID
     * @return 结果
     */
    public int deletePxVisitsById(Long id);

    /**
     * 批量删除访客
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxVisitsByIds(Long[] ids);
}
