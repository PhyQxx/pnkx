package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxMealPlan;
import com.pnkx.service.IPxMealPlanService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxMealPlanController
 * @data 2026/07/05
 * @description 餐饮计划Controller
 */
@RestController
@RequestMapping("/mealPlan")
public class PxMealPlanController extends BaseController {
    @Resource
    private IPxMealPlanService pxMealPlanService;

    /**
     * 查询餐饮计划列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxMealPlan pxMealPlan) {
        startPage();
        List<PxMealPlan> list = pxMealPlanService.selectPxMealPlanList(pxMealPlan);
        return getDataTable(list);
    }

    /**
     * 获取餐饮计划详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxMealPlanService.selectPxMealPlanById(id));
    }

    /**
     * 周视图查询餐饮计划
     */
    @GetMapping("/week")
    public AjaxResult week(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate) {
        return AjaxResult.success(pxMealPlanService.selectByDateRange(startDate, endDate));
    }

    /**
     * 新增餐饮计划
     */
    @Log(title = "餐饮计划", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxMealPlan pxMealPlan) {
        int rows = pxMealPlanService.insertPxMealPlan(pxMealPlan);
        if (rows > 0) {
            return AjaxResult.success(pxMealPlan.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改餐饮计划
     */
    @Log(title = "餐饮计划", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxMealPlan pxMealPlan) {
        return toAjax(pxMealPlanService.updatePxMealPlan(pxMealPlan));
    }

    /**
     * 删除餐饮计划
     */
    @Log(title = "餐饮计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxMealPlanService.deletePxMealPlanByIds(ids));
    }

    /**
     * 把日期范围内餐饮计划关联菜谱的食材转入购物清单
     */
    @Log(title = "餐饮计划", businessType = BusinessType.INSERT)
    @PostMapping("/transferToShopping")
    public AjaxResult transferToShopping(@RequestParam("listId") Long listId, @RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate) {
        return toAjax(pxMealPlanService.transferToShopping(listId, startDate, endDate));
    }
}
