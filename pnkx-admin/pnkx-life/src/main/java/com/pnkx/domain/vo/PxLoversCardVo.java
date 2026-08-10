package com.pnkx.domain.vo;

import com.pnkx.common.annotation.Excel;
import com.pnkx.domain.po.PxLoversCard;

/**
 * 情侣卡券对象 px_lovers_card
 *
 * @author pnkx
 * @date 2022-05-21
 */
public class PxLoversCardVo extends PxLoversCard {
    private static final long serialVersionUID = 1L;
    /**
     * 卡片ID
     */
    @Excel(name = "卡片ID")
    private Long cardId;

    /**
     * 用户ID
     */
    @Excel(name = "用户ID")
    private Long userId;

    /**
     * 卡片数量
     */
    @Excel(name = "卡片数量")
    private Long cardNumber;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Long cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    @Override
    public String toString() {
        return "PxLoversCardVo{" +
                "cardId=" + cardId +
                ", userId=" + userId +
                ", cardNumber=" + cardNumber +
                '}';
    }
}
