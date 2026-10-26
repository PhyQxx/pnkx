package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.redis.RedisCache;
import com.pnkx.framework.config.RedisConfig;
import com.pnkx.web.service.intent.AiPendingActionService.PendingAction;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@EnabledIfEnvironmentVariable(named = "PNKX_TEST_REDIS_PORT", matches = "55012")
class PendingRedisSnapshotTest {
  @Test
  void staleSnapshotCannotDeleteNewDraftWithProductionSerializer() {
    var factory = new LettuceConnectionFactory("127.0.0.1", 55012);
    factory.afterPropertiesSet();
    factory.start();
    var template = new RedisConfig().redisTemplate(factory);
    var cache = new RedisCache();
    cache.redisTemplate = template;
    String key = "test:pending:" + UUID.randomUUID();
    var old =
        new PendingAction(
            "old", "r1", "todo", new JSONObject(), Instant.now(), Instant.now().plusSeconds(600));
    var next =
        new PendingAction(
            "new", "r2", "todo", new JSONObject(), Instant.now(), Instant.now().plusSeconds(600));
    try {
      cache.setCacheObject(key, old);
      PendingAction restored = cache.getCacheObject(key);
      assertEquals(old.id(), restored.id());
      assertTrue(cache.deleteIfValueMatches(key, restored));
      assertFalse(template.hasKey(key));
      cache.setCacheObject(key, next);
      assertFalse(cache.deleteIfValueMatches(key, old));
      assertTrue(template.hasKey(key));
      assertTrue(cache.deleteIfValueMatches(key, next));
    } finally {
      template.delete(key);
      factory.destroy();
    }
  }
}
