package com.pnkx.web.websocket.encoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.pnkx.web.websocket.domain.WebSocketMessage;

import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

/**
 * @author by PHY
 * @classname NoticeEncoder
 * @date 2022/8/23 15:22
 * @description: 描述
 */
public class NoticeEncoder implements Encoder.Text<WebSocketMessage>  {

    private static final Logger log = LoggerFactory.getLogger(NoticeEncoder.class);
    @Override
    public String encode(WebSocketMessage webSocketMessage) {
        try {
            JsonMapper jsonMapper = new JsonMapper();
            return jsonMapper.writeValueAsString(webSocketMessage);
        } catch ( JsonProcessingException e) {
            log.error("WebSocket 消息编码异常", e);
            return null;
        }
    }

    @Override
    public void init(EndpointConfig endpointConfig) {

    }

    @Override
    public void destroy() {

    }
}
