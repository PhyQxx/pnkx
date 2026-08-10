package com.pnkx.system.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.system.domain.SysDataGroup;
import com.pnkx.system.domain.SysDataGroupMember;
import com.pnkx.system.mapper.SysDataGroupMapper;
import com.pnkx.system.service.ISysDataGroupService;

/**
 * 数据权限群组 服务层处理
 *
 * @author pnkx
 */
@Service
public class SysDataGroupServiceImpl implements ISysDataGroupService {
    @Autowired
    private SysDataGroupMapper dataGroupMapper;

    /**
     * 查询群组列表
     */
    @Override
    public List<SysDataGroup> selectDataGroupList(SysDataGroup group) {
        return dataGroupMapper.selectDataGroupList(group);
    }

    /**
     * 查询群组详情（含成员用户ID列表）
     */
    @Override
    public SysDataGroup selectDataGroupById(Long id) {
        SysDataGroup group = dataGroupMapper.selectDataGroupById(id);
        if (group != null) {
            group.setUserIds(dataGroupMapper.selectMemberUserIds(id));
        }
        return group;
    }

    /**
     * 新增群组（含成员）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDataGroup(SysDataGroup group) {
        group.setCreateTime(DateUtils.getNowDate());
        int rows = dataGroupMapper.insertDataGroup(group);
        insertMembers(group.getId(), group.getUserIds());
        return rows;
    }

    /**
     * 修改群组（含成员，先清后插）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDataGroup(SysDataGroup group) {
        group.setUpdateTime(DateUtils.getNowDate());
        int rows = dataGroupMapper.updateDataGroup(group);
        dataGroupMapper.deleteMemberByGroupId(group.getId());
        insertMembers(group.getId(), group.getUserIds());
        return rows;
    }

    /**
     * 删除群组（含成员）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDataGroupById(Long id) {
        dataGroupMapper.deleteMemberByGroupId(id);
        return dataGroupMapper.deleteDataGroupById(id);
    }

    /**
     * 查询某用户可见的全部 userId（自己 + 所在群组的所有成员）
     */
    @Override
    public List<Long> selectVisibleUserIds(Long userId) {
        List<Long> ids = dataGroupMapper.selectVisibleUserIds(userId);
        if (ids == null || ids.isEmpty()) {
            // 兜底：至少能看见自己
            return new ArrayList<>(Collections.singletonList(userId));
        }
        return ids;
    }

    /**
     * 批量写入群组成员
     */
    private void insertMembers(Long groupId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Date now = DateUtils.getNowDate();
        List<SysDataGroupMember> members = new ArrayList<>();
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            SysDataGroupMember member = new SysDataGroupMember();
            member.setGroupId(groupId);
            member.setUserId(userId);
            member.setCreateTime(now);
            members.add(member);
        }
        if (!members.isEmpty()) {
            dataGroupMapper.batchInsertMember(members);
        }
    }
}
