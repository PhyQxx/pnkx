package com.pnkx.life.domain.po;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author by PHY
 * @classname PxBookkeeping
 * @date 2021-11-08 20:24
 * @description: 描述
 */
@Data
public class PxBookkeepingAccount extends BaseEntity {
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
     * 账户类型
     */
    @Excel(name = "账户类型")
    private String accountType;

    /**
     * 账户图标
     */
    @Excel(name = "账户图标")
    private String accountIcon;

    /**
     * 账户名称
     */
    @Excel(name = "账户名称")
    private String accountName;

    /**
     * 结余（列表/详情接口返回按流水实时计算的值；JSON 按字符串输出保持前端契约）
     */
    @Excel(name = "结余")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal balance;

    /**
     * 流入
     */
    @Excel(name = "流入")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal inflow;

    /**
     * 流出
     */
    @Excel(name = "流出")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal flowOut;

    /**
     * 删除标志
     */
    private Boolean delFlag;

    /**
     * 子集合
     */
    private List<PxBookkeepingAccount> children;
}
