package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxShareResource;
import com.pnkx.mapper.PxShareResourceMapper;
import com.pnkx.service.IPxShareResourceService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 分享资源Service业务层处理
 *
 * @author Codex
 * @date 2026-07-03
 */
@Service
public class PxShareResourceServiceImpl implements IPxShareResourceService {
    private static final String DEFAULT_REMARK = "复制这段内容打开「百度网盘APP 即可获取」";

    @Resource
    private PxShareResourceMapper pxShareResourceMapper;

    /**
     * 查询分享资源
     *
     * @param id 分享资源ID
     * @return 分享资源
     */
    @Override
    public PxShareResource selectPxShareResourceById(Long id) {
        return pxShareResourceMapper.selectPxShareResourceById(id);
    }

    /**
     * 查询分享资源列表
     *
     * @param pxShareResource 分享资源
     * @return 分享资源
     */
    @Override
    @DataScopeSelf
    public List<PxShareResource> selectPxShareResourceList(PxShareResource pxShareResource) {
        return pxShareResourceMapper.selectPxShareResourceList(pxShareResource);
    }

    /**
     * 查询前台启用分享资源列表
     *
     * @param pxShareResource 分享资源
     * @return 分享资源
     */
    @Override
    public List<PxShareResource> selectClientShareResourceList(PxShareResource pxShareResource) {
        pxShareResource.setStatus("1");
        return pxShareResourceMapper.selectPxShareResourceList(pxShareResource);
    }

    /**
     * 新增分享资源
     *
     * @param pxShareResource 分享资源
     * @return 结果
     */
    @Override
    public int insertPxShareResource(PxShareResource pxShareResource) {
        if (StringUtils.isEmpty(pxShareResource.getRemark())) {
            pxShareResource.setRemark(DEFAULT_REMARK);
        }
        pxShareResource.setCreateBy(SecurityUtils.getUserId());
        pxShareResource.setCreateTime(DateUtils.getNowDate());
        return pxShareResourceMapper.insertPxShareResource(pxShareResource);
    }

    /**
     * 修改分享资源
     *
     * @param pxShareResource 分享资源
     * @return 结果
     */
    @Override
    public int updatePxShareResource(PxShareResource pxShareResource) {
        pxShareResource.setUpdateBy(SecurityUtils.getUserName());
        pxShareResource.setUpdateTime(DateUtils.getNowDate());
        return pxShareResourceMapper.updatePxShareResource(pxShareResource);
    }

    /**
     * 批量删除分享资源
     *
     * @param ids 需要删除的分享资源ID
     * @return 结果
     */
    @Override
    public int deletePxShareResourceByIds(Long[] ids) {
        return pxShareResourceMapper.deletePxShareResourceByIds(ids);
    }

    /**
     * 删除分享资源信息
     *
     * @param id 分享资源ID
     * @return 结果
     */
    @Override
    public int deletePxShareResourceById(Long id) {
        return pxShareResourceMapper.deletePxShareResourceById(id);
    }

    /**
     * 获取分享资源标签列表
     *
     * @return 标签列表
     */
    @Override
    public List<String> getLabelList() {
        List<String> result = new ArrayList<>();
        List<String> labelList = pxShareResourceMapper.getLabelList();
        labelList.forEach(item -> {
            String[] split = item.split(",");
            for (String s : split) {
                if (!result.contains(s)) {
                    result.add(s);
                }
            }
        });
        return result;
    }

    /**
     * 增加点击次数
     *
     * @param id 分享资源ID
     * @return 结果
     */
    @Override
    public int incrementClickCount(Long id) {
        return pxShareResourceMapper.incrementClickCount(id);
    }
}
