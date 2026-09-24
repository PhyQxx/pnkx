package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 周期记账规则（房租/会员等固定支出按期自动生成记录）
 *
 * @author PHY
 * @date 2026-09-23
 */
@Data
public class PxBookkeepingRecurring extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /** 离线创建幂等键。 */
    private String clientUuid;

    /**
     * 版本号
     */
    private String version;

    /**
     * 规则名称（如：房租）
     */
    private String name;

    /**
     * 频率：month 每月 / week 每周
     */
    private String frequency;

    /**
     * 月频率为几号(1-28)；周频率为周几(1-7，周一=1)
     */
    private Integer dayNumber;

    /**
     * 收支类型（0收入 1支出 2转账）
     */
    private String typeDifference;

    /**
     * 分类ID
     */
    private Long type;

    /**
     * 账户ID
     */
    private Long account;

    /**
     * 转入账户ID（转账时）
     */
    private Long otherAccount;

    /**
     * 金额
     */
    private BigDecimal money;

    /**
     * 下次执行日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date nextRunDate;

    /**
     * 是否启用（1启用 0停用）
     */
    private Boolean enabled;

    /**
     * 上次执行日（查询展示用）
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date lastRunDate;

    /**
     * 分类名称（联表展示，不落库）
     */
    private String typeName;

    /**
     * 账户名称（联表展示，不落库）
     */
    private String accountName;
}
