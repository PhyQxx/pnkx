package com.pnkx.service.impl;

import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.PxChatMessage;
import com.pnkx.domain.po.WebhookEvent;
import com.pnkx.mapper.PxChatMessageMapper;
import com.pnkx.service.IPxChatRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class PxChatRecordServiceImpl implements IPxChatRecordService {

    @Resource
    private PxChatMessageMapper pxChatMessageMapper;

    /**
     * 保存聊天消息
     */
    @Override
    public void saveMessage(WebhookEvent event, boolean isBotReply, String botReplyContent) {
        try {
            PxChatMessage chatMessage = new PxChatMessage();
            chatMessage.setMessageId(event.getMid());
            chatMessage.setUserId(event.getFrom_uid() != null ? event.getFrom_uid() : event.getUid());
            chatMessage.setUserName(getUserName(event));
            chatMessage.setGroupId(getGroupId(event));
            chatMessage.setGroupName(getGroupName(event));
            chatMessage.setContent(getContent(event));
            chatMessage.setMessageType(getMessageType(event));
            chatMessage.setIsBotReply(isBotReply);
            chatMessage.setBotReplyContent(botReplyContent);
            chatMessage.setCreateTime(new Date());
            pxChatMessageMapper.insertPxChatMessage(chatMessage);
            log.info("聊天消息已保存: {}", event.getMid());
        } catch (Exception e) {
            log.error("保存聊天消息失败: {}", event.getMid(), e);
        }
    }

    /**
     * 获取用户名称
     */
    private String getUserName(WebhookEvent event) {
        if (event.getName() != null) {
            return event.getName();
        }
        if (event.getFrom_uid() != null) {
            return String.valueOf(event.getFrom_uid());
        }
        if (event.getUid() != null) {
            return String.valueOf(event.getUid());
        }
        return "未知用户";
    }

    /**
     * 获取群组ID
     */
    private Integer getGroupId(WebhookEvent event) {
        if (event.getTarget() != null && event.getTarget().getGid() != null) {
            return event.getTarget().getGid();
        }
        return null;
    }

    /**
     * 获取群组名称
     */
    private String getGroupName(WebhookEvent event) {
        if (event.getTarget() != null && event.getTarget().getGid() != null) {
            return String.valueOf(event.getTarget().getGid());
        }
        return null;
    }

    /**
     * 获取消息内容
     */
    private String getContent(WebhookEvent event) {
        // 处理新用户事件
        if ("new_user".equals(event.getType())) {
            return "新用户注册: " + (event.getName() != null ? event.getName() : "未知用户");
        }

        // 处理普通消息事件
        if (event.getDetail() != null && event.getDetail().getContent() != null) {
            return event.getDetail().getContent();
        }

        return "无内容";
    }

    /**
     * 获取消息类型
     */
    private String getMessageType(WebhookEvent event) {
        if (event.getDetail() != null && event.getDetail().getType() != null) {
            return event.getDetail().getType();
        }
        return event.getType();
    }
    /**
     * 查询聊天记录
     *
     * @param id 聊天记录ID
     * @return 聊天记录
     */
    @Override
    public PxChatMessage selectPxChatMessageById(Long id) {
        return pxChatMessageMapper.selectPxChatMessageById(id);
    }

    /**
     * 查询聊天记录列表
     *
     * @param pxChatMessage 聊天记录
     * @return 聊天记录
     */
    @Override
    public List<PxChatMessage> selectPxChatMessageList(PxChatMessage pxChatMessage) {
        return pxChatMessageMapper.selectPxChatMessageList(pxChatMessage);
    }

    /**
     * 新增聊天记录
     *
     * @param pxChatMessage 聊天记录
     * @return 结果
     */
    @Override
    public int insertPxChatMessage(PxChatMessage pxChatMessage) {
        pxChatMessage.setCreateTime(DateUtils.getNowDate());
        return pxChatMessageMapper.insertPxChatMessage(pxChatMessage);
    }

    /**
     * 修改聊天记录
     *
     * @param pxChatMessage 聊天记录
     * @return 结果
     */
    @Override
    public int updatePxChatMessage(PxChatMessage pxChatMessage) {
        return pxChatMessageMapper.updatePxChatMessage(pxChatMessage);
    }

    /**
     * 批量删除聊天记录
     *
     * @param ids 需要删除的聊天记录ID
     * @return 结果
     */
    @Override
    public int deletePxChatMessageByIds(Long[] ids) {
        return pxChatMessageMapper.deletePxChatMessageByIds(ids);
    }

    /**
     * 删除聊天记录信息
     *
     * @param id 聊天记录ID
     * @return 结果
     */
    @Override
    public int deletePxChatMessageById(Long id) {
        return pxChatMessageMapper.deletePxChatMessageById(id);
    }
}
