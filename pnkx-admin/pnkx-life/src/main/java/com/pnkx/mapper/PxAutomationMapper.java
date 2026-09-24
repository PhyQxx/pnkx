package com.pnkx.mapper;

import com.pnkx.domain.po.PxAutomationExecution;
import com.pnkx.domain.po.PxAutomationRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PxAutomationMapper {
    List<PxAutomationRule> selectRules(@Param("userId") String userId);
    List<PxAutomationRule> selectDueRules(@Param("now") java.util.Date now);
    PxAutomationRule selectRule(@Param("id") Long id, @Param("userId") String userId);
    PxAutomationRule selectRuleByClientUuid(@Param("clientUuid") String clientUuid, @Param("userId") String userId);
    int insertRule(PxAutomationRule rule);
    int updateRule(PxAutomationRule rule);
    int deleteRule(@Param("id") Long id, @Param("userId") String userId);
    int updateRunTime(@Param("id") Long id, @Param("userId") String userId,
                      @Param("lastRunTime") java.util.Date lastRunTime,
                      @Param("nextRunTime") java.util.Date nextRunTime);
    List<PxAutomationExecution> selectExecutions(@Param("ruleId") Long ruleId, @Param("userId") String userId);
    PxAutomationExecution selectExecution(@Param("id") Long id, @Param("userId") String userId);
    PxAutomationExecution selectExecutionByKey(@Param("key") String key);
    int insertExecution(PxAutomationExecution execution);
    int updateExecution(PxAutomationExecution execution);
}
