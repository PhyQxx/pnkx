package com.pnkx.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.pnkx.common.core.redis.RedisCache;
import com.pnkx.system.domain.PxPushDevice;
import com.pnkx.system.mapper.PxPushDeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * uniPush 2.0（个推 REST v2）推送服务
 * <p>
 * 配置驱动：UNIPUSH_APPID/APPKEY/MASTERSECRET 环境变量齐全时启用，
 * 任一缺失时视为未启用；普通业务可按能力探测跳过，显式发送调用会返回失败。
 * 鉴权 token 缓存于 Redis（个推有效期 24h，提前 1h 失效重取）。
 * <p>
 * 平台侧前置条件（仅需一次）：DCloud 开发者中心 → 应用 → uniPush 2.0 开通并绑定。
 *
 * @author PHY
 * @date 2026-09-23
 */
@Service
public class UniPushService {

    private static final Logger log = LoggerFactory.getLogger(UniPushService.class);

    /**
     * 个推 REST 基地址
     */
    private static final String GT_BASE = "https://restapi.getui.com/v2";

    /**
     * token 缓存键
     */
    private static final String TOKEN_KEY = "unipush:token";

    @Value("${unipush.appid:}")
    private String appId;

    @Value("${unipush.appkey:}")
    private String appKey;

    @Value("${unipush.mastersecret:}")
    private String masterSecret;

    @Resource
    private PxPushDeviceMapper pushDeviceMapper;

    @Resource
    private RedisCache redisCache;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 是否已配置启用
     */
    public boolean enabled() {
        return !appId.isEmpty() && !appKey.isEmpty() && !masterSecret.isEmpty();
    }

    /**
     * 登记客户端设备（App 启动时调用）
     */
    public void registerDevice(String userId, String clientId, String platform, String appVersion) {
        if (userId == null || clientId == null || clientId.isEmpty()) {
            return;
        }
        PxPushDevice device = new PxPushDevice();
        device.setUserId(userId);
        device.setClientId(clientId);
        device.setPlatform(platform);
        device.setAppVersion(appVersion);
        pushDeviceMapper.upsertDevice(device);
        log.info("推送设备登记：user={}, platform={}, clientId={}", userId, platform, clientId);
    }

    /**
     * 向用户全部在册设备推送通知（离线到达锁屏）。
     * 已启用服务但没有可投递设备或全部投递失败时抛出异常，由提醒引擎记录失败并支持重试。
     *
     * @param userId  用户ID
     * @param title   通知标题
     * @param content 通知内容
     * @param payload 透传数据（点击通知跳转用）
     * @return 成功推送的设备数
     */
    public int sendToUser(String userId, String title, String content, Map<String, String> payload) {
        if (!enabled()) {
            return 0;
        }
        List<PxPushDevice> devices = pushDeviceMapper.selectByUserId(userId);
        if (devices.isEmpty()) {
            throw new IllegalStateException("用户没有已登记的推送设备");
        }
        String token = getToken();
        if (token == null) {
            throw new IllegalStateException("uniPush 鉴权失败");
        }
        int success = 0;
        for (PxPushDevice device : devices) {
            try {
                JSONObject body = new JSONObject();
                body.put("request_id", java.util.UUID.randomUUID().toString().replace("-", ""));
                JSONObject message = new JSONObject();
                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", content);
                notification.put("click_type", "payload");
                notification.put("payload", payload == null ? "" : JSON.toJSONString(payload));
                message.put("notification", notification);
                body.put("message", message);
                JSONObject settings = new JSONObject();
                settings.put("ttl", 3600 * 1000);
                body.put("settings", settings);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("token", token);
                String resp = restTemplate.postForObject(
                        GT_BASE + "/" + appId + "/push/single/cid/" + device.getClientId(),
                        new HttpEntity<>(body.toJSONString(), headers), String.class);
                if (resp != null && resp.contains("\"code\":0")) {
                    success++;
                } else {
                    log.warn("uniPush 单推响应异常, clientId={}, resp={}", device.getClientId(), resp);
                }
            } catch (Exception e) {
                log.warn("uniPush 推送失败, clientId={}", device.getClientId(), e);
            }
        }
        if (success == 0) throw new IllegalStateException("uniPush 全部设备投递失败");
        return success;
    }

    /**
     * 清理 30 天未上报的失效设备（可挂在任意定时任务里调用）
     */
    public int cleanStaleDevices() {
        return pushDeviceMapper.cleanStaleDevices(30);
    }

    /**
     * JDK 原生 SHA-256 转 hex（不引入 commons-codec）
     */
    private static String sha256Hex(String input) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 获取（或复用缓存的）鉴权 token
     */
    private String getToken() {
        String cached = redisCache.getCacheObject(TOKEN_KEY);
        if (cached != null) {
            return cached;
        }
        try {
            long timestamp = System.currentTimeMillis();
            String sign = sha256Hex(appKey + timestamp + masterSecret);
            JSONObject body = new JSONObject();
            body.put("sign", sign);
            body.put("timestamp", timestamp);
            body.put("appkey", appKey);
            String resp = restTemplate.postForObject(
                    GT_BASE + "/" + appId + "/auth", body, String.class);
            JSONObject json = JSON.parseObject(resp);
            if (json != null && json.getIntValue("code") == 0) {
                String token = json.getJSONObject("data").getString("token");
                redisCache.setCacheObject(TOKEN_KEY, token, 23, TimeUnit.HOURS);
                return token;
            }
            log.error("uniPush 获取 token 失败: {}", resp);
        } catch (Exception e) {
            log.error("uniPush 获取 token 异常", e);
        }
        return null;
    }
}
