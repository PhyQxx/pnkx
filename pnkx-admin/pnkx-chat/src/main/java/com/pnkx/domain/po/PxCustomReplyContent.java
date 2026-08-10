package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 自定义回复规则内容表
 * @author pnkx
 */
@Data
public class PxCustomReplyContent extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 规则ID */
    @Excel(name = "规则ID")
    private Long ruleId;

    /** 回复内容 */
    @Excel(name = "回复内容")
    private String content;

    /** 权重（用于控制随机概率） */
    @Excel(name = "权重")
    private Integer weight;

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
