package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @author PHY
 * @classname PxBookkeepingRecord
 * @data 2021/11/18 0018 14:31
 * @description 描述
 */
@Data
public class PxBookkeepingRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

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
    @DateTimeFormat(pattern = "yyyy-MM")
    private Date payTime;

    /**
     * 金额
     */
    @Excel(name = "金额")
    private String money;

    /**
     * 图片
     */
    @Excel(name = "图片")
    private String images;

    /**
     * 删除标志
     */
    private Boolean delFlag;

    /**
     * 客户端唯一标识（离线幂等去重）
     */
    private String clientUuid;

    /**
     * 关联纪念日ID（礼物类支出联动）
     */
    private Long commemorationDayId;

    /**
     * 关联的纪念日对象（非持久化，查询时按需关联返回）
     */
    private PxCommemorationDay commemorationDay;
}
