package com.pnkx.service;

import com.pnkx.domain.po.PxChatMessage;
import com.pnkx.domain.po.WebhookEvent;

import java.util.List;

/**
 * 聊天记录服务接口
 */
public interface IPxChatRecordService {

    /**
     * 保存聊天消息
     */
    void saveMessage(WebhookEvent event, boolean isBotReply, String botReplyContent);
    /**
     * 查询聊天记录
     *
     * @param id 聊天记录ID
     * @return 聊天记录
     */
    public PxChatMessage selectPxChatMessageById(Long id);

    /**
     * 查询聊天记录列表
     *
     * @param pxChatMessage 聊天记录
     * @return 聊天记录集合
     */
    public List<PxChatMessage> selectPxChatMessageList(PxChatMessage pxChatMessage);

    /**
     * 新增聊天记录
     *
     * @param pxChatMessage 聊天记录
     * @return 结果
     */
    public int insertPxChatMessage(PxChatMessage pxChatMessage);

    /**
     * 修改聊天记录
     *
     * @param pxChatMessage 聊天记录
     * @return 结果
     */
    public int updatePxChatMessage(PxChatMessage pxChatMessage);

    /**
     * 批量删除聊天记录
     *
     * @param ids 需要删除的聊天记录ID
     * @return 结果
     */
    public int deletePxChatMessageByIds(Long[] ids);

    /**
     * 删除聊天记录信息
     *
     * @param id 聊天记录ID
     * @return 结果
     */
    public int deletePxChatMessageById(Long id);
}
