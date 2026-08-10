package com.pnkx.service.impl;

import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxRegion;
import com.pnkx.mapper.PxRegionMapper;
import com.pnkx.service.IPxRegionService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 地区管理Service业务层处理
 *
 * @author 裴浩宇
 * @date 2023-12-06
 */
@Service
public class PxRegionServiceImpl implements IPxRegionService {
    @Resource
    private PxRegionMapper pxRegionMapper;

    /**
     * 查询地区管理列表
     *
     * @param pxRegion 地区管理
     * @return 地区管理
     */
    @Override
    public List<PxRegion> getRegionList(PxRegion pxRegion) {
        List<PxRegion> regionList = pxRegionMapper.getRegionList(pxRegion);
        // 如果名称不为空，说明是模糊查询需要处理数据
        if (StringUtils.isNotEmpty(pxRegion.getName())) {
            // 遍历所有地区
            for (PxRegion region : regionList) {
                if (region.getLb() == 1) {
                    // 市的话默认带出该市的区
                    // 参数
                    PxRegion param = new PxRegion();
                    param.setLb(2L);
                    param.setSsdqdm(region.getId());
                    // 获取该市下的区
                    List<PxRegion> children = pxRegionMapper.getRegionList(param);
                    region.setChildren(children);
                } else if (region.getLb() == 2) {
                    region.setChildren(null);
                    // 区的话
                    // 参数
                    PxRegion param = new PxRegion();
                    // 找到区的所属市
                    param.setId(region.getSsdqdm());
                    List<PxRegion> city = pxRegionMapper.getRegionList(param);
                    city.get(0).setChildren(Collections.singletonList(region));
                    // 将市加入
                    regionList.add(city.get(0));
                    regionList.remove(region);
                }
            }
        }
        return regionList;
    }
}
