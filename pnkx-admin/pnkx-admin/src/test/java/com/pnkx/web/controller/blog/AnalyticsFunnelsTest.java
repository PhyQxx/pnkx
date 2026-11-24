package com.pnkx.web.controller.blog;
import com.pnkx.blog.service.AnalyticsFunnels;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AnalyticsFunnelsTest {
    Map<String,Object> e(String name,String time,String op,String page) {
        Map<String,Object> e=new HashMap<>();e.put("visitorId","v");e.put("sessionId","s");e.put("pageViewId",page);e.put("pageKey","post_detail");e.put("eventName",name);e.put("time",time);e.put("operationId",op);return e;
    }
    Map<String,Object> result(String funnel,Map<String,Object>... rows) {
        return AnalyticsFunnels.calculate(Arrays.asList(rows)).stream().filter(r->funnel.equals(r.get("funnel"))).findFirst().orElseThrow();
    }
    @Test void searchRequiresSameOperation() {
        var r=result("search",e("search_submit","1","a","p"),e("search_success","2","a","p"),e("search_click","3","b","p"));
        assertEquals(1,r.get("second"));assertEquals(0,r.get("third"));
    }
    @Test void outcomesBeforeSubmitDoNotConvert() {
        var r=result("ai",e("ai_open","1",null,"p"),e("ai_success","2","a","p"),e("ai_submit","3","a","p"));
        assertEquals(0,r.get("third"));
    }
    @Test void commentMustUseReadPage() {
        var r=result("article",e("read_50","1",null,"p"),e("comment_submit","2","a","other"),e("comment_success","3","a","other"));
        assertEquals(1,r.get("first"));assertEquals(0,r.get("second"));
    }
    @Test void duplicatesAndEqualTimestampsCountOnce() {
        var r=result("search",e("search_click","1","a","p"),e("search_submit","1","a","p"),e("search_success","1","a","p"),e("search_click","1","a","p"));
        assertEquals(1,r.get("first"));assertEquals(1,r.get("third"));
    }
    @Test void sessionsNeverJoin() {
        var success=e("ai_success","3","a","p");success.put("sessionId","other");
        var r=result("ai",e("ai_open","1",null,"p"),e("ai_submit","2","a","p"),success);
        assertEquals(0,r.get("third"));
    }
    @Test void validArticleConversion() {
        var r=result("article",e("read_50","1",null,"p"),e("comment_submit","2","a","p"),e("comment_success","3","a","p"));
        assertEquals(1,r.get("third"));
    }
}
