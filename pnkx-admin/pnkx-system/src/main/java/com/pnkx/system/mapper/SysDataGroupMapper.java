package com.pnkx.system.mapper;

import java.util.List;

import com.pnkx.system.domain.SysDataGroup;
import com.pnkx.system.domain.SysDataGroupMember;

/**
 * 数据权限群组 数据层
 *
 * @author pnkx
 */
public interface SysDataGroupMapper {
    /**
     * 查询群组列表
     *
     * @param group 群组信息
     * @return 群组集合
     */
    public List<SysDataGroup> selectDataGroupList(SysDataGroup group);

    /**
     * 查询群组详情
     *
     * @param id 群组ID
     * @return 群组
     */
    public SysDataGroup selectDataGroupById(Long id);

    /**
     * 新增群组
     *
     * @param group 群组信息
     * @return 影响行数
     */
    public int insertDataGroup(SysDataGroup group);

    /**
     * 修改群组
     *
     * @param group 群组信息
     * @return 影响行数
     */
    public int updateDataGroup(SysDataGroup group);

    /**
     * 删除群组
     *
     * @param id 群组ID
     * @return 影响行数
     */
    public int deleteDataGroupById(Long id);

    /**
     * 查询群组成员ID列表
     *
     * @param groupId 群组ID
     * @return 成员用户ID集合
     */
    public List<Long> selectMemberUserIds(Long groupId);

    /**
     * 批量新增群组成员
     *
     * @param members 成员列表
     * @return 影响行数
     */
    public int batchInsertMember(List<SysDataGroupMember> members);

    /**
     * 删除群组全部成员
     *
     * @param groupId 群组ID
     * @return 影响行数
     */
    public int deleteMemberByGroupId(Long groupId);

    /**
     * 查询某用户可见的全部 userId（自己 + 所在群组的所有成员）。
     * 用于数据权限过滤。已包含 userId 自身。
     *
     * @param userId 用户ID
     * @return 可见 userId 集合
     */
    public List<Long> selectVisibleUserIds(Long userId);
}
