package com.pnkx.domain.vo;

import com.pnkx.common.annotation.Excel;
import com.pnkx.domain.po.PxCardRecord;
import lombok.Data;

/**
 * 情侣卡使用记录对象 px_card_record
 *
 * @author pnkx
 * @date 2022-05-22
 */
@Data
public class PxCardRecordVo extends PxCardRecord {
   /**
     * 卡片名称
     */
    @Excel(name = "卡片名称")
    private String cardName;

    /**
     * 用户姓名
     */
    @Excel(name = "用户姓名")
    private String userName;
}
