package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 记账预算（月度总预算或分类预算）
 *
 * @author PHY
 * @date 2026-09-23
 */
@Data
public class PxBookkeepingBudget extends BaseEntity {
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
     * 预算归属月 yyyy-MM
     */
    private String month;

    /**
     * 分类ID（0 表示月度总预算，否则为二级分类ID）
     */
    private Long typeId;

    /**
     * 预算金额
     */
    private BigDecimal amount;

    /**
     * 分类名称（查询联表展示用，不落库）
     */
    private String typeName;

    /**
     * 当月该预算口径已支出金额（状态计算用，不落库）
     */
    private BigDecimal used;

    /**
     * 剩余额度（状态计算用，不落库）
     */
    private BigDecimal remaining;

    /**
     * 使用百分比（状态计算用，不落库）
     */
    private Integer percent;

    /**
     * 是否超支（状态计算用，不落库）
     */
    private Boolean exceeded;
}
