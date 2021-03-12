package com.ruoyi.web.controller.px.admin;

import com.ruoyi.web.core.websocket.WebSocket;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Classname PxWebSocket
 * @Description TODO
 * @Date 2021-03-11 11:02
 * @Author by PHY
 */
@RestController
@RequestMapping("/pxWebSocket")
public class PxWebSocket {

    @Resource
    WebSocket webSocket;

    @GetMapping("/sendAllWebSocket")
    public String test() {
        String text="你们好！这是websocket群体发送！";
//        webSocket.sendAllMessage(text);
        return text;
    }

    @GetMapping("/sendOneWebSocket/{userName}")
    public String sendOneWebSocket(@PathVariable("userName") String userName) {
        String text=userName+" 你好！ 这是websocket单人发送！";
        webSocket.sendOneMessage(userName,text);
        return text;
    }
}
