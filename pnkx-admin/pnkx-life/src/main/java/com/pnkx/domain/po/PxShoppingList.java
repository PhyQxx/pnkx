package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

/**
 * 购物清单对象
 *
 * @author PHY
 * @date 2026/07/05
 */
public class PxShoppingList extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "清单名称")
    private String name;

    @Excel(name = "图标")
    private String icon;

    @Excel(name = "排序")
    private Integer orderNum;

    @Excel(name = "版本号")
    private String version;

    private String clientUuid;

    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
