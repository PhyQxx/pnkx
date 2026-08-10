package com.pnkx.service.impl;

import com.pnkx.common.constant.RedisConstants;
import com.pnkx.domain.po.PxChatMessageInfo;
import com.pnkx.service.IPxChatMessageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PxChatServiceImpl
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 11:57
 * @description 描述
 */
@Service
public class PxChatMessageServiceImpl implements IPxChatMessageService {

    @Resource
    private RedisTemplate<String, List<PxChatMessageInfo>> redisTemplate;
    /**
     * 发送消息
     *
     * @param pxChatMessage 参数
     * @return 发送结果
     */
    @Override
    public List<PxChatMessageInfo> sendMessage(PxChatMessageInfo pxChatMessage) {
        ValueOperations<String, List<PxChatMessageInfo>> operations = redisTemplate.opsForValue();
        boolean hasKey = Boolean.TRUE.equals(redisTemplate.hasKey(RedisConstants.PX_CHAT_MESSAGE));
        DateTimeFormatter fmTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        pxChatMessage.setSendTime(now.format(fmTime));
        List<PxChatMessageInfo> result = new ArrayList<>();
        if (hasKey) {
            result = operations.get(RedisConstants.PX_CHAT_MESSAGE);
        }
        if (result != null) {
            result.add(pxChatMessage);
            operations.set(RedisConstants.PX_CHAT_MESSAGE, result);
        }
        return result;
    }

    @Override
    public List<PxChatMessageInfo> getMessageRecord() {
        ValueOperations<String, List<PxChatMessageInfo>> operations = redisTemplate.opsForValue();
        return operations.get(RedisConstants.PX_CHAT_MESSAGE);
    }
}
