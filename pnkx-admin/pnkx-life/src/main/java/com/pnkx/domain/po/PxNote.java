package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author PHY
 * @classname PxNote
 * @data 2021/12/30 17:29
 * @description 笔记对象 px_note
 */
public class PxNote extends BaseEntity
{
    private static final long serialVersionUID=1L;

    /** 主键 */
    private Long id;

    /** 笔记标题 */
    @Excel(name = "笔记标题")
    private String title;

    /** 笔记内容 */
    @Excel(name = "笔记内容")
    private String content;

    /** 笔记内容（富文本形式） */
    @Excel(name = "笔记内容", readConverterExp = "富=文本形式")
    private String richText;

    /** 笔记目录 */
    @Excel(name = "笔记目录")
    private Long folder;

    /** 排序 */
    private Integer order;

    /** 删除标志 */
    private Integer delFlag;

    /** 版本号 */
    @Excel(name = "版本号")
    private String version;

    /** 客户端唯一标识（离线幂等去重） */
    private String clientUuid;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }
    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }
    public void setRichText(String richText)
    {
        this.richText = richText;
    }

    public String getRichText()
    {
        return richText;
    }
    public void setFolder(Long folder)
    {
        this.folder = folder;
    }

    public Long getFolder()
    {
        return folder;
    }
    public void setDelFlag(Integer delFlag)
    {
        this.delFlag = delFlag;
    }

    public Integer getDelFlag()
    {
        return delFlag;
    }
    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public String toString(){
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id",getId())
                .append("title",getTitle())
                .append("content",getContent())
                .append("richText",getRichText())
                .append("folder",getFolder())
                .append("delFlag",getDelFlag())
                .append("order",getOrder())
                .append("version",getVersion())
                .append("createBy",getCreateBy())
                .append("createTime",getCreateTime())
                .append("updateBy",getUpdateBy())
                .append("updateTime",getUpdateTime())
                .append("remark",getRemark())
                .toString();
    }
}
