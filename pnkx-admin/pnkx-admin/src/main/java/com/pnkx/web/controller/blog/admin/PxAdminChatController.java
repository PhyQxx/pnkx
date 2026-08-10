package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.ip.IpLocation;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.domain.po.PxChatMessageInfo;
import com.pnkx.service.IPxChatMemberService;
import com.pnkx.service.IPxChatMessageService;
import com.pnkx.web.websocket.controller.WebSocketController;
import com.pnkx.web.websocket.domain.MessageType;
import com.pnkx.web.websocket.domain.WebSocketMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ChatController
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 11:46
 * @description 聊天控制器
 */
@Tag(name = "博客管理-聊天管理")
@RestController
@RequestMapping("/admin/chat")
public class PxAdminChatController {

    @Resource
    WebSocketController webSocketController;
    @Resource
    IPxChatMemberService pxChatMemberService;
    @Resource
    IPxChatMessageService pxChatMessageService;

    /**
     * 登录聊天室
     *
     * @return 新增结果
     */
    @RequestMapping("/loginChat")
    public AjaxResult loginChat(HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        IpLocation location = IpUtils.getLocation(ip);
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        SysUser user = SecurityUtils.getLoginUser().getUser();
        user.setLocation(location);
        webSocketMessage.setWebSocket(MessageType.LOGIN);
        webSocketMessage.setUserId(user.getUserId().toString());
        webSocketMessage.setMessage(user);
        webSocketController.sendAllMessage(webSocketMessage);
        return AjaxResult.success("新增成功", pxChatMemberService.loginChat(user));
    }

    /**
     * 退出聊天室
     * @return 新增结果
     */
    @RequestMapping("/signOut")
    public AjaxResult signOut() {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setWebSocket(MessageType.LOG_OUT);
        webSocketMessage.setUserId(SecurityUtils.getUserId());
        webSocketController.sendAllMessage(webSocketMessage);
        pxChatMemberService.signOut(SecurityUtils.getUserId());
        return AjaxResult.success("退出成功");
    }

    /**
     * 发送消息
     *
     * @param pxChatMessage 参数
     * @return 发送结果
     */
    @RequestMapping("/sendMessage")
    public AjaxResult sendMessage(@RequestBody PxChatMessageInfo pxChatMessage, HttpServletRequest request) {
        SysUser user = SecurityUtils.getLoginUser().getUser();
        pxChatMessage.setUserId(user.getUserId().toString());
        pxChatMessage.setNickName(user.getNickName());
        pxChatMessage.setAvatar(user.getAvatar());
        String ip = IpUtils.getIpAddr(request);
        IpLocation location = IpUtils.getLocation(ip);
        pxChatMessage.setLocation(location);
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setWebSocket(MessageType.CHAT_MESSAGE);
        webSocketMessage.setUserId(SecurityUtils.getUserId());
        webSocketMessage.setMessage(pxChatMessage);
        webSocketController.sendAllMessage(webSocketMessage);
        return AjaxResult.success("发送成功", pxChatMessageService.sendMessage(pxChatMessage));
    }
    /**
     * 获取消息
     *
     * @return 获取消息
     */
    @RequestMapping("/getMessageRecord")
    public AjaxResult getMessageRecord() {
        return AjaxResult.success("获取消息成功", pxChatMessageService.getMessageRecord());
    }
}
