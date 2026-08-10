package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.List;

/**
 * 账本分类对象 px_bookkeeping_classification
 *
 * @author pnkx
 * @date 2021-11-18
 */
@Data
public class PxBookkeepingClassification extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 版本号
     */
    private String version;

    /**
     * 种类图标
     */
    @Excel(name = "种类图标")
    private String typeIcon;

    /**
     * 排序
     */
    private Integer orderNum;

    /**
     * 种类名称
     */
    @Excel(name = "种类名称")
    private String typeName;

    /**
     * 种类等级，0：一级，1：二级
     */
    @Excel(name = "种类等级，0：一级，1：二级")
    private String typeLevel;

    /**
     * 种类parentId
     */
    @Excel(name = "种类parentId")
    private Long typeParentId;

    /**
     * 种类区分，0：收入，1：支出
     */
    @Excel(name = "种类区分，0：收入，1：支出")
    private String typeDifference;

    /**
     * 收入统计与支出统计
     */
    @Excel(name = "收入统计与支出统计")
    private String statistics;

    /**
     * 删除标志
     */
    private Boolean delFlag;

    /**
     * 子分类
     */
    private List<PxBookkeepingClassification> children;
}
