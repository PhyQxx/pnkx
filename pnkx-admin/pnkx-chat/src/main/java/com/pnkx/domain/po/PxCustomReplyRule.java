package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 自定义回复规则实体类
 * 
 * @author pnkx
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PxCustomReplyRule extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 规则名称 */
    @Excel(name = "规则名称")
    private String ruleName;

    /** 关键词列表（JSON格式） */
    @Excel(name = "关键词列表", readConverterExp = "JSON格式")
    private String keywords;

    /** 回复内容列表 */
    private List<PxCustomReplyContent> replyContents;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private Boolean enabled;

    /** 是否精确匹配 */
    @Excel(name = "是否精确匹配")
    private Boolean exactMatch;

    /** 优先级 */
    @Excel(name = "优先级")
    private Long priority;

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
