package com.pnkx.web.websocket.controller;

import com.pnkx.web.websocket.encoder.NoticeEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 11:46
 * @description WebSocket
 */
@Component
@ServerEndpoint(value = "/websocket/{userName}", encoders = { NoticeEncoder.class })

public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private static final CopyOnWriteArraySet<WebSocketController> WEB_SOCKETS = new CopyOnWriteArraySet<>();
    private static final Map<String, Session> SESSION_POOL = new HashMap<String, Session>();

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
    public void onOpen(Session session, @PathParam(value = "userName") String userName) {
        this.session = session;
        WEB_SOCKETS.add(this);
        SESSION_POOL.put(userName, session);
        log.info("【websocket消息】有新的连接，新连接用户名为：{}，总数为：{}", userName, WEB_SOCKETS.size());
    }

    @OnClose
    public void onClose(@PathParam(value = "userName") String userName) {
        WEB_SOCKETS.remove(this);
        log.info("【websocket消息】连接断开，总数为：{}", WEB_SOCKETS.size());
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
                log.error("广播消息异常，异常信息：{}", e.getMessage());
            }
        }
    }

    /**
     * 此为单点消息
     */
    public void sendOneMessage(String userName, String message) {
        log.info("【websocket消息】单点消息：{}", message);
        Session oneSession = SESSION_POOL.get(userName);
        if (oneSession != null) {
            try {
                oneSession.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("单点消息异常，异常信息：{}", e.getMessage());
            }
        }
    }
}
