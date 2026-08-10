package com.pnkx.mapper;

import com.pnkx.domain.po.PxRecipe;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜谱 Mapper
 *
 * @author PHY
 * @date 2026/07/05
 */
@Mapper
public interface PxRecipeMapper {
    PxRecipe selectPxRecipeById(Long id);

    List<PxRecipe> selectPxRecipeList(PxRecipe pxRecipe);

    int insertPxRecipe(PxRecipe pxRecipe);

    int updatePxRecipe(PxRecipe pxRecipe);

    int deletePxRecipeById(Long id);

    int deletePxRecipeByIds(Long[] ids);
}
