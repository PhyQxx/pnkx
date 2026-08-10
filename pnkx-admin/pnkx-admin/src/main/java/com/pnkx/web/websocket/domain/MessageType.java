package com.pnkx.web.websocket.domain;

/**
 * MessageType
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 16:20
 * @description 消息类型
 */
public class MessageType {

    /**
     * 登录
     */
    public static final String LOGIN = "login";

    /**
     * 退出登录
     */
    public static final String LOG_OUT = "log_out";

    /**
     * 聊天消息
     */
    public static final String CHAT_MESSAGE = "chat_message";
}
