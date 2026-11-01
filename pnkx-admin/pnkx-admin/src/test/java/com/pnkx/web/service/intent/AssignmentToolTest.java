package com.pnkx.web.service.intent;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.service.impl.AssignmentService;
import com.pnkx.web.service.operation.AssignmentTool;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AssignmentToolTest {
    AssignmentService service=mock(AssignmentService.class);
    AssignmentTool tool=new AssignmentTool(service);
    JSONObject draft() { return JSONObject.parseObject("{\"sourceType\":\"shopping\",\"sourceId\":1,\"assignee\":\"2\",\"reason\":\"调整分工\"}"); }
    @Test void previewCapturesAssigneeWithoutWriting() {
        when(service.detail("shopping",1L,"1",1)).thenReturn(new JSONObject().fluentPut("canTransfer",true).fluentPut("assignee","3"));
        var draft=draft(); tool.prepare(draft,"1");
        assertEquals("3",draft.getString("expectedAssignee"));
        verify(service).validateTransfer("shopping",1L,"1",draft,false);
        verify(service,never()).transfer(anyString(),anyLong(),anyString(),any());
    }
    @Test void unassignedMarkerSurvivesPersistence() {
        when(service.detail("shopping",1L,"1",1)).thenReturn(new JSONObject().fluentPut("canTransfer",true));
        var draft=draft(); tool.prepare(draft,"1");
        var restored=JSONObject.parseObject(draft.toJSONString());
        assertEquals("",restored.getString("expectedAssignee"));
        tool.validate(restored,"1",false);
        verify(service,times(2)).validateTransfer(eq("shopping"),eq(1L),eq("1"),argThat(input -> input.containsKey("expectedAssignee") && input.getString("expectedAssignee")==null),eq(false));
    }
    @Test void readonlyUserCannotPrepareTransfer() {
        when(service.detail("shopping",1L,"1",1)).thenReturn(new JSONObject().fluentPut("canTransfer",false));
        assertThrows(ServiceException.class,()->tool.prepare(draft(),"1"));
        verify(service,never()).transfer(anyString(),anyLong(),anyString(),any());
    }
    @Test void missingSourceNeverQueries() {
        assertThrows(ServiceException.class,()->tool.detail(new JSONObject(),"1")); verifyNoInteractions(service);
    }
    @Test void executeUsesAuthorizedServiceAndPreservesEntityType() {
        when(service.transfer(eq("shopping"),eq(1L),eq("1"),any())).thenReturn(new JSONObject().fluentPut("assignee","2"));
        var result=tool.run(draft(),"1"); assertEquals("assignment_mutation",result.getString("sourceType"));
        assertEquals("shopping",result.getString("entityType"));
    }
}
