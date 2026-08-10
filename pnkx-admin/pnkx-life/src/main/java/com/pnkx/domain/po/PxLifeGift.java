package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * PxLifeGift
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2026/7/3 17:55
 * @description 描述
 */
@Data
public class PxLifeGift extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 礼物记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 礼物名称
     */
    private String giftName;

    /**
     * 礼物图片
     */
    private String giftImage;

    /**
     * 礼物价格
     */
    private Double giftPrice;
}
