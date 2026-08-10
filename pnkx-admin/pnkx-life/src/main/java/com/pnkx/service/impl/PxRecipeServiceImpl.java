package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.PxRecipe;
import com.pnkx.domain.po.PxRecipeIngredient;
import com.pnkx.mapper.PxRecipeIngredientMapper;
import com.pnkx.mapper.PxRecipeMapper;
import com.pnkx.service.IPxRecipeService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxRecipeServiceImpl
 * @data 2026/07/05
 * @description 菜谱Service实现
 */
@Service
public class PxRecipeServiceImpl implements IPxRecipeService {
    @Resource
    private PxRecipeMapper pxRecipeMapper;

    @Resource
    private PxRecipeIngredientMapper pxRecipeIngredientMapper;

    /**
     * 查询菜谱
     *
     * @param id 菜谱ID
     * @return 菜谱
     */
    @Override
    public PxRecipe selectPxRecipeById(Long id) {
        return pxRecipeMapper.selectPxRecipeById(id);
    }

    /**
     * 查询菜谱列表
     *
     * @param pxRecipe 菜谱
     * @return 菜谱
     */
    @Override
    @DataScopeSelf
    public List<PxRecipe> selectPxRecipeList(PxRecipe pxRecipe) {
        return pxRecipeMapper.selectPxRecipeList(pxRecipe);
    }


    /**
     * 新增菜谱
     *
     * @param pxRecipe 菜谱
     * @return 结果
     */
    @Override
    public int insertPxRecipe(PxRecipe pxRecipe) {
        pxRecipe.setCreateTime(DateUtils.getNowDate());
        int rows = pxRecipeMapper.insertPxRecipe(pxRecipe);
        if (pxRecipe.getIngredients() != null) {
            pxRecipeIngredientMapper.deleteByRecipeId(pxRecipe.getId());
            for (PxRecipeIngredient ingredient : pxRecipe.getIngredients()) {
                ingredient.setRecipeId(pxRecipe.getId());
                pxRecipeIngredientMapper.insertPxRecipeIngredient(ingredient);
            }
        }
        return rows;
    }

    /**
     * 修改菜谱
     *
     * @param pxRecipe 菜谱
     * @return 结果
     */
    @Override
    public int updatePxRecipe(PxRecipe pxRecipe) {
        pxRecipe.setUpdateTime(DateUtils.getNowDate());
        int rows = pxRecipeMapper.updatePxRecipe(pxRecipe);
        if (pxRecipe.getIngredients() != null) {
            pxRecipeIngredientMapper.deleteByRecipeId(pxRecipe.getId());
            for (PxRecipeIngredient ingredient : pxRecipe.getIngredients()) {
                ingredient.setRecipeId(pxRecipe.getId());
                pxRecipeIngredientMapper.insertPxRecipeIngredient(ingredient);
            }
        }
        return rows;
    }

    /**
     * 批量删除菜谱
     *
     * @param ids 需要删除的菜谱ID
     * @return 结果
     */
    @Override
    public int deletePxRecipeByIds(Long[] ids) {
        return pxRecipeMapper.deletePxRecipeByIds(ids);
    }

    /**
     * 删除菜谱信息
     *
     * @param id 菜谱ID
     * @return 结果
     */
    @Override
    public int deletePxRecipeById(Long id) {
        return pxRecipeMapper.deletePxRecipeById(id);
    }

    /**
     * 查询菜谱含食材列表
     *
     * @param id 菜谱ID
     * @return 菜谱（含食材列表）
     */
    @Override
    public PxRecipe selectRecipeWithIngredients(Long id) {
        PxRecipe pxRecipe = pxRecipeMapper.selectPxRecipeById(id);
        if (pxRecipe != null) {
            List<PxRecipeIngredient> ingredients = pxRecipeIngredientMapper.selectByRecipeId(id);
            pxRecipe.setIngredients(ingredients);
        }
        return pxRecipe;
    }
}
