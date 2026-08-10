package com.pnkx.service.impl;

import com.pnkx.domain.po.PxEmailSubscribe;
import com.pnkx.mapper.PxEmailSubscribeMapper;
import com.pnkx.service.IPxEmailSubscribeService;
import com.pnkx.common.utils.DateUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author by PHY
 * @Classname PxAdminSubscribeServiceImpl
 * @date 2021-06-17 15:34
 */
@Service
public class PxEmailSubscribeServiceImpl implements IPxEmailSubscribeService {

    @Resource
    private PxEmailSubscribeMapper pxEmailSubscribeMapper;

    /**
     * 查询订阅
     *
     * @param id 订阅ID
     * @return 订阅
     */
    @Override
    public PxEmailSubscribe selectPxEmailSubscribeById(Long id) {
        return pxEmailSubscribeMapper.selectPxEmailSubscribeById(id);
    }

    /**
     * 查询订阅列表
     *
     * @param pxEmailSubscribe 订阅
     * @return 订阅
     */
    @Override
    public List<PxEmailSubscribe> selectPxEmailSubscribeList(PxEmailSubscribe pxEmailSubscribe) {
        return pxEmailSubscribeMapper.selectPxEmailSubscribeList(pxEmailSubscribe);
    }

    /**
     * 新增订阅
     *
     * @param pxEmailSubscribe 订阅
     * @return 结果
     */
    @Override
    public int insertPxEmailSubscribe(PxEmailSubscribe pxEmailSubscribe) {
        pxEmailSubscribe.setCreateTime(DateUtils.getNowDate());
        return pxEmailSubscribeMapper.insertPxEmailSubscribe(pxEmailSubscribe);
    }

    /**
     * 修改订阅
     *
     * @param pxEmailSubscribe 订阅
     * @return 结果
     */
    @Override
    public int updatePxEmailSubscribe(PxEmailSubscribe pxEmailSubscribe) {
        pxEmailSubscribe.setUpdateTime(DateUtils.getNowDate());
        return pxEmailSubscribeMapper.updatePxEmailSubscribe(pxEmailSubscribe);
    }

    /**
     * 批量删除订阅
     *
     * @param ids 需要删除的订阅ID
     * @return 结果
     */
    @Override
    public int deletePxEmailSubscribeByIds(Long[] ids) {
        return pxEmailSubscribeMapper.deletePxEmailSubscribeByIds(ids);
    }

    /**
     * 删除订阅信息
     *
     * @param id 订阅ID
     * @return 结果
     */
    @Override
    public int deletePxEmailSubscribeById(Long id) {
        return pxEmailSubscribeMapper.deletePxEmailSubscribeById(id);
    }
}
