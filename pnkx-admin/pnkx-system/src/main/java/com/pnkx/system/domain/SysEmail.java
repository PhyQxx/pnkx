package com.pnkx.system.domain;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 邮件记录对象 px_email
 *
 * @author phy
 * @date 2021-05-13
 */
@Data
public class SysEmail extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 发送者ID
     */
    @Excel(name = "发送者ID")
    private Long sendId;

    /**
     * 收件人ID
     */
    @Excel(name = "收件人ID")
    private Long receiverId;

    /**
     * 收件人邮箱
     */
    @Excel(name = "收件人邮箱")
    private String receiverEmail;

    /**
     * 抄送人id,多个用逗号分隔
     */
    @Excel(name = "抄送人id,多个用逗号分隔")
    private Long ccId;

    /**
     * 抄送人邮箱,多个用逗号分隔
     */
    @Excel(name = "抄送人邮箱,多个用逗号分隔")
    private String ccEmail;

    /**
     * 邮件主题
     */
    @Excel(name = "邮件主题")
    private String subject;

    /**
     * 邮件内容
     */
    @Excel(name = "邮件内容")
    private String content;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;
}
