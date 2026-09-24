package com.pnkx.system.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.exception.ServiceException;
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
        try {
            Long userId = Long.valueOf(SecurityUtils.getUserId());
            if (!SecurityUtils.isAdmin(userId)) group.getParams().put("currentUserId", userId);
        } catch (Exception ignored) {
            // system jobs may query without a login context
        }
        return dataGroupMapper.selectDataGroupList(group);
    }

    /**
     * 查询群组详情（含成员用户ID列表）
     */
    @Override
    public SysDataGroup selectDataGroupById(Long id) {
        SysDataGroup group = dataGroupMapper.selectDataGroupById(id);
        if (group != null) {
            try {
                Long userId = Long.valueOf(SecurityUtils.getUserId());
                boolean member = dataGroupMapper.selectMembers(id).stream()
                        .anyMatch(item -> userId.equals(item.getUserId()) && "active".equals(item.getStatus()));
                if (!SecurityUtils.isAdmin(userId) && !member) throw new ServiceException("空间不存在或无查看权限");
            } catch (ServiceException e) {
                throw e;
            } catch (Exception ignored) {
                // internal calls without login context are filtered by their caller
            }
            group.setUserIds(dataGroupMapper.selectMemberUserIds(id));
            group.setMembers(dataGroupMapper.selectMembers(id));
        }
        return group;
    }

    /**
     * 新增群组（含成员）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDataGroup(SysDataGroup group) {
        Long ownerId = Long.valueOf(group.getCreateBy());
        group.setOwnerUserId(ownerId);
        if (group.getSpaceType() == null) group.setSpaceType("family");
        if (group.getVisibilityJson() == null) group.setVisibilityJson("[\"todo\",\"shopping\",\"meal\",\"commemoration\",\"budget\"]");
        group.setOwnershipPolicy("retain_creator");
        group.setCreateTime(DateUtils.getNowDate());
        int rows = dataGroupMapper.insertDataGroup(group);
        insertMembers(group, group.getUserIds());
        return rows;
    }

    /**
     * 修改群组（含成员，先清后插）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDataGroup(SysDataGroup group) {
        SysDataGroup existing = requireManager(group.getId());
        group.setOwnerUserId(existing.getOwnerUserId());
        group.setOwnershipPolicy("retain_creator");
        group.setUpdateTime(DateUtils.getNowDate());
        int rows = dataGroupMapper.updateDataGroup(group);
        dataGroupMapper.deleteMemberByGroupId(group.getId());
        insertMembers(group, group.getUserIds());
        return rows;
    }

    /**
     * 删除群组（含成员）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDataGroupById(Long id) {
        requireManager(id);
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

    @Override
    public List<Long> selectVisibleUserIds(Long userId, String module) {
        List<Long> ids = dataGroupMapper.selectVisibleUserIdsByModule(userId, module == null ? "" : module);
        return ids == null || ids.isEmpty() ? new ArrayList<>(Collections.singletonList(userId)) : ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int leaveGroup(Long groupId, Long userId) {
        SysDataGroup group = dataGroupMapper.selectDataGroupById(groupId);
        if (group == null) throw new ServiceException("空间不存在");
        if (userId.equals(group.getOwnerUserId())) throw new ServiceException("空间所有者需先转移所有权，不能直接退出");
        int rows = dataGroupMapper.leaveMember(groupId, userId);
        if (rows <= 0) throw new ServiceException("不是该空间的有效成员");
        dataGroupMapper.insertMemberAudit(groupId, userId, "leave", userId,
                "retain_creator", "{\"sharedAccessRevoked\":true,\"dataOwnerUnchanged\":true}");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int transferOwnership(Long groupId, Long newOwnerUserId) {
        SysDataGroup group = dataGroupMapper.selectDataGroupById(groupId);
        Long operatorId = Long.valueOf(SecurityUtils.getUserId());
        if (group == null || (!SecurityUtils.isAdmin(operatorId) && !operatorId.equals(group.getOwnerUserId()))) {
            throw new ServiceException("空间不存在或无转移权限");
        }
        if (newOwnerUserId == null || newOwnerUserId.equals(group.getOwnerUserId())) {
            throw new ServiceException("请选择另一名有效成员作为新所有者");
        }
        boolean activeMember = dataGroupMapper.selectMembers(groupId).stream()
                .anyMatch(item -> newOwnerUserId.equals(item.getUserId()) && "active".equals(item.getStatus()));
        if (!activeMember) throw new ServiceException("新所有者必须是空间的有效成员");
        Long previousOwnerId = group.getOwnerUserId();
        int rows = dataGroupMapper.updateOwner(groupId, newOwnerUserId, String.valueOf(operatorId));
        dataGroupMapper.updateMemberRole(groupId, previousOwnerId, "admin");
        dataGroupMapper.updateMemberRole(groupId, newOwnerUserId, "owner");
        dataGroupMapper.insertMemberAudit(groupId, newOwnerUserId, "transfer_owner", operatorId,
                "retain_creator", "{\"previousOwnerId\":" + previousOwnerId
                        + ",\"newOwnerId\":" + newOwnerUserId + ",\"dataOwnerUnchanged\":true}");
        return rows;
    }

    @Override
    public boolean canShare(Long userId, Long ownerId, String module) {
        return userId != null && ownerId != null && (userId.equals(ownerId)
                || dataGroupMapper.countSharedMembership(userId, ownerId, module) > 0);
    }

    /**
     * 批量写入群组成员
     */
    private void insertMembers(SysDataGroup group, List<Long> userIds) {
        List<Long> normalized = userIds == null ? new ArrayList<>() : new ArrayList<>(userIds);
        if (group.getMembers() != null) group.getMembers().stream().map(SysDataGroupMember::getUserId).forEach(normalized::add);
        if (!normalized.contains(group.getOwnerUserId())) normalized.add(group.getOwnerUserId());
        Date now = DateUtils.getNowDate();
        List<SysDataGroupMember> members = new ArrayList<>();
        for (Long userId : normalized.stream().distinct().toList()) {
            if (userId == null) {
                continue;
            }
            SysDataGroupMember member = new SysDataGroupMember();
            member.setGroupId(group.getId());
            member.setUserId(userId);
            String requestedRole = group.getMembers() == null ? null : group.getMembers().stream()
                    .filter(item -> userId.equals(item.getUserId())).map(SysDataGroupMember::getRole).findFirst().orElse(null);
            member.setRole(userId.equals(group.getOwnerUserId()) ? "owner"
                    : ("admin".equals(requestedRole) ? "admin" : "member"));
            member.setStatus("active");
            member.setCreateTime(now);
            members.add(member);
        }
        if (!members.isEmpty()) {
            dataGroupMapper.batchInsertMember(members);
        }
    }

    private SysDataGroup requireManager(Long groupId) {
        SysDataGroup group = dataGroupMapper.selectDataGroupById(groupId);
        Long userId = Long.valueOf(SecurityUtils.getUserId());
        if (group == null || (!SecurityUtils.isAdmin(userId) && !userId.equals(group.getOwnerUserId()))) {
            throw new ServiceException("空间不存在或无管理权限");
        }
        return group;
    }
}
