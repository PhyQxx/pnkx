package com.pnkx.system.service;

import java.util.List;

import com.pnkx.system.domain.SysDataGroup;

/**
 * 数据权限群组 服务层
 *
 * @author pnkx
 */
public interface ISysDataGroupService {
    /**
     * 查询群组列表
     *
     * @param group 群组信息
     * @return 群组集合
     */
    public List<SysDataGroup> selectDataGroupList(SysDataGroup group);

    /**
     * 查询群组详情（含成员用户ID列表）
     *
     * @param id 群组ID
     * @return 群组
     */
    public SysDataGroup selectDataGroupById(Long id);

    /**
     * 新增群组（含成员）
     *
     * @param group 群组信息
     * @return 影响行数
     */
    public int insertDataGroup(SysDataGroup group);

    /**
     * 修改群组（含成员）
     *
     * @param group 群组信息
     * @return 影响行数
     */
    public int updateDataGroup(SysDataGroup group);

    /**
     * 删除群组（含成员）
     *
     * @param id 群组ID
     * @return 影响行数
     */
    public int deleteDataGroupById(Long id);

    /**
     * 查询某用户可见的全部 userId（自己 + 所在群组的所有成员）。
     * 用于数据权限过滤。
     *
     * @param userId 用户ID
     * @return 可见 userId 集合（含自身）
     */
    public List<Long> selectVisibleUserIds(Long userId);

    /** 按业务共享域查询可见用户。 */
    public List<Long> selectVisibleUserIds(Long userId, String module);

    /** 成员主动退出；数据保留给原创建者并写审计。 */
    int leaveGroup(Long groupId, Long userId);

    /** 所有者将空间转移给一名有效成员，原所有者降为管理员。 */
    int transferOwnership(Long groupId, Long newOwnerUserId);

    /** 两个用户是否通过启用指定业务域的空间建立了共享关系。 */
    boolean canShare(Long userId, Long ownerId, String module);
}
