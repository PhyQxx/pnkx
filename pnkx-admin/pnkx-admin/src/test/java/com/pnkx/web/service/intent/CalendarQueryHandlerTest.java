package com.pnkx.web.service.intent;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.service.CalendarAggregateService;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class CalendarQueryHandlerTest {
    JSONObject dates(String start,String end) { return new JSONObject().fluentPut("startDate",start).fluentPut("endDate",end); }
    @Test void allowsExactlyNinetyDays() { assertEquals(2,CalendarQueryHandler.range(dates("2026-01-01","2026-03-31")).length); }
    @Test void rejectsMoreThanNinetyDays() { assertThrows(ServiceException.class,()->CalendarQueryHandler.range(dates("2026-01-01","2026-04-01"))); }
    @Test void rejectsReversedOrInvalidDates() {
        assertThrows(ServiceException.class,()->CalendarQueryHandler.range(dates("2026-02-02","2026-02-01")));
        assertThrows(ServiceException.class,()->CalendarQueryHandler.range(dates("2026-02-30","2026-03-01")));
        assertThrows(ServiceException.class,()->CalendarQueryHandler.range(new JSONObject()));
    }
    @Test void malformedModelDatesNeverCallDataService() throws Exception {
        var handler=new CalendarQueryHandler(); var ai=mock(AiClient.class); var calendar=mock(CalendarAggregateService.class);
        ReflectionTestUtils.setField(handler,"ai",ai); ReflectionTestUtils.setField(handler,"calendar",calendar);
        when(ai.chat(anyString(),anyString())).thenReturn(new JSONObject().fluentPut("content","{}"));
        var out=new ByteArrayOutputStream(); handler.handle("查询日历",new JSONObject(),out);
        assertTrue(out.toString().contains("请提供有效的起止日期")); verifyNoInteractions(calendar);
    }
}
