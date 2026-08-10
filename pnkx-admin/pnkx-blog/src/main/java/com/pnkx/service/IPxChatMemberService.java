package com.pnkx.service;

import com.pnkx.common.core.domain.entity.SysUser;

import java.util.List;

/**
 * IPxChatService
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/11/14 11:57
 * @description 聊天接口
 */
public interface IPxChatMemberService {
    /**
     * 登录聊天室
     * @param sysUser
     * @return
     */
    List<SysUser> loginChat(SysUser sysUser);

    /**
     * 退出
     * @param userId
     * @return
     */
    void signOut(String userId);
}
