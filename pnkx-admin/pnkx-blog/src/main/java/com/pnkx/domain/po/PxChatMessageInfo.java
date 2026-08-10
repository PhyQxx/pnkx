package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import com.pnkx.common.utils.ip.IpLocation;
import lombok.Data;

/**
 * @author by PHY
 * @Classname PxChatMember
 * @date 2021-04-01 15:33
 */
@Data
public class PxChatMessageInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型
     */
    private String webSocket;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 用户名
     */
    private String nickName;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 标志位
     */
    private String status;
    /**
     * 信息内容
     */
    private String message;
    /**
     * 信息类型
     */
    private String type;
    /**
     * 发送时间
     */
    private String sendTime;

    /**
     * 地址信息
     */
    private IpLocation location;

    @Override
    public String toString() {
        return "{" +
                "webSocket: '" + webSocket + '\'' +
                ", userId: '" + userId + '\'' +
                ", nickName: '" + nickName + '\'' +
                ", avatar: '" + avatar + '\'' +
                ", status: '" + status + '\'' +
                ", message: '" + message + '\'' +
                ", type: '" + type + '\'' +
                ", sendTime: '" + sendTime + '\'' +
                ", location: " + location +
                '}';
    }
}
