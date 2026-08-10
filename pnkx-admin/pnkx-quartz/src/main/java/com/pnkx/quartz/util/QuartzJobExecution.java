package com.pnkx.quartz.util;

import org.quartz.JobExecutionContext;
import com.pnkx.quartz.domain.SysJob;

/**
 * 定时任务处理（允许并发执行）
 *
 * @author phy
 */
public class QuartzJobExecution extends AbstractQuartzJob {
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
