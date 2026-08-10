package com.pnkx.domain.po;

import lombok.Data;

@Data
public class FileProperties {
    /**
     * 文件类型
     */
    private String content_type;
    
    /**
     * 图片高度（如果是图片）
     */
    private Integer height;
    
    /**
     * 图片宽度（如果是图片）
     */
    private Integer width;
    
    /**
     * 文件名
     */
    private String name;
    
    /**
     * 文件大小
     */
    private Long size;
}
