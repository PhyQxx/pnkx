package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AiPendingActionServiceTest {

    @Test
    public void confirmationCommandsAcceptCommonUserReplies() {
        assertTrue(AiPendingActionService.isConfirmCommand("确认"));
        assertTrue(AiPendingActionService.isConfirmCommand("确认保存。"));
        assertTrue(AiPendingActionService.isConfirmCommand("ok"));
        assertFalse(AiPendingActionService.isConfirmCommand("重新记一笔午饭"));
    }

    @Test
    public void cancelCommandsAcceptCommonUserReplies() {
        assertTrue(AiPendingActionService.isCancelCommand("取消"));
        assertTrue(AiPendingActionService.isCancelCommand("不保存！"));
        assertFalse(AiPendingActionService.isCancelCommand("保存"));
    }

    @Test
    public void saveAndClearCurrentPendingAction() {
        AiPendingActionService service = new AiPendingActionService();
        JSONObject draft = new JSONObject().fluentPut("money", "28");

        AiPendingActionService.PendingAction action = service.save("test-request-id", "bookkeeping", draft);

        assertNotNull(action.id());
        assertEquals("test-request-id", action.requestId());
        assertTrue(action.expireAt().isAfter(action.createdAt()));
        assertEquals("bookkeeping", service.getCurrent().intent());
        assertEquals("28", service.getCurrent().draft().getString("money"));

        service.clearCurrent();
        assertNull(service.getCurrent());
    }
}
