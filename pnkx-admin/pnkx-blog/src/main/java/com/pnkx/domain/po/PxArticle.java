package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 文章对象 px_article
 *
 * @author phy
 * @date 2021-01-26
 */
@Data
public class PxArticle extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 文章标题
     */
    @Excel(name = "文章标题")
    private String title;

    /**
     * 文章封面
     */
    @Excel(name = "文章封面")
    private String cover;

    /**
     * 文章内容
     */
    private String content;

    /**
     * 文章内容（富文本形式）
     */
    @Excel(name = "文章内容", readConverterExp = "富文本形式")
    private String richText;

    /**
     * 文章标签
     */
    @Excel(name = "文章标签")
    private String tag;

    /**
     * 文章分类
     */
    @Excel(name = "文章分类")
    private String type;

    /**
     * 状态
     */
    private String state;

    /**
     * 是否公开
     */
    private String open;

    /**
     * 点赞数
     */
    private Integer likeNumber;

    /**
     * 版本号
     */
    private String version;
}
