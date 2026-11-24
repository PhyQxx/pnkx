package com.pnkx.blog.mapper;
import com.pnkx.blog.domain.po.PxAnalyticsEvent;
import java.util.List;
import java.util.Map;
public interface PxAnalyticsMapper {
    List<Map<String, Object>> conversions(Map<String, Object> query);
    List<Map<String, Object>> funnelEvents(Map<String, Object> query);
    int insert(PxAnalyticsEvent event);
    Map<String, Object> summary(Map<String, Object> query);
    List<Map<String, Object>> pages(Map<String, Object> query);
    List<Map<String, Object>> features(Map<String, Object> query);
    List<Map<String, Object>> events(Map<String, Object> query);
}
