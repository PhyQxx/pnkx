package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxSubscription;
import com.pnkx.service.IPxSubscriptionService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订阅管理 Controller
 *
 * @author PHY
 * @date 2026/07/05
 */
@RestController
@RequestMapping("/subscription")
public class PxSubscriptionController extends BaseController {

    @Resource
    private IPxSubscriptionService pxSubscriptionService;

    @GetMapping("/list")
    public TableDataInfo list(PxSubscription pxSubscription) {
        startPage();
        List<PxSubscription> list = pxSubscriptionService.selectPxSubscriptionList(pxSubscription);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxSubscriptionService.selectPxSubscriptionById(id));
    }

    @Log(title = "订阅管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxSubscription pxSubscription) {
        int rows = pxSubscriptionService.insertPxSubscription(pxSubscription);
        if (rows > 0) {
            return AjaxResult.success(pxSubscription.getId());
        }
        return AjaxResult.error();
    }

    @Log(title = "订阅管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxSubscription pxSubscription) {
        return toAjax(pxSubscriptionService.updatePxSubscription(pxSubscription));
    }

    @Log(title = "订阅管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxSubscriptionService.deletePxSubscriptionByIds(ids));
    }

    /**
     * 月度/年度预测汇总
     * <p>
     * 返回所有启用订阅的月均总额、年度总额，以及逐条明细。
     */
    @GetMapping("/forecast")
    public AjaxResult forecast() {
        PxSubscription query = new PxSubscription();
        query.setEnabled(true);
        List<PxSubscription> list = pxSubscriptionService.selectPxSubscriptionList(query);
        BigDecimal monthlyTotal = BigDecimal.ZERO;
        BigDecimal yearlyTotal = BigDecimal.ZERO;
        List<Map<String, Object>> details = new ArrayList<>();
        for (PxSubscription sub : list) {
            BigDecimal monthly = pxSubscriptionService.monthlyNormalized(sub);
            BigDecimal yearly = pxSubscriptionService.yearlyPredicted(sub);
            monthlyTotal = monthlyTotal.add(monthly);
            yearlyTotal = yearlyTotal.add(yearly);
            Map<String, Object> d = new HashMap<>(6);
            d.put("id", sub.getId());
            d.put("name", sub.getName());
            d.put("amount", sub.getAmount());
            d.put("cycle", sub.getCycle());
            d.put("cycleInterval", sub.getCycleInterval());
            d.put("monthly", monthly);
            d.put("yearly", yearly);
            d.put("nextPaymentDate", sub.getNextPaymentDate());
            details.add(d);
        }
        Map<String, Object> result = new HashMap<>(4);
        result.put("monthlyTotal", monthlyTotal);
        result.put("yearlyTotal", yearlyTotal);
        result.put("count", list.size());
        result.put("details", details);
        return AjaxResult.success(result);
    }
}
