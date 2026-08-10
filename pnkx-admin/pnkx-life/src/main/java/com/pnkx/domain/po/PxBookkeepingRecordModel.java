package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 账本记录模板对象 px_bookkeeping_record_model
 *
 * @author pnkx
 * @date 2021-12-08
 */
public class PxBookkeepingRecordModel extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 模板名称
     */
    @Excel(name = "模板名称")
    private String name;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 账户ID（转账时的出账账户）
     */
    @Excel(name = "账户ID（转账时的出账账户）")
    private Long account;

    /**
     * 转账时的收账账户Id
     */
    private Long otherAccount;

    /**
     * 转账时的收账账户
     */
    private PxBookkeepingAccount otherAccountObject;

    /**
     * 账户ID（转账时的出账账户）
     */
    private PxBookkeepingAccount accountObject;

    /**
     * 种类ID
     */
    @Excel(name = "种类ID")
    private Long type;

    /**
     * 种类对象
     */
    private PxBookkeepingClassification typeObject;

    /**
     * 消费时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /**
     * 金额
     */
    @Excel(name = "金额")
    private String money;

    /**
     * 删除标志
     */
    private Boolean delFlag;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setAccount(Long account) {
        this.account = account;
    }

    public Long getAccount() {
        return account;
    }

    public Long getType() {
        return type;
    }

    public void setType(Long type) {
        this.type = type;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public String getMoney() {
        return money;
    }

    public Boolean getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Boolean delFlag) {
        this.delFlag = delFlag;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public PxBookkeepingAccount getAccountObject() {
        return accountObject;
    }

    public void setAccountObject(PxBookkeepingAccount accountObject) {
        this.accountObject = accountObject;
    }

    public PxBookkeepingClassification getTypeObject() {
        return typeObject;
    }

    public void setTypeObject(PxBookkeepingClassification typeObject) {
        this.typeObject = typeObject;
    }

    public Long getOtherAccount() {
        return otherAccount;
    }

    public void setOtherAccount(Long otherAccount) {
        this.otherAccount = otherAccount;
    }

    public PxBookkeepingAccount getOtherAccountObject() {
        return otherAccountObject;
    }

    public void setOtherAccountObject(PxBookkeepingAccount otherAccountObject) {
        this.otherAccountObject = otherAccountObject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PxBookkeepingRecord{" +
                "id=" + id +
                ", version='" + version + '\'' +
                ", name=" + name +
                ", account=" + account +
                ", otherAccount=" + otherAccount +
                ", otherAccountObject=" + otherAccountObject +
                ", accountObject=" + accountObject +
                ", type='" + type + '\'' +
                ", typeObject=" + typeObject +
                ", payTime=" + payTime +
                ", money='" + money + '\'' +
                ", delFlag=" + delFlag +
                '}';
    }
}
