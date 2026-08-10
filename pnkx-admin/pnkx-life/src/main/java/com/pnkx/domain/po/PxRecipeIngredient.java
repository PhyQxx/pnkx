package com.pnkx.domain.po;

/**
 * 菜谱食材对象
 *
 * @author PHY
 * @date 2026/07/05
 */
public class PxRecipeIngredient {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long recipeId;
    private String name;
    private String quantity;
    private Long classificationId;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public Long getClassificationId() { return classificationId; }
    public void setClassificationId(Long classificationId) { this.classificationId = classificationId; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
