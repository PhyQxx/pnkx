package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 分享资源对象 px_share_resource
 *
 * @author Codex
 * @date 2026-07-03
 */
@Data
public class PxShareResource extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 云盘 */
    @Excel(name = "云盘")
    private String diskType;

    /** 类型 */
    @Excel(name = "类型")
    private String resourceType;

    /** 分享链接 */
    @Excel(name = "分享链接")
    private String shareUrl;

    /** 封面 */
    @Excel(name = "封面")
    private String cover;

    /** 提取码 */
    @Excel(name = "提取码")
    private String extractCode;

    /** 二维码 */
    @Excel(name = "二维码")
    private String qrCode;

    /** 标签 */
    @Excel(name = "标签")
    private String tags;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 状态（0停用 1启用） */
    @Excel(name = "状态")
    private String status;

    /** 删除标志 */
    private Long delFlag;

    /** 点击次数 */
    @Excel(name = "点击次数")
    private Integer clickCount;

    /** 版本号 */
    private String version;
}
