package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author PHY
 * @classname PxNoteFolder
 * @data 2021/12/30 17:29
 * @description 笔记文件夹对象 px_note_folder
 */
@Data
public class PxNoteFolder extends PxNote
{
    private static final long serialVersionUID=1L;

    /** 主键 */
    private Long id;

    /** 文件夹名称 */
    @Excel(name = "文件夹名称")
    private String name;

    /** 父级id */
    @Excel(name = "父级id")
    private Long parentId;

    /** 阅读密码 */
    @Excel(name = "阅读密码")
    private String password;

    /** 排序 */
    private Integer order;

    /** 删除标志 */
    private Integer delFlag;

    /** 版本号 */
    @Excel(name = "版本号")
    private String version;

    /** 笔记数量 */
    @Excel(name = "笔记数量")
    private Integer noteCount;
}
