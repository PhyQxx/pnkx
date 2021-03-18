package com.ruoyi.domain.po;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 留言对象 px_leave_message
 *
 * @author ruoyi
 * @date 2021-03-09
 */
public class PxLeaveMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 回复ID*/
    private Long replyId;

    /** 版本号 */
    @Excel(name = "版本号")
    private String version;

    /** 文章ID */
    @Excel(name = "文章ID")
    private String articleId;

    /** 留言内容 */
    @Excel(name = "留言内容")
    private String content;

    /** 留言人名称 */
    @Excel(name = "留言人名称")
    private String authorName;

    /** 留言人邮箱 */
    @Excel(name = "留言人邮箱")
    private String authorMailbox;

    /** 留言人头像图片id */
    @Excel(name = "留言人头像图片id")
    private String authorHeader;

    /** 留言类型0：文章留言；1：留言板留言；2：相册留言 */
    @Excel(name = "留言类型0：文章留言；1：留言板留言；2：相册留言")
    private String messageBoard;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }
    public void setArticleId(String articleId)
    {
        this.articleId = articleId;
    }

    public String getArticleId()
    {
        return articleId;
    }
    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }
    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public String getAuthorName()
    {
        return authorName;
    }
    public void setAuthorMailbox(String authorMailbox)
    {
        this.authorMailbox = authorMailbox;
    }

    public String getAuthorMailbox()
    {
        return authorMailbox;
    }
    public void setAuthorHeader(String authorHeader)
    {
        this.authorHeader = authorHeader;
    }

    public String getAuthorHeader()
    {
        return authorHeader;
    }
    public void setMessageBoard(String messageBoard)
    {
        this.messageBoard = messageBoard;
    }

    public String getMessageBoard()
    {
        return messageBoard;
    }

    public Long getReplyId() {
        return replyId;
    }

    public void setReplyId(Long replyId) {
        this.replyId = replyId;
    }

    @Override
    public String toString() {
        return "PxLeaveMessage{" +
                "id=" + id +
                ", replyId=" + replyId +
                ", version='" + version + '\'' +
                ", articleId='" + articleId + '\'' +
                ", content='" + content + '\'' +
                ", authorName='" + authorName + '\'' +
                ", authorMailbox='" + authorMailbox + '\'' +
                ", authorHeader='" + authorHeader + '\'' +
                ", messageBoard='" + messageBoard + '\'' +
                '}';
    }
}
