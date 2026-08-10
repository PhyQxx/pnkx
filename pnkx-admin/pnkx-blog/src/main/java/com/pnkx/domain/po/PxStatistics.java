package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author by phy
 * @classname Statistics
 * @date 2022-07-01 08:49
 * @description: 统计实体
 */
public class PxStatistics {

    /**
     * 统计时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotBlank(message = "统计时间不能为空")
    private Date date;

    /**
     * 统计维度-时间
     */
    @NotBlank(message = "统计维度-时间不能为空")
    private String dateDimension;

    /**
     * 统计维度-业务
     */
    @NotBlank(message = "统计维度-业务不能为空")
    private String businessDimension;

    /**
     * 统计开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;

    /**
     * 统计结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;

    /**
     * 统计数据
     */
    private List<Map<String, Object>> list;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDateDimension() {
        return dateDimension;
    }

    public void setDateDimension(String dateDimension) {
        this.dateDimension = dateDimension;
    }

    public String getBusinessDimension() {
        return businessDimension;
    }

    public void setBusinessDimension(String businessDimension) {
        this.businessDimension = businessDimension;
    }

    public List<Map<String, Object>> getList() {
        return list;
    }

    public void setList(List<Map<String, Object>> list) {
        this.list = list;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "date=" + date +
                ", dateDimension='" + dateDimension + '\'' +
                ", businessDimension='" + businessDimension + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", list=" + list +
                '}';
    }
}
