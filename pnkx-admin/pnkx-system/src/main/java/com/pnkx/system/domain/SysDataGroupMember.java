package com.pnkx.system.domain;

import lombok.Data;

import java.util.Date;

/**
 * 数据权限群组成员 px_data_group_member
 *
 * @author pnkx
 */
@Data
public class SysDataGroupMember {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 群组ID
     */
    private Long groupId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 加入时间
     */
    private Date createTime;
}
