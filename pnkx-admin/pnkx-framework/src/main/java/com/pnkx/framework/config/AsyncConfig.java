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

    /**
     * 网址剪藏任务线程池：网页抓取，IO 密集且单任务较快（10s 超时），
     * 独立于通知线程池避免互相阻塞；队列打满时由调用线程执行，保证提交不丢
     */
    @Bean("webClipExecutor")
    public Executor webClipExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pnkx-webclip-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 网址 AI 分析线程池：LLM 单次调用耗时长（30s+）且有速率限额，
     * 单线程串行消费——RSS 批量场景下分析任务在队列排队而非并发轰炸模型 API，
     * 也避免长时间占用抓取线程。队列打满时由调用线程执行（天然背压）。
     */
    @Bean("webArticleAiExecutor")
    public Executor webArticleAiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("pnkx-wa-ai-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
