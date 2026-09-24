package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxBookkeepingRecurring;
import com.pnkx.service.impl.PxBookkeepingRecurringService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 周期记账规则
 *
 * @author PHY
 * @date 2026-09-23
 */
@RestController
@RequestMapping("/bookkeeping/recurring")
public class PxBookkeepingRecurringController extends BaseController {

    @Resource
    private PxBookkeepingRecurringService recurringService;

    /**
     * 当前用户的规则列表
     */
    @GetMapping("/list")
    public AjaxResult list() {
        return AjaxResult.success(recurringService.listMine());
    }

    /**
     * 新增规则（下次执行日自动计算，首次不早于明天）
     */
    @Log(title = "周期记账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxBookkeepingRecurring recurring) {
        int rows = recurringService.add(recurring);
        return rows > 0 ? AjaxResult.success(recurring.getId()) : AjaxResult.error();
    }

    /**
     * 修改规则
     */
    @Log(title = "周期记账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult update(@RequestBody PxBookkeepingRecurring recurring) {
        return toAjax(recurringService.update(recurring));
    }

    /**
     * 启停规则
     */
    @Log(title = "周期记账", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}")
    public AjaxResult toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return toAjax(recurringService.toggle(id, Boolean.TRUE.equals(body.get("enabled"))));
    }

    /**
     * 删除规则
     */
    @Log(title = "周期记账", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(recurringService.delete(id));
    }
}
