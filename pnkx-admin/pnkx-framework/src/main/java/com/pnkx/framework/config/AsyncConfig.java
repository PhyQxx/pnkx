package com.pnkx.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 供邮件群发（新文章通知）等 IO 型异步任务使用，
 * 通过 @Async("notifyExecutor") 声明式投递。
 *
 * @author phy
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 通知类任务线程池：IO 密集，小核心 + 弹性上限；
     * 队列打满时由调用线程执行（CallerRuns），保证任务不丢
     */
    @Bean("notifyExecutor")
    public Executor notifyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("pnkx-notify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
