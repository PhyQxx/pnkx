package com.pnkx.domain.po;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

/**
 * 点赞记录对象 px_like_record
 *
 * @author pnkx
 * @date 2023-08-25
 */
@Data
public class PxLikeRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 点赞id
     */
    @Excel(name = "点赞id")
    private Long itemId;

    /**
     * 类型：0：文章；1：评论；2：壁纸点赞
     */
    @Excel(name = "类型")
    private String type;

    /**
     * 删除标志
     */
    private Long delFlag;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 目标名称（冗余，壁纸名等，用于记录列表展示）
     */
    private String itemName;

    /**
     * 目标缩略图地址（冗余，用于记录列表展示）
     */
    private String itemThumbnail;

    /**
     * 目标原图地址（冗余，缩略图为空时兜底）
     */
    private String itemUrl;

    /**
     * 操作用户昵称（JOIN sys_user 取 nick_name，仅管理端记录列表展示用）
     */
    private String createByName;
}
