package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.List;

/**
 * @author PHY
 * @classname PxWallpaper
 * @description 壁纸对象 px_wallpaper
 */
@Data
public class PxWallpaper extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 壁纸名称
     */
    @Excel(name = "壁纸名称")
    private String name;

    /**
     * 壁纸地址
     */
    @Excel(name = "壁纸地址")
    private String url;

    /**
     * 缩略图地址
     */
    @Excel(name = "缩略图地址")
    private String thumbnail;

    /**
     * 所属文件夹
     */
    @Excel(name = "所属文件夹")
    private Long folder;

    /**
     * 点赞数
     */
    @Excel(name = "点赞数")
    private Integer likeCount;

    /**
     * 宽度
     */
    @Excel(name = "宽度")
    private Integer width;

    /**
     * 高度
     */
    @Excel(name = "高度")
    private Integer height;

    /**
     * 排序
     */
    private Integer order;

    /**
     * 删除标志
     */
    private Integer delFlag;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 需要排除的文件夹 id 集合（非持久化，仅用于客户端查询时排除停用文件夹子树）
     */
    private List<Long> excludeFolderIds;

    /**
     * 排序方式（非持久化，仅用于客户端查询）：
     * time 最新、name 名称、like 最热，默认 time
     */
    private String sort;
}
