package com.ruoyi.domain.vo;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.domain.po.PxArticle;

/**
 * @author PHY
 */
public class PxArticleVo extends PxArticle {

    @Excel(name = "昵称")
    private String nickName;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}
