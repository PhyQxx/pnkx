package com.ruoyi.px.customer.mapper;

import com.ruoyi.domain.po.PxLeaveMessage;

import java.util.List;

public interface PxLeaveMessageMapper {
    /**
     * 查询留言列表
     *
     * @param pxLeaveMessage 留言
     * @return 留言集合
     */
    public List<PxLeaveMessage> selectPxLeaveMessageList(PxLeaveMessage pxLeaveMessage);
}
