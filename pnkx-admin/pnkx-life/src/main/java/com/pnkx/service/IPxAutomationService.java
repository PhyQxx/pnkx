package com.pnkx.service;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.domain.po.PxAutomationExecution;
import com.pnkx.domain.po.PxAutomationRule;

import java.util.List;

public interface IPxAutomationService {
    List<PxAutomationRule> listRules();
    List<JSONObject> templates();
    PxAutomationRule saveRule(PxAutomationRule rule);
    int deleteRule(Long id);
    PxAutomationExecution execute(Long ruleId, boolean dryRun, JSONObject input);
    PxAutomationExecution retry(Long executionId);
    List<PxAutomationExecution> listExecutions(Long ruleId);
    List<PxAutomationExecution> trigger(String triggerType, JSONObject input);
    int executeDueRules();
}
