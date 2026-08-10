package com.pnkx.service;

/**
 * @author by PHY
 * @Classname IPxAdminFriendLinkService
 * @date 2021-04-30 11:43
 */

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pnkx.domain.po.PxFriendLink;

import java.util.List;

/**
 * 友链Service接口
 *
 * @author phy
 * @date 2021-04-30
 */
public interface IPxFriendLinkService extends IService<PxFriendLink> {
    /**
     * 查询友链
     *
     * @param id 友链ID
     * @return 友链
     */
    PxFriendLink selectPxFriendLinkById(Long id);

    /**
     * 查询友链列表
     *
     * @param pxFriendLink 友链
     * @return 友链集合
     */
    List<PxFriendLink> selectPxFriendLinkList(PxFriendLink pxFriendLink);

    /**
     * 查询友链列表（分页）
     *
     * @param page 分页对象
     * @param pxFriendLink 友链
     * @return 友链分页结果
     */
    IPage<PxFriendLink> selectPxFriendLinkList(IPage<PxFriendLink> page, PxFriendLink pxFriendLink);

    /**
     * 新增友链
     *
     * @param pxFriendLink 友链
     * @return 结果
     */
    int insertPxFriendLink(PxFriendLink pxFriendLink);

    /**
     * 修改友链
     *
     * @param pxFriendLink 友链
     * @return 结果
     */
    int updatePxFriendLink(PxFriendLink pxFriendLink);

    /**
     * 批量删除友链
     *
     * @param ids 需要删除的友链ID
     * @return 结果
     */
    int deletePxFriendLinkByIds(Long[] ids);

    /**
     * 删除友链信息
     *
     * @param id 友链ID
     * @return 结果
     */
    int deletePxFriendLinkById(Long id);
}
