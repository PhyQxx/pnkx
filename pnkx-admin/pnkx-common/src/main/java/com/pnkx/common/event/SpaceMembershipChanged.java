package com.pnkx.common.event;

/** 空间成员或角色变更；业务模块在同一事务提交前清理失效协作分配。 */
public record SpaceMembershipChanged(Long groupId, Long userId) {}
