package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 餐饮计划对象
 *
 * @author PHY
 * @date 2026/07/05
 */
public class PxMealPlan extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日期", dateFormat = "yyyy-MM-dd")
    private Date planDate;

    @Excel(name = "餐次（1早2午3晚4加餐）")
    private Integer mealType;

    private Long recipeId;

    @Excel(name = "餐名")
    private String title;

    private String notes;

    @Excel(name = "排序")
    private Integer sortOrder;

    @Excel(name = "版本号")
    private String version;

    private String clientUuid;

    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getPlanDate() { return planDate; }
    public void setPlanDate(Date planDate) { this.planDate = planDate; }
    public Integer getMealType() { return mealType; }
    public void setMealType(Integer mealType) { this.mealType = mealType; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
