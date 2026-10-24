package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.IPxAiOperationLogService;
import com.pnkx.web.controller.tool.AiController;
import com.pnkx.web.service.intent.*;
import com.pnkx.web.service.operation.OperationPlanService;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LocalIntentPlanTest {
  @Test
  void allFiveConfirmHandlersUseExistingPlan() throws Exception {
    var adapter = mock(LocalIntentPlanService.class);
    var draft = JSON.parseObject("{\"action\":\"create\",\"operationPlanId\":\"p1\"}");
    var out = new ByteArrayOutputStream();
    when(adapter.confirm(draft, out)).thenReturn(true);
    for (var handler :
        new ConfirmableIntentHandler[] {
          new NoteHandler(),
          new DiaryWriteHandler(),
          new CommemorationDayHandler(),
          new TodoHandler(),
          new ShoppingListHandler()
        }) {
      ReflectionTestUtils.setField(handler, "localPlans", adapter);
      assertTrue(handler.confirm(draft, out));
    }
    verify(adapter, times(5)).confirm(draft, out);
  }

  @Test
  void cancellationCancelsDurablePlanBeforeClearingDraft() {
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUserId).thenReturn("1");
      var controller = new AiController();
      var pending = mock(AiPendingActionService.class);
      var plans = mock(OperationPlanService.class);
      var log = mock(IPxAiOperationLogService.class);
      ReflectionTestUtils.setField(controller, "pendingActionService", pending);
      ReflectionTestUtils.setField(controller, "operationPlans", plans);
      ReflectionTestUtils.setField(controller, "aiOperationLogService", log);
      when(pending.getCurrent())
          .thenReturn(
              new AiPendingActionService.PendingAction(
                  "d",
                  "r",
                  "note",
                  JSON.parseObject("{\"operationPlanId\":\"p1\"}"),
                  Instant.now(),
                  Instant.now().plusSeconds(600)));
      ReflectionTestUtils.invokeMethod(
          controller, "cancelPendingAction", new ByteArrayOutputStream());
      var order = inOrder(plans, pending);
      order.verify(plans).cancel("p1", "1");
      order.verify(pending).clearCurrent();
      reset(pending);
      when(pending.getCurrent())
          .thenReturn(
              new AiPendingActionService.PendingAction(
                  "d",
                  "r",
                  "note",
                  JSON.parseObject("{\"operationPlanId\":\"p1\"}"),
                  Instant.now(),
                  Instant.now().plusSeconds(600)));
      when(plans.cancel("p1", "1")).thenThrow(new IllegalStateException("already running"));
      assertThrows(
          Exception.class,
          () ->
              ReflectionTestUtils.invokeMethod(
                  controller, "cancelPendingAction", new ByteArrayOutputStream()));
      verify(pending, never()).clearCurrent();
    }
  }
}
