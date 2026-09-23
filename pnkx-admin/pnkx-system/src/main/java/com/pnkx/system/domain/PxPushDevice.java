package com.pnkx.system.domain;

import lombok.Data;

/**
 * App 推送设备（uniPush clientId 登记）
 *
 * @author PHY
 * @date 2026-09-23
 */
@Data
public class PxPushDevice {

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * uniPush clientId
     */
    private String clientId;

    /**
     * 平台（android/ios）
     */
    private String platform;

    /**
     * 客户端版本
     */
    private String appVersion;

    /**
     * 最近上报时间
     */
    private java.util.Date updateTime;
}
