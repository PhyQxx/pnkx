package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 情侣卡券对象 px_lovers_card
 *
 * @author pnkx
 * @date 2022-05-21
 */
@Data
public class PxLoversCard extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 卡片名称
     */
    @Excel(name = "卡片名称")
    private String title;

    /**
     * 卡片描述
     */
    @Excel(name = "卡片描述")
    private String describe;

    /**
     * 卡片logo
     */
    @Excel(name = "卡片logo")
    private String logo;

    /**
     * 卡片logo缩略图
     */
    @Excel(name = "卡片logo缩略图")
    private String thumbnail;

    /**
     * 价值金额
     */
    @Excel(name = "价值金额")
    private Integer money;

    /**
     * 定期发放数量
     */
    @Excel(name = "定期发放数量")
    private Integer number;

    /**
     * 删除标志
     */
    private Long delFlag;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;
}
