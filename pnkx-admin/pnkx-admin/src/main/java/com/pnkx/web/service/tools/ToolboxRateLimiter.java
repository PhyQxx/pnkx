package com.pnkx.web.service.tools;

import com.pnkx.common.core.redis.RedisCache;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 工具箱免登录接口 IP 限流器
 *
 * 固定窗口计数：第一次命中时写入过期时间，窗口内超过 limit 次则拒绝。
 * Redis 异常时放行（限流失败不应阻断正常使用）。
 *
 * @author PHY
 */
@Component
public class ToolboxRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(ToolboxRateLimiter.class);

    @Resource
    private RedisCache redisCache;

    /**
     * 尝试获取一次调用配额
     *
     * @param key           限流 key（需含维度标识，如 IP）
     * @param limit         窗口内最大次数
     * @param windowSeconds 窗口时长（秒）
     * @return true=放行，false=超限
     */
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        try {
            Long count = redisCache.redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisCache.redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            logger.warn("工具箱限流计数失败，放行本次请求 - key: {} - {}", key, e.getMessage());
            return true;
        }
    }
}
