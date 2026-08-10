package com.pnkx.service.impl;

import com.pnkx.common.constant.RedisConstants;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.service.IPxChatMemberService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PxChatServiceImpl
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 11:57
 * @description 描述
 */
@Service
public class PxChatMemberServiceImpl implements IPxChatMemberService {

    @Resource
    private RedisTemplate<String, List<SysUser>> redisTemplate;

    /**
     * 登录聊天室
     *
     * @param sysUser 用户信息
     * @return 用户列表
     */
    @Override
    public List<SysUser> loginChat(SysUser sysUser) {
        ValueOperations<String, List<SysUser>> operations = redisTemplate.opsForValue();
        boolean hasKey = Boolean.TRUE.equals(redisTemplate.hasKey(RedisConstants.PX_CHAT_MEMBER));
        List<SysUser> result;
        if (hasKey) {
            result = operations.get(RedisConstants.PX_CHAT_MEMBER);
            AtomicBoolean flag = new AtomicBoolean(true);
            if (result == null) {
                result = new ArrayList<>();
                result.add(sysUser);
                operations.set(RedisConstants.PX_CHAT_MEMBER, result);
            }
            result.forEach(item -> {
                if (item.getUserId().equals(sysUser.getUserId())) {
                    flag.set(false);
                }
            });
            if (flag.get()) {
                result.add(sysUser);
                operations.set(RedisConstants.PX_CHAT_MEMBER, result);
            }
        } else {
            result = new ArrayList<>();
            result.add(sysUser);
            operations.set(RedisConstants.PX_CHAT_MEMBER, result);
        }
        return result;
    }

    /**
     * 退出
     *
     * @param userId 用户id
     */
    @Override
    public void signOut(String userId) {
        ValueOperations<String, List<SysUser>> operations = redisTemplate.opsForValue();
        boolean hasKey = Boolean.TRUE.equals(redisTemplate.hasKey(RedisConstants.PX_CHAT_MEMBER));
        List<SysUser> result;
        if (hasKey) {
            result = operations.get(RedisConstants.PX_CHAT_MEMBER);
            if (result != null) {
                result.removeIf(user -> userId.equals(user.getUserId().toString()));
                operations.set(RedisConstants.PX_CHAT_MEMBER, result);
            }
        }
    }
}
