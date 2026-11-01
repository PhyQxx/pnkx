package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.PxShoppingItem;
import com.pnkx.life.mapper.PxShoppingItemMapper;
import com.pnkx.life.service.impl.ShoppingActionService;
import com.pnkx.web.service.operation.ShoppingManageTool;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ShoppingManageToolTest {
    JdbcTemplate db=mock(JdbcTemplate.class);
    DataPermissionService permissions=mock(DataPermissionService.class);
    PxShoppingItemMapper items=mock(PxShoppingItemMapper.class);
    ShoppingActionService actions=mock(ShoppingActionService.class);
    ShoppingManageTool tool=new ShoppingManageTool(db,permissions,items,actions);
    JSONObject input() { return new JSONObject().fluentPut("itemId",1L).fluentPut("expectedRevision",3L).fluentPut("action","claim"); }
    PxShoppingItem item() {
        var item=new PxShoppingItem(); item.setId(1L); item.setListId(2L); item.setRevision(3L); item.setDelFlag("0"); item.setFulfillmentState("needed");
        when(items.selectPxShoppingItemById(1L)).thenReturn(item); return item;
    }
    @Test void previewNeverMutates() {
        item(); tool.validate(input(),"1",false);
        verify(actions,never()).act(anyLong(),anyString(),any(),anyString());
        verify(db,never()).update(anyString(),any(Object[].class));
    }
    @Test void changedRevisionRejectsExecution() {
        item().setRevision(4L);
        assertThrows(ServiceException.class,()->tool.run(input(),"1"));
        verify(actions,never()).act(anyLong(),anyString(),any(),anyString());
    }
    @Test void processedItemCannotBeClaimed() {
        item().setFulfillmentState("purchased");
        assertThrows(ServiceException.class,()->tool.validate(input(),"1",false));
    }
    @Test void settledItemCannotChangeState() {
        item().setBatchId(8L);
        var input=input().fluentPut("action","fulfillment").fluentPut("state","needed");
        assertThrows(ServiceException.class,()->tool.validate(input,"1",false));
    }
    @Test void otherClaimCannotBeTaken() {
        item().setClaimedBy("2");
        assertThrows(ServiceException.class,()->tool.validate(input(),"1",false));
    }
    @Test void accessDeniedNeverExecutes() {
        item(); doThrow(new ServiceException("无权操作")).when(actions).checkAccess(1L);
        assertThrows(ServiceException.class,()->tool.run(input(),"1"));
        verify(actions,never()).act(anyLong(),anyString(),any(),anyString());
    }
    @Test void successfulExecutionDelegatesToBusinessService() {
        var item=item(); when(actions.act(eq(1L),eq("claim"),any(),eq("1"))).thenReturn(item);
        assertEquals("shopping_mutation",tool.run(input(),"1").getString("sourceType"));
        verify(actions,times(1)).act(eq(1L),eq("claim"),any(),eq("1"));
    }
    @Test void noVisibleOwnersReturnsNothing() {
        when(permissions.getVisibleUserIds("shopping")).thenReturn(List.of());
        assertTrue(tool.find(new JSONObject()).isEmpty()); verifyNoInteractions(db);
    }
    @Test void ambiguousSelectionDoesNotPrepareWrite() {
        when(permissions.getVisibleUserIds("shopping")).thenReturn(null);
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("id",1L),Map.of("id",2L)));
        assertThrows(ServiceException.class,()->tool.prepare(new JSONObject().fluentPut("keyword","牛奶"),"1"));
        verifyNoInteractions(actions);
    }
}
