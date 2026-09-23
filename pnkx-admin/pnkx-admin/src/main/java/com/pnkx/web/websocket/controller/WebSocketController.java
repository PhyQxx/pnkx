package com.pnkx.web.websocket.controller;

import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.spring.SpringUtils;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.web.websocket.encoder.NoticeEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket
 * 客户端以 userId 作为路径参数连接，握手时校验 query 参数中的 JWT，
 * 令牌身份与路径 userId 不一致或令牌无效则立即关闭连接，防止冒充他人收消息
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 11:46
 * @description WebSocket
 */
@Component
@ServerEndpoint(value = "/websocket/{userId}", encoders = { NoticeEncoder.class })

public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private static final CopyOnWriteArraySet<WebSocketController> WEB_SOCKETS = new CopyOnWriteArraySet<>();
    private static final Map<String, Session> SESSION_POOL = new ConcurrentHashMap<>();

    private Session session;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebSocketController webSocketController = (WebSocketController) o;
        return Objects.equals(session, webSocketController.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        if (!authenticate(session, userId)) {
            closeQuietly(session);
            log.warn("【websocket消息】拒绝未授权连接，userId：{}", userId);
            return;
        }
        this.session = session;
        WEB_SOCKETS.add(this);
        SESSION_POOL.put(userId, session);
        log.info("【websocket消息】有新的连接，连接用户ID：{}，总数为：{}", userId, WEB_SOCKETS.size());
    }

    @OnClose
    public void onClose(@PathParam(value = "userId") String userId) {
        WEB_SOCKETS.remove(this);
        // 仅当会话仍是当前连接时才移除，避免误删同一用户的新连接
        SESSION_POOL.remove(userId, this.session);
        log.info("【websocket消息】连接断开，总数为：{}", WEB_SOCKETS.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("【websocket消息】连接异常", error);
        closeQuietly(session);
    }

    /**
     * 握手鉴权：query 参数携带 token，解析后身份必须与路径 userId 一致
     */
    private boolean authenticate(Session session, String userId) {
        try {
            Map<String, List<String>> params = session.getRequestParameterMap();
            List<String> tokens = params.get("token");
            if (tokens == null || tokens.isEmpty() || StringUtils.isEmpty(tokens.get(0))) {
                return false;
            }
            TokenService tokenService = SpringUtils.getBean(TokenService.class);
            LoginUser loginUser = tokenService.getLoginUserByToken(tokens.get(0));
            return loginUser != null && loginUser.getUser() != null
                    && String.valueOf(loginUser.getUser().getUserId()).equals(userId);
        } catch (Exception e) {
            log.error("【websocket消息】握手鉴权异常", e);
            return false;
        }
    }

    private void closeQuietly(Session session) {
        try {
            session.close();
        } catch (Exception ignored) {
        }
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【websocket消息】收到客户端消息：{}", message);
    }

    /**
     * 此为广播消息
     */
    public void sendAllMessage(Object object) {
        log.info("【websocket消息】广播消息：{}", object.toString());
        for (WebSocketController webSocketController : WEB_SOCKETS) {
            try {
                webSocketController.session.getAsyncRemote().sendObject(object);
            } catch (Exception e) {
                log.error("广播消息异常", e);
            }
        }
    }

    /**
     * 此为单点消息
     */
    public void sendOneMessage(String userId, String message) {
        log.info("【websocket消息】单点消息：{}", message);
        Session oneSession = SESSION_POOL.get(userId);
        if (oneSession != null) {
            try {
                oneSession.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("单点消息异常", e);
            }
        }
    }
}
