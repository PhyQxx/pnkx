package com.pnkx.system.domain;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 通知已读 SysNoticeRead
 *
 * @author phy
 * @date 2021-03-09
 */
@Data
public class SysNoticeRead extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 通知公告ID
     */
    @Excel(name = "通知公告ID")
    private Long noticeId;

    /**
     * IP地址
     */
    @Excel(name = "IP地址")
    private String ip;

    @Excel(name = "经纬度", readConverterExp = "$column.readConverterExp()")
    private String location;

    /**
     * 国家
     */
    @Excel(name = "国家")
    private String country;

    /**
     * 省份
     */
    @Excel(name = "省份")
    private String province;

    /**
     * 城市
     */
    @Excel(name = "城市")
    private String city;

    /**
     * 昵称
     */
    private String nickName;
}
