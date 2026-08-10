package com.pnkx.domain.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * @author by PHY
 * @Classname 友链对象 px_friend_link
 * @date 2021-04-30 11:39
 */
@Data
@TableName("px_friend_link")
public class PxFriendLink extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 友链图片
     */
    @Excel(name = "友链图片")
    private String img;

    /**
     * 友链标题
     */
    @Excel(name = "友链标题")
    private String title;

    /**
     * 友链url
     */
    @Excel(name = "友链url")
    private String url;

    /**
     * 邮箱
     */
    @Excel(name = "邮箱")
    private String email;

    /**
     * 是否接受邮箱提示
     */
    @Excel(name = "是否接受邮箱提示")
    private String emailFlag;

    /**
     * 状态
     */
    @Excel(name = "状态")
    @TableField(insertStrategy = FieldStrategy.NEVER)
    private String status;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;
}
