package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * @author PHY
 * @classname PxCommemorationDay
 * @data 2021/11/29 16:28
 * @description 纪念日对象
 */
public class PxCommemorationDay extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 纪念日名称
     */
    @Excel(name = "纪念日名称")
    private String name;

    /**
     * 图标名称
     */
    @Excel(name = "图标名称")
    private String icon;

    /**
     * 纪念日时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;

    /**
     * 是否重复
     */
    @Excel(name = "是否重复")
    private Boolean repeat;

    /**
     * 排序
     */
    @Excel(name = "排序")
    private Long orderNum;

    /**
     * 删除标志
     */
    private Boolean delFlag;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 客户端唯一标识（离线幂等去重）
     */
    private String clientUuid;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setOrderNum(Long orderNum) {
        this.orderNum = orderNum;
    }

    public Long getOrderNum() {
        return orderNum;
    }

    public void setDelFlag(Boolean delFlag) {
        this.delFlag = delFlag;
    }

    public Boolean getDelFlag() {
        return delFlag;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getRepeat() {
        return repeat;
    }

    public void setRepeat(Boolean repeat) {
        this.repeat = repeat;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("name", getName())
                .append("icon", getIcon())
                .append("date", getDate())
                .append("isRepeat", getRepeat())
                .append("orderNum", getOrderNum())
                .append("delFlag", getDelFlag())
                .append("version", getVersion())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
