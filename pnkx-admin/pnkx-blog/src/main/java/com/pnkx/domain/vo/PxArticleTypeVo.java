package com.pnkx.domain.vo;

import com.pnkx.common.core.domain.entity.SysDictData;

/**
 * 文章类型vo
 *
 * @author 裴浩宇
 */
public class PxArticleTypeVo extends SysDictData {

    /**
     * 文章数量
     */
    private Integer articleNumber;

    public Integer getArticleNumber() {
        return articleNumber;
    }

    public void setArticleNumber(Integer articleNumber) {
        this.articleNumber = articleNumber;
    }
}
