package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * @author PHY
 * @classname PxWallpaperFolder
 * @description 壁纸文件夹对象 px_wallpaper_folder
 */
@Data
public class PxWallpaperFolder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 文件夹名称
     */
    @Excel(name = "文件夹名称")
    private String name;

    /**
     * 封面地址
     */
    @Excel(name = "封面地址")
    private String cover;

    /**
     * 父级id
     */
    @Excel(name = "父级id")
    private Long parentId;

    /**
     * 排序
     */
    private Integer order;

    /**
     * 删除标志
     */
    private Integer delFlag;

    /**
     * 是否启用（0否 1是）
     */
    @Excel(name = "是否启用", readConverterExp = "0=未启用,1=启用")
    private Integer enabled;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 壁纸数量
     */
    @Excel(name = "壁纸数量")
    private Integer wallpaperCount;
}
