package com.pnkx.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 情侣卡使用记录对象 px_card_record
 *
 * @author pnkx
 * @date 2022-05-22
 */
public class PxCardRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 卡片ID
     */
    @Excel(name = "卡片ID")
    private Long cardId;

    /**
     * 使用用户ID
     */
    @Excel(name = "使用用户ID")
    private Long userId;

    /**
     * 使用说明
     */
    @Excel(name = "使用说明")
    private String instructions;

    /**
     * 确认状态
     */
    @Excel(name = "确认状态")
    private Boolean confirm;

    /**
     * 确认时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;

    /**
     * 评分
     */
    @Excel(name = "评分")
    private Integer score;

    /**
     * 评分时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date scoreTime;

    /**
     * 删除标志
     */
    private Long delFlag;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Long getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Long delFlag) {
        this.delFlag = delFlag;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getConfirm() {
        return confirm;
    }

    public void setConfirm(Boolean confirm) {
        this.confirm = confirm;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Date getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(Date confirmTime) {
        this.confirmTime = confirmTime;
    }

    public Date getScoreTime() {
        return scoreTime;
    }

    public void setScoreTime(Date scoreTime) {
        this.scoreTime = scoreTime;
    }

    @Override
    public String toString() {
        return "PxCardRecord{" +
                "id=" + id +
                ", cardId=" + cardId +
                ", userId=" + userId +
                ", instructions='" + instructions + '\'' +
                ", confirm=" + confirm +
                ", confirmTime=" + confirmTime +
                ", score=" + score +
                ", scoreTime=" + scoreTime +
                ", delFlag=" + delFlag +
                ", version='" + version + '\'' +
                '}';
    }
}
