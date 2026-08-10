package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

/**
 * @author PHY
 */
public class PxPhoto extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 图片名称
     */
    @Excel(name = "图片名称")
    private String name;

    /**
     * 图片Base64码
     */
    @Excel(name = "图片Base64码")
    private String photoBase64;

    /**
     * 文件缩略图路径
     */
    @Excel(name = "文件缩略图路径")
    private String thumbnail;

    /**
     * 图片地址
     */
    @Excel(name = "图片地址")
    private String photoAddress;

    /**
     * 所属相册
     */
    @Excel(name = "所属相册")
    private String type;

    /**
     * 版本号
     */
    private String version;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoAddress(String photoAddress) {
        this.photoAddress = photoAddress;
    }

    public String getPhotoAddress() {
        return photoAddress;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    @Override
    public String toString() {
        return "PxPhoto{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", photoBase64='" + photoBase64 + '\'' +
                ", thumbnail='" + thumbnail + '\'' +
                ", photoAddress='" + photoAddress + '\'' +
                ", type='" + type + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
