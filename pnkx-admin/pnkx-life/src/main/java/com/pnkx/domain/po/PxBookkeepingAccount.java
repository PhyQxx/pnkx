package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

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
     * 结余
     */
    @Excel(name = "结余")
    private String balance;

    /**
     * 流入
     */
    @Excel(name = "流入")
    private String inflow;

    /**
     * 流出
     */
    @Excel(name = "流出")
    private String flowOut;

    /**
     * 删除标志
     */
    private Boolean delFlag;

    /**
     * 子集合
     */
    private List<PxBookkeepingAccount> children;
}
