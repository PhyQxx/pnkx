package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 姨妈记录对象 px_menstruation_record
 *
 * @author pnkx
 * @date 2021-12-03
 */
@Data
public class PxMenstruationRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户id
     */
    @Excel(name = "用户id")
    private Long userId;

    /**
     * 时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date date;

    /**
     * 标志（0：大姨妈来了 1：大姨妈走了）
     */
    @Excel(name = "标志", readConverterExp = "0=：大姨妈来了,1=：大姨妈走了")
    private String type;

    /**
     * 心情
     */
    @Excel(name = "心情")
    private String mood;

    /**
     * 是否爱爱
     */
    @Excel(name = "是否爱爱")
    private Boolean makeLove;

    /**
     * 体温（摄氏度）
     */
    @Excel(name = "体温", readConverterExp = "摄=氏度")
    private Double temperature;

    /**
     * 体重（千克）
     */
    @Excel(name = "体重", readConverterExp = "千=克")
    private Double weight;

    /**
     * 状态（记录经期、我在备孕、我怀孕了、我在育儿）
     */
    private String state;

    /**
     * 版本号
     */
    private String version;

    /**
     * 检查项目
     */
    private String items;

    /**
     * 检查结果
     */
    private String results;
}
