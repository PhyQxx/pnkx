package com.pnkx.common.utils.ip;

import lombok.Data;

import java.io.Serializable;

/**
 * IpLocation
 *
 * @author 裴浩宇
 * @date 2023/8/12 11:37
 */
@Data
public class IpLocation implements Serializable {

    /**
     * ip地址
     */
    private String ip;

    /**
     * 国家
     */
    private String country;

    /**
     * 省
     */
    private String province;

    /**
     * 省
     */
    private String city;

    /**
     * 服务商
     */
    private String isp;
}
