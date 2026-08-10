package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 壁纸下载记录对象 px_wallpaper_download_record
 *
 * @author pnkx
 */
@Data
public class PxWallpaperDownloadRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 壁纸ID
     */
    private Long itemId;

    /**
     * 壁纸名称（冗余）
     */
    private String itemName;

    /**
     * 壁纸缩略图（冗余）
     */
    private String itemThumbnail;

    /**
     * 壁纸原图地址（冗余，缩略图为空时兜底）
     */
    private String itemUrl;

    /**
     * 下载方式 single/zip
     */
    private String downloadType;

    /**
     * 下载用户昵称（JOIN sys_user 取 nick_name，仅管理端记录列表展示用）
     */
    private String createByName;
}
