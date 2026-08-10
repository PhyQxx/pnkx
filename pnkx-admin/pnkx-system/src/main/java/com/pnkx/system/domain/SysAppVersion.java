package com.pnkx.system.domain;

import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * App版本管理表 sys_app_version
 *
 * @author pnkx
 * @date 2024-07-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysAppVersion extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 平台 (android/ios)
     */
    private String appType;

    /**
     * 版本编号 (整数，如 101)
     */
    private Integer versionCode;

    /**
     * 版本名称 (字符串，如 1.0.1)
     */
    private String versionName;

    /**
     * 更新类型 (0:wgt热更新 1:整包更新)
     */
    private Integer updateType;

    /**
     * 是否强制更新 (0:否 1:是)
     */
    private Integer forceUpdate;

    /**
     * 下载地址 (wgt/apk/ipa链接)
     */
    private String downloadUrl;

    /**
     * 更新日志/内容
     */
    private String updateLog;

    /**
     * 状态 (0:下线 1:正常生效)
     */
    private Integer status;

}
