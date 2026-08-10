package com.pnkx.system.domain.vo;

import com.pnkx.system.domain.SysNotice;

/**
 * @Classname SysNoticeVo
 * @Description 通知公告视图对象
 * @Date 2021-03-12 16:24
 * @Author by PHY
 */
public class SysNoticeVo extends SysNotice {

    /**
     * 作者昵称
     */
    private String author;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
