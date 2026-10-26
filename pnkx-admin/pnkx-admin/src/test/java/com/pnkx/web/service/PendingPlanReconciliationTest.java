package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.common.core.redis.RedisCache;
import com.pnkx.life.service.IPxAiOperationLogService;
import com.pnkx.web.service.intent.AiPendingActionService;
import com.pnkx.web.service.operation.OperationPlanService;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class PendingPlanReconciliationTest {
  RedisCache cache;
  OperationPlanService plans;
  IPxAiOperationLogService logs;
  AiPendingActionService service;
  AiPendingActionService.PendingAction action;

  @BeforeEach
  void setup() {
    cache = mock(RedisCache.class);
    plans = mock(OperationPlanService.class);
    logs = mock(IPxAiOperationLogService.class);
    service = new AiPendingActionService();
    ReflectionTestUtils.setField(service, "redisCache", cache);
    ReflectionTestUtils.setField(service, "operationPlans", plans);
    ReflectionTestUtils.setField(service, "operationLogs", logs);
    action =
        new AiPendingActionService.PendingAction(
            "draft",
            "request",
            "todo",
            JSON.parseObject("{\"operationPlanId\":\"old\"}"),
            Instant.now(),
            Instant.now().plusSeconds(600));
    when(cache.getCacheObject(anyString())).thenReturn(action);
  }

  @Test
  void completedPlanWritesReceiptBeforeConditionalRemoval() {
    when(plans.get("old", "anonymous"))
        .thenReturn(JSON.parseObject("{\"id\":\"old\",\"status\":\"succeeded\"}"));
    assertNull(service.getCurrent());
    var order = inOrder(logs, cache);
    order
        .verify(logs)
        .finishWriteDetail(
            eq("request"), eq("confirmed"), anyString(), contains("succeeded"), isNull(), isNull());
    order.verify(cache).deleteIfValueMatches(anyString(), same(action));
    verify(cache, never()).deleteObject(anyString());
  }

  @Test
  void replacedPlanIsRetiredWithoutExecutingOrReadingReplacement() {
    when(plans.get("old", "anonymous"))
        .thenReturn(
            JSON.parseObject(
                "{\"id\":\"old\",\"status\":\"cancelled\",\"replacement_id\":\"new\"}"));
    assertNull(service.getCurrent());
    verify(logs)
        .finishWriteDetail(
            eq("request"),
            eq("cancelled"),
            anyString(),
            contains("replacement_id"),
            isNull(),
            isNull());
    verify(plans).get("old", "anonymous");
    verifyNoMoreInteractions(plans);
  }

  @Test
  void unfinishedPlansKeepTheirConfirmationContext() {
    for (String status :
        new String[] {"awaiting_confirmation", "running", "failed", "partially_succeeded"}) {
      when(plans.get("old", "anonymous"))
          .thenReturn(JSON.parseObject("{\"status\":\"" + status + "\"}"));
      assertSame(action, service.getCurrent());
    }
    verifyNoInteractions(logs);
    verify(cache, never()).deleteIfValueMatches(anyString(), any());
  }

  @Test
  void committedOperationIsNotReportedFailedWhenSyncFails() {
    var controller = new com.pnkx.web.controller.life.PxOperationPlanController(plans);
    var pending = mock(AiPendingActionService.class);
    ReflectionTestUtils.setField(controller, "pending", pending);
    when(pending.getCurrent()).thenThrow(new IllegalStateException("cache unavailable"));
    try (var security = mockStatic(com.pnkx.common.utils.SecurityUtils.class)) {
      security.when(com.pnkx.common.utils.SecurityUtils::getUserId).thenReturn("1");
      var result = JSON.parseObject("{\"id\":\"old\",\"status\":\"succeeded\"}");
      when(plans.execute("old", "1")).thenReturn(result);
      assertSame(result, controller.run("old").get("data"));
    }
  }

  @Test
  void logFailureKeepsContextForLaterRetry() {
    when(plans.get("old", "anonymous")).thenReturn(JSON.parseObject("{\"status\":\"succeeded\"}"));
    doThrow(new IllegalStateException("log unavailable"))
        .when(logs)
        .finishWriteDetail(anyString(), anyString(), anyString(), anyString(), isNull(), isNull());
    assertThrows(IllegalStateException.class, () -> service.getCurrent());
    verify(cache, never()).deleteIfValueMatches(anyString(), any());
  }
}
