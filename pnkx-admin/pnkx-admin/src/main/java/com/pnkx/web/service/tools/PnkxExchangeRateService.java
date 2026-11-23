package com.pnkx.web.service.tools;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.constant.RedisConstants;
import com.pnkx.common.core.redis.RedisCache;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 汇率服务（工具箱·免登录）
 *
 * 数据链路：Redis 主缓存(1h) → 三方接口(open.er-api.com，免鉴权) → Redis 备份(7d) → 内置静态兜底
 * 兜底以 CNY 牌价表按交叉汇率推导任意基准，并带 stale 标记提示非实时。
 *
 * @author PHY
 */
@Service
public class PnkxExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(PnkxExchangeRateService.class);

    /** 面向工具箱开放的币种白名单（控制响应体积，过滤三方全量牌价） */
    private static final Map<String, String> CURRENCY_NAMES = Map.ofEntries(
            Map.entry("CNY", "人民币"), Map.entry("USD", "美元"), Map.entry("EUR", "欧元"),
            Map.entry("JPY", "日元"), Map.entry("GBP", "英镑"), Map.entry("HKD", "港币"),
            Map.entry("MOP", "澳门元"), Map.entry("TWD", "新台币"), Map.entry("KRW", "韩元"),
            Map.entry("SGD", "新加坡元"), Map.entry("THB", "泰铢"), Map.entry("MYR", "林吉特"),
            Map.entry("VND", "越南盾"), Map.entry("PHP", "菲律宾比索"), Map.entry("IDR", "印尼盾"),
            Map.entry("AUD", "澳元"), Map.entry("CAD", "加元"), Map.entry("CHF", "瑞士法郎"),
            Map.entry("RUB", "卢布"), Map.entry("NZD", "新西兰元")
    );

    /** 静态兜底牌价（以 CNY 为基准的近似值，仅三方全部不可用时使用） */
    private static final Map<String, Double> FALLBACK_CNY_RATES = Map.ofEntries(
            Map.entry("CNY", 1.0), Map.entry("USD", 0.14), Map.entry("EUR", 0.13),
            Map.entry("JPY", 21.5), Map.entry("GBP", 0.11), Map.entry("HKD", 1.09),
            Map.entry("MOP", 1.12), Map.entry("TWD", 4.4), Map.entry("KRW", 195.0),
            Map.entry("SGD", 0.185), Map.entry("THB", 4.6), Map.entry("MYR", 0.60),
            Map.entry("VND", 3550.0), Map.entry("PHP", 8.0), Map.entry("IDR", 2250.0),
            Map.entry("AUD", 0.21), Map.entry("CAD", 0.195), Map.entry("CHF", 0.115),
            Map.entry("RUB", 11.5), Map.entry("NZD", 0.23)
    );

    /** 主缓存时长（小时） */
    private static final int CACHE_HOURS = 1;

    /** 备份缓存时长（天） */
    private static final int BACKUP_DAYS = 7;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("GMT+8"));

    @Resource
    private RedisCache redisCache;

    /** 三方牌价接口前缀（免鉴权，GET {url}{base} 返回 result/rates/time_last_update_unix） */
    @Value("${tools.exrate.url:https://open.er-api.com/v6/latest/}")
    private String remoteUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 查询以 base 为基准的牌价
     *
     * @param base 基准币种（需在白名单内）
     * @return {base, updateTime, rates, stale, source}
     */
    public Map<String, Object> getRates(String base) {
        String normalized = base == null ? "" : base.trim().toUpperCase();
        if (!CURRENCY_NAMES.containsKey(normalized)) {
            throw new IllegalArgumentException("暂不支持该币种: " + normalized);
        }

        // 1. Redis 主缓存
        String cacheKey = RedisConstants.TOOL_EXRATE_CACHE + normalized;
        JSONObject cached = readCache(cacheKey);
        if (cached != null) {
            return wrapResult(normalized, cached, false, "cache");
        }

        // 2. 拉取三方接口
        try {
            JSONObject payload = fetchRemote(normalized);
            redisCache.setCacheObject(cacheKey, payload.toJSONString(), CACHE_HOURS, java.util.concurrent.TimeUnit.HOURS);
            redisCache.setCacheObject(cacheKey + ":backup", payload.toJSONString(), BACKUP_DAYS, java.util.concurrent.TimeUnit.DAYS);
            logger.info("汇率拉取成功 base={} 牌价数={}", normalized, payload.getJSONObject("rates").size());
            return wrapResult(normalized, payload, false, "remote");
        } catch (Exception e) {
            logger.warn("汇率三方接口拉取失败 base={} - {}", normalized, e.getMessage());
        }

        // 3. Redis 备份缓存（三方故障时最近一次成功数据）
        JSONObject backup = readCache(cacheKey + ":backup");
        if (backup != null) {
            logger.warn("汇率使用备份缓存 base={}", normalized);
            return wrapResult(normalized, backup, true, "backup");
        }

        // 4. 内置静态兜底（交叉汇率推导）
        logger.warn("汇率使用内置静态兜底 base={}", normalized);
        return wrapResult(normalized, buildStaticFallback(normalized), true, "static");
    }

    /**
     * 调用三方接口并解析为统一 payload：{rates, updateTime}
     */
    private JSONObject fetchRemote(String base) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(remoteUrl + base))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("三方接口响应码 " + response.statusCode());
        }
        JSONObject body = JSONObject.parseObject(response.body());
        if (!"success".equalsIgnoreCase(body.getString("result")) || body.getJSONObject("rates") == null) {
            throw new IllegalStateException("三方接口返回异常: " + body.getString("result"));
        }
        JSONObject allRates = body.getJSONObject("rates");
        JSONObject rates = new JSONObject(true);
        for (String code : CURRENCY_NAMES.keySet()) {
            Double value = allRates.getDouble(code);
            if (value != null) {
                rates.put(code, value);
            }
        }
        JSONObject payload = new JSONObject(true);
        payload.put("rates", rates);
        Long updateUnix = body.getLong("time_last_update_unix");
        payload.put("updateTime", updateUnix == null
                ? TIME_FORMATTER.format(Instant.now())
                : TIME_FORMATTER.format(Instant.ofEpochSecond(updateUnix)));
        return payload;
    }

    /**
     * 静态兜底：以 CNY 牌价表交叉推导任意基准
     */
    private JSONObject buildStaticFallback(String base) {
        double baseRate = FALLBACK_CNY_RATES.getOrDefault(base, 1.0);
        JSONObject rates = new JSONObject(true);
        for (Map.Entry<String, Double> entry : FALLBACK_CNY_RATES.entrySet()) {
            rates.put(entry.getKey(), round6(entry.getValue() / baseRate));
        }
        JSONObject payload = new JSONObject(true);
        payload.put("rates", rates);
        payload.put("updateTime", TIME_FORMATTER.format(Instant.now()));
        return payload;
    }

    private JSONObject readCache(String key) {
        try {
            String cached = redisCache.getCacheObject(key);
            return cached == null ? null : JSONObject.parseObject(cached);
        } catch (Exception e) {
            logger.warn("汇率缓存读取失败 - {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> wrapResult(String base, JSONObject payload, boolean stale, String source) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("base", base);
        result.put("rates", payload.getJSONObject("rates"));
        result.put("updateTime", payload.getString("updateTime"));
        result.put("stale", stale);
        result.put("source", source);
        result.put("currencyNames", CURRENCY_NAMES);
        return result;
    }

    private double round6(double value) {
        return Math.round(value * 1000000.0) / 1000000.0;
    }
}
