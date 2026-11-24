package com.pnkx.blog.service;

import java.util.*;

/** Ordered event funnels; outcomes must belong to the same operation. */
public final class AnalyticsFunnels {
    private AnalyticsFunnels() {}
    private static int rank(String name) {
        if (Set.of("ai_open","read_50","search_submit").contains(name)) return 0;
        if (Set.of("ai_submit","comment_submit","search_success").contains(name)) return 1;
        return 2;
    }
    public static List<Map<String,Object>> calculate(List<Map<String,Object>> input) {
        List<Map<String,Object>> rows=new ArrayList<>(input);
        rows.sort(Comparator.comparing((Map<String,Object> e)->String.valueOf(e.get("time")))
            .thenComparingInt(e->rank(String.valueOf(e.get("eventName")))));
        Set<String> opened=new HashSet<>(), read=new HashSet<>();
        Set<String> searchStarted=new HashSet<>(), searchReady=new HashSet<>(), aiSent=new HashSet<>(), commentSent=new HashSet<>();
        Map<String,List<Set<String>>> counts=new LinkedHashMap<>();
        for(String name:List.of("search","article","ai")) counts.put(name,List.of(new HashSet<>(),new HashSet<>(),new HashSet<>()));
        for(Map<String,Object> e:rows) {
            String visitor=String.valueOf(e.get("visitorId")), session=visitor+":"+e.get("sessionId");
            String page=session+":"+e.get("pageViewId"), name=String.valueOf(e.get("eventName"));
            Object operation=e.get("operationId");
            String op=operation==null?null:session+":"+operation;
            if (name.equals("ai_open")) { opened.add(session); counts.get("ai").get(0).add(session); }
            if (name.equals("read_50") && "post_detail".equals(e.get("pageKey"))) { read.add(page); counts.get("article").get(0).add(session); }
            if(op==null) continue;
            switch(name) {
                case "search_submit": searchStarted.add(op); counts.get("search").get(0).add(session); break;
                case "search_success": if(searchStarted.contains(op)) {searchReady.add(op);counts.get("search").get(1).add(session);} break;
                case "search_click": if(searchReady.contains(op)) counts.get("search").get(2).add(session); break;
                case "ai_submit": if(opened.contains(session)) {aiSent.add(op);counts.get("ai").get(1).add(session);} break;
                case "ai_success": if(aiSent.contains(op)) counts.get("ai").get(2).add(session); break;
                case "comment_submit": if(read.contains(page)) {commentSent.add(op+":"+page);counts.get("article").get(1).add(session);} break;
                case "comment_success": if(commentSent.contains(op+":"+page)) counts.get("article").get(2).add(session); break;
                default: break;
            }
        }
        List<Map<String,Object>> result=new ArrayList<>();
        counts.forEach((name, stages)->result.add(Map.of("funnel",name,"first",stages.get(0).size(),"second",stages.get(1).size(),"third",stages.get(2).size())));
        return result;
    }
}
