package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.constant.RedisConstants;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.core.redis.RedisCache;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class AiPendingActionService {

    private static final long EXPIRE_SECONDS = 10 * 60;

    @Resource
    private RedisCache redisCache;

    public PendingAction save(String requestId, String intent, JSONObject draft) {
        Instant now = Instant.now();
        PendingAction action = new PendingAction(UUID.randomUUID().toString(), requestId, intent, draft, now, now.plusSeconds(EXPIRE_SECONDS));
        String key = RedisConstants.AI_PENDING_ACTION + currentUserKey();
        redisCache.setCacheObject(key, action, (int) EXPIRE_SECONDS, TimeUnit.SECONDS);
        return action;
    }

    public PendingAction getCurrent() {
        String key = RedisConstants.AI_PENDING_ACTION + currentUserKey();
        return redisCache.getCacheObject(key);
    }

    public PendingAction getCurrentIncludingExpired() {
        return getCurrent();
    }

    public void clearCurrent() {
        String key = RedisConstants.AI_PENDING_ACTION + currentUserKey();
        redisCache.deleteObject(key);
    }

    public boolean isExpired(PendingAction action) {
        return action != null && !action.expireAt().isAfter(Instant.now());
    }

    public static boolean isConfirmCommand(String question) {
        String text = normalize(question);
        return "确认".equals(text)
                || "确认保存".equals(text)
                || "保存".equals(text)
                || "提交".equals(text)
                || "是".equals(text)
                || "可以".equals(text)
                || "好的".equals(text)
                || "ok".equals(text)
                || "yes".equals(text);
    }

    public static boolean isCancelCommand(String question) {
        String text = normalize(question);
        return "取消".equals(text)
                || "不保存".equals(text)
                || "放弃".equals(text)
                || "算了".equals(text)
                || "撤销".equals(text)
                || "cancel".equals(text)
                || "no".equals(text);
    }

    private static String normalize(String question) {
        if (question == null) {
            return "";
        }
        return question.trim()
                .replace("。", "")
                .replace(".", "")
                .replace("！", "")
                .replace("!", "")
                .replace("，", "")
                .replace(",", "")
                .toLowerCase();
    }

    private String currentUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser
                && loginUser.getUser() != null && loginUser.getUser().getUserId() != null) {
            return String.valueOf(loginUser.getUser().getUserId());
        }
        return "anonymous";
    }

    public record PendingAction(String id, String requestId, String intent, JSONObject draft, Instant createdAt, Instant expireAt) {
    }
}
