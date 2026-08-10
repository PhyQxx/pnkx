package com.pnkx.service.impl;

import com.pnkx.domain.po.PxVisits;
import com.pnkx.mapper.PxVisitsMapper;
import com.pnkx.service.IPxVisitsService;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 访客Service业务层处理
 *
 * @author phy
 * @date 2021-10-30
 */
@Service
public class PxVisitsServiceImpl implements IPxVisitsService {

    @Resource
    private PxVisitsMapper pxVisitsMapper;

    /**
     * 查询访客
     *
     * @param id 访客ID
     * @return 访客
     */
    @Override
    public PxVisits selectPxVisitsById(Long id) {
        return pxVisitsMapper.selectPxVisitsById(id);
    }

    /**
     * 查询访客列表
     *
     * @param pxVisits 访客
     * @return 访客
     */
    @Override
    public List<PxVisits> selectPxVisitsList(PxVisits pxVisits) {
        return pxVisitsMapper.selectPxVisitsList(pxVisits);
    }

    /**
     * 新增访客
     *
     * @param pxVisits 访客
     * @return 结果
     */
    @Override
    public int insertPxVisits(PxVisits pxVisits) {
        pxVisits.setCreateTime(DateUtils.getNowDate());
        try {
            pxVisits.setCreateBy(SecurityUtils.getUserId());
        } catch (Exception e) {
            pxVisits.setCreateBy("游客");
        }
        return pxVisitsMapper.insertPxVisits(pxVisits);
    }

    /**
     * 修改访客
     *
     * @param pxVisits 访客
     * @return 结果
     */
    @Override
    public int updatePxVisits(PxVisits pxVisits) {
        pxVisits.setUpdateTime(DateUtils.getNowDate());
        pxVisits.setUpdateBy(SecurityUtils.getUserName());
        return pxVisitsMapper.updatePxVisits(pxVisits);
    }

    /**
     * 批量删除访客
     *
     * @param ids 需要删除的访客ID
     * @return 结果
     */
    @Override
    public int deletePxVisitsByIds(Long[] ids) {
        return pxVisitsMapper.deletePxVisitsByIds(ids);
    }

    /**
     * 删除访客信息
     *
     * @param id 访客ID
     * @return 结果
     */
    @Override
    public int deletePxVisitsById(Long id) {
        return pxVisitsMapper.deletePxVisitsById(id);
    }
}
