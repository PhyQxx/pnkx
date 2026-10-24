package com.pnkx.life.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.life.domain.po.PxMealPlan;
import com.pnkx.life.domain.po.PxRecipeIngredient;
import com.pnkx.life.domain.po.PxShoppingItem;
import com.pnkx.life.domain.po.PxShoppingList;
import com.pnkx.life.mapper.PxMealPlanMapper;
import com.pnkx.life.mapper.PxRecipeIngredientMapper;
import com.pnkx.life.mapper.PxShoppingItemMapper;
import com.pnkx.life.mapper.PxShoppingListMapper;
import com.pnkx.life.service.IPxMealPlanService;
import org.springframework.stereotype.Service;
import com.pnkx.framework.web.service.DataPermissionService;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author PHY
 * @classname PxMealPlanServiceImpl
 * @data 2026/07/05
 * @description 餐饮计划Service实现
 */
@Service
public class PxMealPlanServiceImpl implements IPxMealPlanService {
    @Resource private ShoppingWorkflowService shoppingWorkflowService;
    @Resource
    private PxMealPlanMapper pxMealPlanMapper;

    @Resource
    private PxRecipeIngredientMapper pxRecipeIngredientMapper;

    @Resource
    private PxShoppingItemMapper pxShoppingItemMapper;
    @Resource
    private PxShoppingListMapper pxShoppingListMapper;
    @Resource private DataPermissionService dataPermissionService;
    @Resource private com.pnkx.life.mapper.PxRecipeMapper recipeMapper;

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
    @DataScopeSelf(module = "meal")
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
        validateInput(pxMealPlan);
        if (StringUtils.isNotEmpty(pxMealPlan.getClientUuid())) {
            PxMealPlan existing = pxMealPlanMapper.selectByClientUuid(
                    pxMealPlan.getClientUuid());
            if (existing != null) {
                requireOwner(existing);
                pxMealPlan.setId(existing.getId());
                return 1;
            }
        }
        pxMealPlan.setCreateBy(SecurityUtils.getUserId());
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
        validateInput(pxMealPlan);
        requireOwner(pxMealPlanMapper.selectPxMealPlanById(pxMealPlan.getId()));
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
        for (Long id : ids) requireOwner(pxMealPlanMapper.selectPxMealPlanById(id));
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
        requireOwner(pxMealPlanMapper.selectPxMealPlanById(id));
        return pxMealPlanMapper.deletePxMealPlanById(id);
    }

    private void validateInput(PxMealPlan meal) {
        if (meal.getServings() != null && (meal.getServings().signum() <= 0
                || meal.getServings().scale() > 3 || meal.getServings().compareTo(new java.math.BigDecimal("100000")) > 0)) {
            throw new ServiceException("用餐份数须大于0、最多3位小数且不超过100000");
        }
        if (meal.getRecipeId() != null) {
            var recipe = recipeMapper.selectPxRecipeById(meal.getRecipeId());
            var existing = meal.getId() == null ? null : pxMealPlanMapper.selectPxMealPlanById(meal.getId());
            boolean unchanged = existing != null && Objects.equals(existing.getRecipeId(), meal.getRecipeId())
                    && recipe != null && Objects.equals(existing.getCreateBy(), recipe.getCreateBy());
            if (recipe == null || "1".equals(recipe.getDelFlag()) || (!unchanged
                    && !Objects.equals(SecurityUtils.getUserId(), recipe.getCreateBy()))) {
                throw new ServiceException("菜谱不存在或无权使用");
            }
        }
    }

    private void requireOwner(PxMealPlan entity) {
        if (entity == null || !dataPermissionService.canWrite(entity.getCreateBy(), "meal")) {
            throw new ServiceException("记录不存在或无权操作");
        }
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
        String userId = SecurityUtils.getUserId();
        var preview=shoppingWorkflowService.preview(listId,startDate,endDate,userId);
        return shoppingWorkflowService.commit(preview.getString("previewId"),userId).getIntValue("created");
    }
}
