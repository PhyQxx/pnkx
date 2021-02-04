package com.ruoyi.domain.vo;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.domain.po.PxArticle;

/**
 * @author PHY
 */
public class PxArticleVo extends PxArticle {

    /** 昵称 */
    @Excel(name = "昵称")
    private String nickName;

    /** 搜索内容 */
    private String search;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}
