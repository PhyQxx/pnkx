package com.pnkx.blog.service;
import com.pnkx.blog.mapper.PxAnalyticsMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;
import java.util.Map;

@Service
public class AnalyticsRetention {
    private final PxAnalyticsMapper mapper;
    private final boolean enabled;
    public AnalyticsRetention(PxAnalyticsMapper mapper, @Value("${pnkx.analytics.cleanup-enabled:false}") boolean enabled) {
        this.mapper=mapper; this.enabled=enabled;
    }
    public static LocalDateTime cutoff(Clock clock) {
        return LocalDate.now(clock.withZone(ZoneId.of("Asia/Shanghai"))).minusDays(90).atStartOfDay();
    }
    public Map<String,Object> preview() {
        var cutoff=cutoff(Clock.systemUTC());
        return Map.of("enabled",enabled,"cutoff",cutoff.toString(),"expiredCount",mapper.countExpired(cutoff),"maxRowsPerRun",10000);
    }
    /** Small independently committed batches; fixed cutoff throughout a run. */
    public int cleanup() {
        if(!enabled) return 0;
        var cutoff=cutoff(Clock.systemUTC());
        int total=0;
        for(int i=0;i<10;i++) {
            int removed=mapper.deleteExpired(cutoff);total+=removed;
            if(removed<1000) break;
        }
        return total;
    }
}
