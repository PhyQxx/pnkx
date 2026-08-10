package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * @author PHY
 * @classname PxDiary
 * @data 2021/12/30 0030 17:41
 * @description 日记对象 px_diary
 */
public class PxDiary extends BaseEntity
{
    private static final long serialVersionUID=1L;

    /** 主键 */
    private Long id;

    /** 文章标题 */
    @Excel(name = "文章标题")
    private String title;

    /** 心情 */
    @Excel(name = "心情")
    private String mood;

    /** 天气 */
    @Excel(name = "天气")
    private String weather;

    /** 时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date date;

    /** 日记内容 */
    @Excel(name = "日记内容")
    private String content;

    /** 日记内容（富文本形式） */
    @Excel(name = "日记内容", readConverterExp = "富=文本形式")
    private String richText;

    /** 删除标志 */
    private Integer delFlag;

    /** 版本号 */
    @Excel(name = "版本号")
    private String version;

    /** 客户端唯一标识（离线幂等去重） */
    private String clientUuid;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRichText() {
        return richText;
    }

    public void setRichText(String richText) {
        this.richText = richText;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    @Override
    public String toString() {
        return "PxDiary{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", mood='" + mood + '\'' +
                ", weather='" + weather + '\'' +
                ", date=" + date +
                ", content='" + content + '\'' +
                ", richText='" + richText + '\'' +
                ", delFlag=" + delFlag +
                ", version='" + version + '\'' +
                '}';
    }
}
