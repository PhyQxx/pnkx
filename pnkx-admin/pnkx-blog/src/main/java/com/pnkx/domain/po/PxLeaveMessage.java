package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.List;

/**
 * 留言对象 px_leave_message
 *
 * @author phy
 * @date 2021-03-09
 */
@Data
public class PxLeaveMessage extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * $column.columnComment
     */
    private Long id;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 回复ID
     */
    private Long replyId;

    /**
     * 回复数量
     */
    private Integer replyNumber;

    /**
     * 回复用户ID
     */
    private Long replyUserId;

    /**
     * 回复用户昵称
     */
    @Excel(name = "回复用户昵称")
    private String replyNickName;

    /**
     * 回复列表
     */
    private List<PxLeaveMessage> replyList;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 文章ID
     */
    @Excel(name = "文章ID")
    private String articleId;

    /**
     * 文章标题
     */
    private String articleTitle;

    /**
     * 文章点赞数
     */
    private Integer articleLikeNumber;

    /**
     * 评论点赞数
     */
    private Integer commentLikeNumber;

    /**
     * 点赞数
     */
    private Integer likeNumber;

    /**
     * 留言内容
     */
    @Excel(name = "留言内容")
    private String content;

    /**
     * 留言人名称
     */
    @Excel(name = "留言人名称")
    private String authorName;

    /**
     * 博客主人昵称
     */
    private String nickName;

    /**
     * 留言人网站
     */
    @Excel(name = "留言人网站")
    private String authorAddress;

    /**
     * 留言人邮箱
     */
    @Excel(name = "留言人邮箱")
    private String authorMailbox;

    /**
     * 博客主人名称
     */
    private String avatar;

    /**
     * 博客主人邮箱
     */
    private String email;

    /**
     * 状态
     */
    private String state;

    /**
     * IP地址
     */
    @Excel(name = "IP地址")
    private String ip;

    @Excel(name = "经纬度", readConverterExp = "$column.readConverterExp()")
    private String location;

    /**
     * 国家
     */
    @Excel(name = "国家")
    private String country;

    /**
     * 省份
     */
    @Excel(name = "省份")
    private String province;

    /**
     * 城市
     */
    @Excel(name = "城市")
    private String city;

    /**
     * 是否接受邮件提示
     */
    private Boolean emailFlag;


    /**
     * 留言类型0：文章留言；1：留言板留言；2：相册留言；3：友链留言
     */
    @Excel(name = "留言类型0：文章留言；1：留言板留言；2：相册留言；3：友链留言")
    private String messageBoard;
}
