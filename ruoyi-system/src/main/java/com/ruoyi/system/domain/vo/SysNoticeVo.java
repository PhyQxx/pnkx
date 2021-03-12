package com.ruoyi.system.domain.vo;

import com.ruoyi.system.domain.SysNotice;

/**
 * @Classname SysNoticeVo
 * @Description TODO
 * @Date 2021-03-12 16:24
 * @Author by PHY
 */
public class SysNoticeVo extends SysNotice {

    /**作者昵称*/
    private String author;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
