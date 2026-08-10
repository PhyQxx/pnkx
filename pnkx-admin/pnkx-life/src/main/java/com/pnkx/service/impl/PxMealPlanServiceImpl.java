package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.PxMealPlan;
import com.pnkx.domain.po.PxRecipeIngredient;
import com.pnkx.domain.po.PxShoppingItem;
import com.pnkx.mapper.PxMealPlanMapper;
import com.pnkx.mapper.PxRecipeIngredientMapper;
import com.pnkx.mapper.PxShoppingItemMapper;
import com.pnkx.service.IPxMealPlanService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author PHY
 * @classname PxMealPlanServiceImpl
 * @data 2026/07/05
 * @description 餐饮计划Service实现
 */
@Service
public class PxMealPlanServiceImpl implements IPxMealPlanService {
    @Resource
    private PxMealPlanMapper pxMealPlanMapper;

    @Resource
    private PxRecipeIngredientMapper pxRecipeIngredientMapper;

    @Resource
    private PxShoppingItemMapper pxShoppingItemMapper;

    /**
     * 查询餐饮计划
     *
     * @param id 餐饮计划ID
     * @return 餐饮计划
     */
    @Override
    public PxMealPlan selectPxMealPlanById(Long id) {
        return pxMealPlanMapper.selectPxMealPlanById(id);
    }

    /**
     * 查询餐饮计划列表
     *
     * @param pxMealPlan 餐饮计划
     * @return 餐饮计划
     */
    @Override
    @DataScopeSelf
    public List<PxMealPlan> selectPxMealPlanList(PxMealPlan pxMealPlan) {
        return pxMealPlanMapper.selectPxMealPlanList(pxMealPlan);
    }


    /**
     * 新增餐饮计划
     *
     * @param pxMealPlan 餐饮计划
     * @return 结果
     */
    @Override
    public int insertPxMealPlan(PxMealPlan pxMealPlan) {
        pxMealPlan.setCreateTime(DateUtils.getNowDate());
        return pxMealPlanMapper.insertPxMealPlan(pxMealPlan);
    }

    /**
     * 修改餐饮计划
     *
     * @param pxMealPlan 餐饮计划
     * @return 结果
     */
    @Override
    public int updatePxMealPlan(PxMealPlan pxMealPlan) {
        pxMealPlan.setUpdateTime(DateUtils.getNowDate());
        return pxMealPlanMapper.updatePxMealPlan(pxMealPlan);
    }

    /**
     * 批量删除餐饮计划
     *
     * @param ids 需要删除的餐饮计划ID
     * @return 结果
     */
    @Override
    public int deletePxMealPlanByIds(Long[] ids) {
        return pxMealPlanMapper.deletePxMealPlanByIds(ids);
    }

    /**
     * 删除餐饮计划信息
     *
     * @param id 餐饮计划ID
     * @return 结果
     */
    @Override
    public int deletePxMealPlanById(Long id) {
        return pxMealPlanMapper.deletePxMealPlanById(id);
    }

    /**
     * 按日期范围查询餐饮计划（周视图）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 餐饮计划
     */
    @Override
    public List<PxMealPlan> selectByDateRange(String startDate, String endDate) {
        return pxMealPlanMapper.selectByDateRange(startDate, endDate);
    }

    /**
     * 把日期范围内餐饮计划关联菜谱的食材，批量写入指定购物清单
     *
     * @param listId    购物清单ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 结果
     */
    @Override
    public int transferToShopping(Long listId, String startDate, String endDate) {
        List<PxMealPlan> mealPlans = pxMealPlanMapper.selectByDateRange(startDate, endDate);
        List<PxShoppingItem> shoppingItems = new ArrayList<>();
        for (PxMealPlan mealPlan : mealPlans) {
            if (mealPlan.getRecipeId() == null) {
                continue;
            }
            List<PxRecipeIngredient> ingredients = pxRecipeIngredientMapper.selectByRecipeId(mealPlan.getRecipeId());
            for (PxRecipeIngredient ingredient : ingredients) {
                PxShoppingItem shoppingItem = new PxShoppingItem();
                shoppingItem.setListId(listId);
                shoppingItem.setName(ingredient.getName());
                shoppingItem.setQuantity(ingredient.getQuantity());
                shoppingItem.setClassificationId(ingredient.getClassificationId());
                shoppingItem.setChecked(false);
                shoppingItem.setAddedFromMeal(true);
                shoppingItem.setCreateTime(DateUtils.getNowDate());
                shoppingItems.add(shoppingItem);
            }
        }
        if (shoppingItems.isEmpty()) {
            return 0;
        }
        return pxShoppingItemMapper.insertBatch(shoppingItems);
    }
}
