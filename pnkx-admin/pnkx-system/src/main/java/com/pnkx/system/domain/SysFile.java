package com.pnkx.system.domain;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * @author by PHY
 * @Classname SysFile
 * @date 2021-06-18 16:10
 */
@Data
public class SysFile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long id;
    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件地址
     */
    private String url;
    /**
     * 文件路径
     */
    @Excel(name = "文件路径")
    private String path;

    /**
     * 文件缩略图路径
     */
    @Excel(name = "文件缩略图路径")
    private String thumbnail;

    /**
     * 端口
     */
    @Excel(name = "端口")
    private String port;

    /**
     * 类型
     */
    @Excel(name = "类型")
    private String type;

    /**
     * 点赞
     */
    @Excel(name = "点赞")
    private Integer thumb;

    /**
     * 浏览
     */
    @Excel(name = "浏览")
    private Integer browse;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;
}
