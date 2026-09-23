package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxBookkeepingBudget;
import com.pnkx.service.IPxBookkeepingBudgetService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 记账预算
 *
 * @author PHY
 * @date 2026-09-23
 */
@RestController
@RequestMapping("/bookkeeping/budget")
public class PxBookkeepingBudgetController extends BaseController {

    @Resource
    private IPxBookkeepingBudgetService budgetService;

    /**
     * 查询某月预算配置列表
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam String month) {
        List<PxBookkeepingBudget> list = budgetService.listBudgets(month);
        return AjaxResult.success(list);
    }

    /**
     * 查询某月预算使用状态（已用/剩余/百分比/超支标记）
     */
    @GetMapping("/status")
    public AjaxResult status(@RequestParam String month) {
        List<PxBookkeepingBudget> list = budgetService.getBudgetStatus(month);
        return AjaxResult.success(list);
    }

    /**
     * 保存预算（同月同分类自动覆盖更新）
     */
    @Log(title = "记账预算", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@RequestBody PxBookkeepingBudget budget) {
        return toAjax(budgetService.saveBudget(budget));
    }

    /**
     * 删除预算
     */
    @Log(title = "记账预算", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(budgetService.deleteBudget(id));
    }
}
