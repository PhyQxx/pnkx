package com.pnkx.web.service;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.life.mapper.PxToDoMapper;
import com.pnkx.life.service.impl.ShoppingTodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShoppingTodoServiceTest {
    ShoppingTodoService service;
    JdbcTemplate db;
    PxToDoMapper mapper;
    DataPermissionService permissions;
    PxToDo todo;
    @BeforeEach void setup() {
        service=new ShoppingTodoService(); db=mock(JdbcTemplate.class); mapper=mock(PxToDoMapper.class);
        permissions=mock(DataPermissionService.class);
        ReflectionTestUtils.setField(service,"db",db);
        ReflectionTestUtils.setField(service,"mapper",mapper);
        ReflectionTestUtils.setField(service,"permissions",permissions);
        todo=new PxToDo();todo.setId(1L);todo.setCreateBy("7");todo.setLabel("生活,采购");todo.setStatus(false);
        when(mapper.lockById(1L)).thenReturn(todo);
        when(permissions.canWrite("7","todo")).thenReturn(true);
        when(permissions.canWrite("7","shopping")).thenReturn(true);
        when(db.queryForList("select create_by,del_flag from px_shopping_item where id=? for update",2L)).thenReturn(List.of(Map.of("create_by","7","del_flag","0")));
    }
    @Test void exactFixedLabel() {
        assertTrue(ShoppingTodoService.hasLabel("工作, 采购 "));
        assertFalse(ShoppingTodoService.hasLabel("采购计划"));
        assertFalse(ShoppingTodoService.hasLabel(null));
    }
    @Test void rejectsNormalOrCompletedTodo() {
        todo.setLabel("生活");
        assertThrows(ServiceException.class,()->service.arrange(1L,null,null,List.of(2L)));
        todo.setLabel("采购");todo.setStatus(true);
        assertThrows(ServiceException.class,()->service.arrange(1L,null,null,List.of(2L)));
        verifyNoInteractions(db);
    }
    @Test void requiresBothPermissions() {
        when(permissions.canWrite("7","shopping")).thenReturn(false);
        assertThrows(ServiceException.class,()->service.arrange(1L,null,null,List.of(2L)));
        verifyNoInteractions(db);
    }
    @Test void rejectsDifferentOwner() {
        when(db.queryForList("select create_by,del_flag from px_shopping_item where id=? for update",2L)).thenReturn(List.of(Map.of("create_by","8","del_flag","0")));
        assertThrows(ServiceException.class,()->service.arrange(1L,null,null,List.of(2L)));
    }
    @Test void duplicateSameLinkIsIdempotentButOtherTodoRejected() {
        when(db.queryForList("select todo_id from px_shopping_todo_link where item_id=?",2L)).thenReturn(List.of(Map.of("todo_id",1L)));
        assertEquals(1L,service.arrange(1L,null,null,List.of(2L,2L)));
        verify(db,never()).update(anyString(),any(),any());
        when(db.queryForList("select todo_id from px_shopping_todo_link where item_id=?",2L)).thenReturn(List.of(Map.of("todo_id",3L)));
        assertThrows(ServiceException.class,()->service.arrange(1L,null,null,List.of(2L)));
    }
    @Test void createsLink() {
        assertEquals(1L,service.arrange(1L,null,null,List.of(2L)));
        verify(db).update("insert into px_shopping_todo_link(item_id,todo_id) values (?,?)",2L,1L);
    }
    @Test void todoCreatesItemsAndLinksIntoExistingList() {
        var items=mock(com.pnkx.life.mapper.PxShoppingItemMapper.class);
        ReflectionTestUtils.setField(service,"shoppingItems",items);
        when(db.queryForList("select create_by,del_flag from px_shopping_list where id=? for update",9L)).thenReturn(List.of(Map.of("create_by","7","del_flag","0")));
        when(items.insertPxShoppingItem(any())).thenAnswer(invocation->{
            com.pnkx.life.domain.po.PxShoppingItem item=invocation.getArgument(0);
            assertEquals("7",item.getCreateBy());assertEquals(9L,item.getListId());
            assertEquals("牛奶",item.getName());item.setId(22L);return 1;
        });
        assertEquals(9L,service.createShopping(1L,9L,List.of(" 牛奶 ")));
        verify(db).update("insert into px_shopping_todo_link(item_id,todo_id) values (?,?)",22L,1L);
    }
    @Test void rejectsCreationInOtherOwnersListAndEmptyNames() {
        when(db.queryForList("select create_by,del_flag from px_shopping_list where id=? for update",9L)).thenReturn(List.of(Map.of("create_by","8","del_flag","0")));
        assertThrows(ServiceException.class,()->service.createShopping(1L,9L,List.of("牛奶")));
        assertThrows(ServiceException.class,()->service.createShopping(1L,9L,List.of(" ")));
    }
}
