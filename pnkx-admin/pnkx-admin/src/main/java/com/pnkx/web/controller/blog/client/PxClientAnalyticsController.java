package com.pnkx.web.controller.blog.client;

import com.pnkx.blog.domain.po.PxAnalyticsEvent;
import com.pnkx.blog.mapper.PxAnalyticsMapper;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.web.service.tools.ToolboxRateLimiter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/client/analytics")
public class PxClientAnalyticsController {
    @Resource private PxAnalyticsMapper mapper;
    @Resource private ToolboxRateLimiter limiter;
    private static final Set<String> EVENTS = Set.of("login_submit","login_success","login_fail","reply_submit","reply_success","reply_fail","read_25","read_50","read_75","read_100","video_select","video_start","video_25","video_50","video_75","video_complete","video_error","page_view","page_duration","search_open","search_submit","search_success","search_fail","search_click","comment_submit","comment_success","comment_fail","ai_open","ai_submit","ai_success","ai_fail","ai_stop");
    private static final Set<String> PAGES = Set.of("home","post_detail","category","category_detail","archives","video","video_detail","album","message","link","share","login","user","other");
    private static boolean uuid(String s) { return s != null && s.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"); }
    public static void validate(PxAnalyticsEvent e, long now) {
        boolean valid = e != null && uuid(e.getEventId()) && uuid(e.getVisitorId()) && uuid(e.getSessionId()) && uuid(e.getPageViewId())
            && e.getEventName() != null && EVENTS.contains(e.getEventName()) && e.getPageKey() != null && PAGES.contains(e.getPageKey())
            && (e.getOperationId() == null || uuid(e.getOperationId()))
            && (e.getTargetId() == null || e.getTargetId().matches("[0-9]{1,20}"))
            && (e.getContentId() == null || e.getContentId().matches("[0-9]{1,20}"))
            && (e.getReferrerHost() == null || (e.getReferrerHost().length() <= 253 && e.getReferrerHost().matches("[a-zA-Z0-9.-]+")))
            && e.getOccurredAt() != null && e.getOccurredAt() >= now-86400000 && e.getOccurredAt() <= now+300000
            && e.getDurationMs() != null && e.getDurationMs() >= 0 && e.getDurationMs() <= 60000
            && ("page_duration".equals(e.getEventName()) || e.getDurationMs() == 0);
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid analytics event");
    }
    @PostMapping("/events")
    @Transactional
    public AjaxResult receive(@RequestBody List<PxAnalyticsEvent> events, HttpServletRequest request) {
        if (events == null || events.isEmpty() || events.size() > 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Batch size must be 1..20");
        if (!limiter.tryAcquire("analytics:ip:"+IpUtils.getIpAddr(request),120,60)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        long now = System.currentTimeMillis();
        events.forEach(e -> validate(e,now));
        String userId = null;
        try { userId = String.valueOf(SecurityUtils.getUserId()); } catch (Exception ignored) { }
        for (PxAnalyticsEvent e : events) {
            e.setUserId(userId);
            e.setEventTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(e.getOccurredAt()),ZoneId.of("Asia/Shanghai")));
            mapper.insert(e);
        }
        return AjaxResult.success();
    }
}
