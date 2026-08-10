package com.pnkx.service;

import com.pnkx.domain.po.PxChatMessageInfo;

import java.util.List;

/**
 * IPxChatService
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 11:57
 * @description 聊天接口
 */
public interface IPxChatMessageService {

    /**
     * 发送消息
     * @param pxChatMessage 参数
     * @return 发送结果
     */
    List<PxChatMessageInfo> sendMessage(PxChatMessageInfo pxChatMessage);

    /**
     * 获取消息
     * @return 发送结果
     */
    List<PxChatMessageInfo> getMessageRecord();
}
