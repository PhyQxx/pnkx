package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 视频模块对象 px_video
 *
 * @author 裴大头
 * @date 2023-04-19
 */
@Data
public class PxVideo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 标题
     */
    @Excel(name = "标题")
    private String title;

    /**
     * 封面
     */
    @Excel(name = "封面")
    private String cover;

    /**
     * 视频地址
     */
    @Excel(name = "视频地址")
    private String url;

    /**
     * 标签
     */
    @Excel(name = "标签")
    private String label;

    /**
     * 分类
     */
    @Excel(name = "分类")
    private String type;

    /**
     * 浏览次数
     */
    private Integer visits;

    /**
     * 点赞次数
     */
    private Integer videoLikeNumber;

    /**
     * 留言次数
     */
    private Integer leaveMessageNumber;

    /**
     * 删除标志
     */
    private Long delFlag;

    /**
     * 版本号
     */
    private String version;
}
