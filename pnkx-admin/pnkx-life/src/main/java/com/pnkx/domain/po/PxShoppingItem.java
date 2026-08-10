package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

/**
 * 购物条目对象
 *
 * @author PHY
 * @date 2026/07/05
 */
public class PxShoppingItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "清单ID")
    private Long listId;

    @Excel(name = "商品名称")
    private String name;

    @Excel(name = "数量")
    private String quantity;

    @Excel(name = "分类ID")
    private Long classificationId;

    @Excel(name = "是否已购")
    private Boolean checked;

    @Excel(name = "来自餐饮")
    private Boolean addedFromMeal;

    @Excel(name = "排序")
    private Integer sortOrder;

    @Excel(name = "版本号")
    private String version;

    private String clientUuid;

    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getListId() { return listId; }
    public void setListId(Long listId) { this.listId = listId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public Long getClassificationId() { return classificationId; }
    public void setClassificationId(Long classificationId) { this.classificationId = classificationId; }
    public Boolean getChecked() { return checked; }
    public void setChecked(Boolean checked) { this.checked = checked; }
    public Boolean getAddedFromMeal() { return addedFromMeal; }
    public void setAddedFromMeal(Boolean addedFromMeal) { this.addedFromMeal = addedFromMeal; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
