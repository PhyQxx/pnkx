package com.pnkx.domain.vo;

import com.pnkx.common.annotation.Excel;
import com.pnkx.domain.po.PxArticle;
import lombok.Data;

/**
 * 文章vo
 *
 * @author PHY
 */
@Data
public class PxArticleVo extends PxArticle {

    /**
     * 昵称
     */
    @Excel(name = "昵称")
    private String nickName;

    /**
     * 留言数量
     */
    @Excel(name = "留言数量")
    private Integer leaveMessageNumber;

    /**
     * 搜索内容
     */
    private String search;

    /**
     * 首张图片
     */
    private String firstPicture;

    /**
     * 浏览数量
     */
    private Integer visitsNumber;

    /**
     * 文章类型
     */
    private String typeCode;

    /**
     * 文章类型名称
     */
    private String typeName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 上一篇文章
     */
    private PxArticle lastArticle;

    /**
     * 下一篇文章
     */
    private PxArticle nextArticle;
}
