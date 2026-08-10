package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订阅管理对象
 *
 * @author PHY
 * @date 2026/07/05
 */
public class PxSubscription extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "订阅名称")
    private String name;

    @Excel(name = "金额")
    private BigDecimal amount;

    @Excel(name = "周期")
    private String cycle;

    @Excel(name = "周期间隔")
    private Integer cycleInterval;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "下次扣费", dateFormat = "yyyy-MM-dd")
    private Date nextPaymentDate;

    private Long accountId;

    private Long classificationId;

    @Excel(name = "支付方式")
    private String paymentMethod;

    @Excel(name = "Logo")
    private String logo;

    @Excel(name = "提前提醒天数")
    private Integer reminderLeadDays;

    @Excel(name = "是否启用")
    private Boolean enabled;

    @Excel(name = "版本号")
    private String version;

    private String clientUuid;

    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCycle() { return cycle; }
    public void setCycle(String cycle) { this.cycle = cycle; }
    public Integer getCycleInterval() { return cycleInterval; }
    public void setCycleInterval(Integer cycleInterval) { this.cycleInterval = cycleInterval; }
    public Date getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(Date nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getClassificationId() { return classificationId; }
    public void setClassificationId(Long classificationId) { this.classificationId = classificationId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public Integer getReminderLeadDays() { return reminderLeadDays; }
    public void setReminderLeadDays(Integer reminderLeadDays) { this.reminderLeadDays = reminderLeadDays; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
