package com.pnkx.service;

import com.pnkx.domain.po.PxRecipe;

import java.util.List;

/**
 * @author PHY
 * @classname IPxRecipeService
 * @data 2026/07/05
 * @description 菜谱Service接口
 */
public interface IPxRecipeService {
    /**
     * 查询菜谱
     *
     * @param id 菜谱ID
     * @return 菜谱
     */
    public PxRecipe selectPxRecipeById(Long id);

    /**
     * 查询菜谱列表
     *
     * @param pxRecipe 菜谱
     * @return 菜谱集合
     */
    public List<PxRecipe> selectPxRecipeList(PxRecipe pxRecipe);

    /**
     * 新增菜谱
     *
     * @param pxRecipe 菜谱
     * @return 结果
     */
    public int insertPxRecipe(PxRecipe pxRecipe);

    /**
     * 修改菜谱
     *
     * @param pxRecipe 菜谱
     * @return 结果
     */
    public int updatePxRecipe(PxRecipe pxRecipe);

    /**
     * 批量删除菜谱
     *
     * @param ids 需要删除的菜谱ID
     * @return 结果
     */
    public int deletePxRecipeByIds(Long[] ids);

    /**
     * 删除菜谱信息
     *
     * @param id 菜谱ID
     * @return 结果
     */
    public int deletePxRecipeById(Long id);

    /**
     * 查询菜谱含食材列表
     *
     * @param id 菜谱ID
     * @return 菜谱（含食材列表）
     */
    public PxRecipe selectRecipeWithIngredients(Long id);
}
