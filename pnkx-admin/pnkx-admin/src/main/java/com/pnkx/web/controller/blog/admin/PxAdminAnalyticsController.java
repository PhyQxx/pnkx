package com.pnkx.web.controller.blog.admin;

import com.pnkx.blog.mapper.PxAnalyticsMapper;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/admin/analytics")
@PreAuthorize("@ss.hasPermi('admin:visits:list')")
public class PxAdminAnalyticsController extends BaseController {
    @Resource private PxAnalyticsMapper mapper;
    private Map<String,Object> query(Map<String,String> params) {
        try {
            LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
            LocalDate start=LocalDate.parse(params.getOrDefault("startDate",today.minusDays(6).toString()));
            LocalDate end=LocalDate.parse(params.getOrDefault("endDate",today.toString())).plusDays(1);
            if (!end.isAfter(start) || end.isAfter(start.plusDays(31))) throw new IllegalArgumentException();
            Map<String,Object> q=new HashMap<>();
            q.put("start",start.atStartOfDay()); q.put("end",end.atStartOfDay());
            for (String key: List.of("pageKey","visitorId","sessionId")) {
                String value=params.get(key);
                if (value != null && value.length()>40) throw new IllegalArgumentException();
                q.put(key,value);
            }
            return q;
        } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid filters; date range maximum 31 days"); }
    }
    @GetMapping("/report")
    public AjaxResult report(@RequestParam Map<String,String> params) {
        Map<String,Object> q=query(params), result=new HashMap<>();
        result.put("summary",mapper.summary(q)); result.put("pages",mapper.pages(q)); result.put("features",mapper.features(q));
        return AjaxResult.success(result);
    }
    @GetMapping("/events")
    public TableDataInfo events(@RequestParam Map<String,String> params, @RequestParam(defaultValue="1") int pageNum, @RequestParam(defaultValue="20") int pageSize) {
        Map<String,Object> q=query(params);
        if (pageNum<1 || pageNum>10000 || pageSize<1 || pageSize>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        setPage(new Page<>(pageNum,pageSize));
        try { return getDataTable(mapper.events(q)); } finally { clearPage(); }
    }
}
