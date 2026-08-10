package com.pnkx.domain.po;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * @author pnkx
 */
@Data
public class PxChatMessage extends BaseEntity
{
    private static final long serialVersionUID=1L;

    /** 主键ID */
    private Long id;

    /** 消息ID */
    @Excel(name = "消息ID")
    private Integer messageId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Integer userId;

    /** 用户名 */
    @Excel(name = "用户名")
    private String userName;

    /** 群组ID */
    @Excel(name = "群组ID")
    private Integer groupId;

    /** 群组名 */
    @Excel(name = "群组名")
    private String groupName;

    /** 消息内容 */
    @Excel(name = "消息内容")
    private String content;

    /** 消息类型 */
    @Excel(name = "消息类型")
    private String messageType;

    /** 是否为机器人回复 */
    @Excel(name = "是否为机器人回复")
    private Boolean isBotReply;

    /** 机器人回复内容 */
    @Excel(name = "机器人回复内容")
    private String botReplyContent;

    /** 逻辑删除标记 */
    @Excel(name = "逻辑删除标记")
    private Long deleted;

    /** 记录创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createAt;

    /** 记录更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "记录更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updateAt;
}
