package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxRecipe;
import com.pnkx.service.IPxRecipeService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxRecipeController
 * @data 2026/07/05
 * @description 菜谱Controller
 */
@RestController
@RequestMapping("/recipe")
public class PxRecipeController extends BaseController {
    @Resource
    private IPxRecipeService pxRecipeService;

    /**
     * 查询菜谱列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxRecipe pxRecipe) {
        startPage();
        List<PxRecipe> list = pxRecipeService.selectPxRecipeList(pxRecipe);
        return getDataTable(list);
    }

    /**
     * 获取菜谱详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxRecipeService.selectPxRecipeById(id));
    }

    /**
     * 获取菜谱（含食材列表）
     */
    @GetMapping(value = "/withIngredients/{id}")
    public AjaxResult getWithIngredients(@PathVariable("id") Long id) {
        return AjaxResult.success(pxRecipeService.selectRecipeWithIngredients(id));
    }

    /**
     * 新增菜谱
     */
    @Log(title = "菜谱", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxRecipe pxRecipe) {
        int rows = pxRecipeService.insertPxRecipe(pxRecipe);
        if (rows > 0) {
            return AjaxResult.success(pxRecipe.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改菜谱
     */
    @Log(title = "菜谱", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxRecipe pxRecipe) {
        return toAjax(pxRecipeService.updatePxRecipe(pxRecipe));
    }

    /**
     * 删除菜谱
     */
    @Log(title = "菜谱", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxRecipeService.deletePxRecipeByIds(ids));
    }
}
