package com.pnkx.domain.po;

import lombok.Data;

@Data
public class Target {
    /**
     * 频道ID（如果消息发送到频道）
     */
    private Integer gid;
    
    /**
     * 用户ID（如果消息发送到个人）
     */
    private Integer uid;
}
