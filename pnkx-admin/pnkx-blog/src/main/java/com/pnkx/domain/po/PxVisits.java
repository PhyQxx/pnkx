package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 访客对象 px_visits
 *
 * @author phy
 * @date 2021-10-30
 */
public class PxVisits extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * $column.columnComment
     */
    private Long id;

    /**
     * 国家
     */
    @Excel(name = "国家")
    private String country;

    /**
     * IP地址
     */
    @Excel(name = "IP地址")
    private String ip;

    /**
     * 省份
     */
    @Excel(name = "省份")
    private String province;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 城市
     */
    @Excel(name = "城市")
    private String city;

    /**
     * 区县
     */
    @Excel(name = "区县")
    private String district;

    /**
     * 运营商（如电信、联通、移动）
     */
    @Excel(name = "运营商", readConverterExp = "如=电信、联通、移动")
    private String isp;

    /**
     * $column.columnComment
     */
    @Excel(name = "经纬度", readConverterExp = "$column.readConverterExp()")
    private String location;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("country", getCountry())
                .append("ip", getIp())
                .append("province", getProvince())
                .append("version", getVersion())
                .append("city", getCity())
                .append("createBy", getCreateBy())
                .append("district", getDistrict())
                .append("createTime", getCreateTime())
                .append("isp", getIsp())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("location", getLocation())
                .append("remark", getRemark())
                .toString();
    }
}
