package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 菜谱对象
 *
 * @author PHY
 * @date 2026/07/05
 */
public class PxRecipe extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 食材列表（非持久化字段）
     */
    private transient List<PxRecipeIngredient> ingredients;

    @Excel(name = "菜名")
    private String title;

    @Excel(name = "链接")
    private String url;

    private String notes;

    @Excel(name = "份数")
    private Integer servings;

    @Excel(name = "版本号")
    private String version;

    private String clientUuid;

    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public List<PxRecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<PxRecipeIngredient> ingredients) { this.ingredients = ingredients; }
}
