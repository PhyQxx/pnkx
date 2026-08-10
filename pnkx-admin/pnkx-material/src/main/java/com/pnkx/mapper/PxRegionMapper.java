package com.pnkx.mapper;

import com.pnkx.domain.po.PxRegion;

import java.util.List;

/**
 * 地区管理Mapper接口
 *
 * @author 裴浩宇
 * @date 2023-12-06
 */
public interface PxRegionMapper {

    /**
     * 查询地区管理列表
     *
     * @param pxRegion 地区管理
     * @return 地区管理集合
     */
    public List<PxRegion> getRegionList(PxRegion pxRegion);
}
