package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 书籍章节。
 */
public class PxBookChapter extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;
    private String bookTitle;
    @NotBlank(message = "章节名不能为空")
    private String chapterName;
    @NotNull(message = "章节序号不能为空")
    private Integer chapterNo;
    private String content;
    private Integer wordCount;
    private String delFlag;
    private Boolean convertHtml;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getChapterName() { return chapterName; }
    public void setChapterName(String chapterName) { this.chapterName = chapterName; }
    public Integer getChapterNo() { return chapterNo; }
    public void setChapterNo(Integer chapterNo) { this.chapterNo = chapterNo; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Boolean getConvertHtml() { return convertHtml; }
    public void setConvertHtml(Boolean convertHtml) { this.convertHtml = convertHtml; }
}
