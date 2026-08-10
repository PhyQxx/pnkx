package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;

/**
 * 我的书城书籍。
 */
public class PxBook extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    @NotBlank(message = "书名不能为空")
    private String title;
    private String author;
    private String description;
    private String status;
    private String delFlag;
    private Integer chapterCount;
    private Long firstChapterId;
    private Long lastReadChapterId;
    private String lastReadChapterName;
    private Date lastReadTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Integer getChapterCount() { return chapterCount; }
    public void setChapterCount(Integer chapterCount) { this.chapterCount = chapterCount; }
    public Long getFirstChapterId() { return firstChapterId; }
    public void setFirstChapterId(Long firstChapterId) { this.firstChapterId = firstChapterId; }
    public Long getLastReadChapterId() { return lastReadChapterId; }
    public void setLastReadChapterId(Long lastReadChapterId) { this.lastReadChapterId = lastReadChapterId; }
    public String getLastReadChapterName() { return lastReadChapterName; }
    public void setLastReadChapterName(String lastReadChapterName) { this.lastReadChapterName = lastReadChapterName; }
    public Date getLastReadTime() { return lastReadTime; }
    public void setLastReadTime(Date lastReadTime) { this.lastReadTime = lastReadTime; }
}
