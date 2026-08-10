package com.pnkx.domain.po;

import java.io.Serializable;
import java.util.List;

/**
 * TXT 书籍解析预览。
 */
public class PxBookTxtPreview implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String suggestedTitle;
    private String encoding;
    private Integer chapterCount;
    private Integer totalWordCount;
    private List<Chapter> chapters;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSuggestedTitle() { return suggestedTitle; }
    public void setSuggestedTitle(String suggestedTitle) { this.suggestedTitle = suggestedTitle; }
    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }
    public Integer getChapterCount() { return chapterCount; }
    public void setChapterCount(Integer chapterCount) { this.chapterCount = chapterCount; }
    public Integer getTotalWordCount() { return totalWordCount; }
    public void setTotalWordCount(Integer totalWordCount) { this.totalWordCount = totalWordCount; }
    public List<Chapter> getChapters() { return chapters; }
    public void setChapters(List<Chapter> chapters) { this.chapters = chapters; }

    public static class Chapter implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer chapterNo;
        private String chapterName;
        private Integer wordCount;

        public Chapter() {}

        public Chapter(Integer chapterNo, String chapterName, Integer wordCount) {
            this.chapterNo = chapterNo;
            this.chapterName = chapterName;
            this.wordCount = wordCount;
        }

        public Integer getChapterNo() { return chapterNo; }
        public void setChapterNo(Integer chapterNo) { this.chapterNo = chapterNo; }
        public String getChapterName() { return chapterName; }
        public void setChapterName(String chapterName) { this.chapterName = chapterName; }
        public Integer getWordCount() { return wordCount; }
        public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    }
}
