package com.pnkx.service;

import com.pnkx.domain.po.PxShareResource;

import java.util.List;

/**
 * 分享资源Service接口
 *
 * @author Codex
 * @date 2026-07-03
 */
public interface IPxShareResourceService {
    /**
     * 查询分享资源
     *
     * @param id 分享资源ID
     * @return 分享资源
     */
    PxShareResource selectPxShareResourceById(Long id);

    /**
     * 查询分享资源列表
     *
     * @param pxShareResource 分享资源
     * @return 分享资源集合
     */
    List<PxShareResource> selectPxShareResourceList(PxShareResource pxShareResource);

    /**
     * 查询前台启用分享资源列表
     *
     * @param pxShareResource 分享资源
     * @return 分享资源集合
     */
    List<PxShareResource> selectClientShareResourceList(PxShareResource pxShareResource);

    /**
     * 新增分享资源
     *
     * @param pxShareResource 分享资源
     * @return 结果
     */
    int insertPxShareResource(PxShareResource pxShareResource);

    /**
     * 修改分享资源
     *
     * @param pxShareResource 分享资源
     * @return 结果
     */
    int updatePxShareResource(PxShareResource pxShareResource);

    /**
     * 批量删除分享资源
     *
     * @param ids 需要删除的分享资源ID
     * @return 结果
     */
    int deletePxShareResourceByIds(Long[] ids);

    /**
     * 删除分享资源信息
     *
     * @param id 分享资源ID
     * @return 结果
     */
    int deletePxShareResourceById(Long id);

    /**
     * 获取分享资源标签列表
     *
     * @return 标签列表
     */
    List<String> getLabelList();

    /**
     * 增加点击次数
     *
     * @param id 分享资源ID
     * @return 结果
     */
    int incrementClickCount(Long id);
}
