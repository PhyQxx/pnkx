package com.pnkx.web.service.intent;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.service.impl.AssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AssignmentValidationTest {
    JdbcTemplate db=mock(JdbcTemplate.class);
    AssignmentService service=new AssignmentService(db);
    JSONObject draft() { return new JSONObject().fluentPut("assignee","1").fluentPut("expectedAssignee","2").fluentPut("reason","调整分工"); }
    void source(String state) {
        var row=new HashMap<String,Object>(); row.put("owner","1"); row.put("assignee","2"); row.put("state",state);
        when(db.queryForList(contains("select i.id"),eq(9L))).thenReturn(List.of(row));
    }
    @Test void validPreviewHasNoWrites() {
        source("needed"); service.validateTransfer("shopping",9L,"1",draft(),false);
        verify(db,never()).update(anyString(),any(Object[].class));
    }
    @Test void changedAssigneeRejects() {
        source("needed"); var input=draft().fluentPut("expectedAssignee","3");
        assertThrows(ServiceException.class,()->service.validateTransfer("shopping",9L,"1",input,false));
    }
    @Test void completedShoppingRejects() {
        source("purchased"); assertThrows(ServiceException.class,()->service.validateTransfer("shopping",9L,"1",draft(),false));
    }
    @Test void inactiveTargetRejects() {
        source("needed"); var input=draft().fluentPut("assignee","8");
        assertThrows(ServiceException.class,()->service.validateTransfer("shopping",9L,"1",input,false));
    }
    @Test void reasonRequired() {
        source("needed"); var input=draft().fluentPut("reason","");
        assertThrows(ServiceException.class,()->service.validateTransfer("shopping",9L,"1",input,false));
    }
}
