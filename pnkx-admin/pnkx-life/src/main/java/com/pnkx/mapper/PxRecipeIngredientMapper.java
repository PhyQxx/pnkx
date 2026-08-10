package com.pnkx.mapper;

import com.pnkx.domain.po.PxRecipeIngredient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜谱食材 Mapper
 *
 * @author PHY
 * @date 2026/07/05
 */
@Mapper
public interface PxRecipeIngredientMapper {
    PxRecipeIngredient selectPxRecipeIngredientById(Long id);

    List<PxRecipeIngredient> selectByRecipeId(@Param("recipeId") Long recipeId);

    int insertPxRecipeIngredient(PxRecipeIngredient ingredient);

    int deleteByRecipeId(@Param("recipeId") Long recipeId);
}
