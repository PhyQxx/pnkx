package com.pnkx.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 统一日历事件 VO
 * <p>
 * 把待办/纪念日/经期/记账等不同来源聚合成统一结构，供前端日历视图渲染。
 *
 * @author PHY
 * @date 2026/07/04
 */
public class CalendarEventVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 来源类型：todo 待办 / commemoration 纪念日 / menstruation 经期 / bookkeeping 记账
     */
    private String sourceType;

    /**
     * 来源实体ID
     */
    private Long sourceId;

    /**
     * 事件标题（展示用）
     */
    private String title;

    /**
     * 事件日期（按天聚合的键）
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date;

    /**
     * 前端着色用 key：todo/commemoration/menstruation/bookkeeping
     */
    private String color;

    /**
     * 前端路由（点击跳转）
     */
    private String route;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }
}
