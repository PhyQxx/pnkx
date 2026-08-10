package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;

/**
 * @author by PHY
 * @Classname PxEmailSubscribe 订阅信息
 * @date 2021-06-17 14:32
 */
public class PxEmailSubscribe extends BaseEntity {

    /**
     * 主键
     */
    private Long id;

    /**
     * 订阅邮箱
     */
    private String subscribeMail;

    /**
     * 版本号
     */
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubscribeMail() {
        return subscribeMail;
    }

    public void setSubscribeMail(String subscribeMail) {
        this.subscribeMail = subscribeMail;
    }

}
