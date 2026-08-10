package com.pnkx.mapper;

import com.pnkx.domain.po.PxLeaveMessage;

import java.util.List;

/**
 * 文章Mapper接口
 *
 * @author phy
 * @date 2021-01-26
 */
public interface PxMessageMapper {
    /**
     * 查询留言
     *
     * @param id 留言ID
     * @return 留言
     */
    PxLeaveMessage getMessageById(Long id);
    /**
     * 留言
     *
     * @param pxLeaveMessage 参数
     * @return 留言结果
     */
    Integer addMessage(PxLeaveMessage pxLeaveMessage);

    /**
     * 查询留言列表
     *
     * @param pxLeaveMessage 留言
     * @return 留言集合
     */
    List<PxLeaveMessage> selectPxLeaveMessageList(PxLeaveMessage pxLeaveMessage);

    /**
     * 修改留言
     *
     * @param pxLeaveMessage 留言
     * @return 结果
     */
    int updatePxLeaveMessage(PxLeaveMessage pxLeaveMessage);
}
