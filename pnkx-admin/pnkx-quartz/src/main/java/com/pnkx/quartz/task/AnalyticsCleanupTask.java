package com.pnkx.quartz.task;
import com.pnkx.blog.service.AnalyticsRetention;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component("analyticsCleanupTask")
public class AnalyticsCleanupTask {
    private static final Logger log=LoggerFactory.getLogger(AnalyticsCleanupTask.class);
    private final AnalyticsRetention retention;
    public AnalyticsCleanupTask(AnalyticsRetention retention) { this.retention=retention; }
    public void run() { log.info("Analytics cleanup removed {} rows",retention.cleanup()); }
}
