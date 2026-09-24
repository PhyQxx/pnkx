package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.domain.po.PxAutomationRule;
import com.pnkx.service.IPxAutomationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/automation")
public class PxAutomationController extends BaseController {
    @Resource private IPxAutomationService automationService;

    @GetMapping("/rules") public AjaxResult rules() { return AjaxResult.success(automationService.listRules()); }
    @GetMapping("/templates") public AjaxResult templates() { return AjaxResult.success(automationService.templates()); }
    @PostMapping("/rules") public AjaxResult add(@RequestBody PxAutomationRule rule) { return AjaxResult.success(automationService.saveRule(rule)); }
    @PutMapping("/rules") public AjaxResult edit(@RequestBody PxAutomationRule rule) { return AjaxResult.success(automationService.saveRule(rule)); }
    @DeleteMapping("/rules/{id}") public AjaxResult delete(@PathVariable Long id) { return toAjax(automationService.deleteRule(id)); }
    @GetMapping("/executions") public AjaxResult executions(Long ruleId) { return AjaxResult.success(automationService.listExecutions(ruleId)); }

    @PostMapping("/rules/{id}/run")
    public AjaxResult run(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        JSONObject input = body == null ? new JSONObject() : new JSONObject(body);
        return AjaxResult.success(automationService.execute(id, false, input));
    }

    @PostMapping("/rules/{id}/dry-run")
    public AjaxResult dryRun(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        JSONObject input = body == null ? new JSONObject() : new JSONObject(body);
        return AjaxResult.success(automationService.execute(id, true, input));
    }

    @PostMapping("/executions/{id}/retry")
    public AjaxResult retry(@PathVariable Long id) { return AjaxResult.success(automationService.retry(id)); }
}
