package com.pnkx.mapper;

import com.pnkx.domain.po.PxChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author pnkx
 */
@Mapper
public interface PxChatMessageMapper {

    List<PxChatMessage> selectByUserIdWithLimit(@Param("userId") String userId, @Param("limit") Integer limit);

    List<PxChatMessage> selectByGroupIdWithLimit(@Param("groupId") String groupId, @Param("limit") Integer limit);

    List<PxChatMessage> selectByTimeRange(@Param("startTime") Date startTime,
                                          @Param("endTime") Date endTime);

    PxChatMessage selectByMessageId(@Param("messageId") String messageId);

    /**
     * 查询聊天记录
     *
     * @param id 聊天记录ID
     * @return 聊天记录
     */
    public PxChatMessage selectPxChatMessageById(Long id);

    /**
     * 查询聊天记录列表
     *
     * @param pxChatMessage 聊天记录
     * @return 聊天记录集合
     */
    public List<PxChatMessage> selectPxChatMessageList(PxChatMessage pxChatMessage);

    /**
     * 新增聊天记录
     *
     * @param pxChatMessage 聊天记录
     * @return 结果
     */
    public int insertPxChatMessage(PxChatMessage pxChatMessage);

    /**
     * 修改聊天记录
     *
     * @param pxChatMessage 聊天记录
     * @return 结果
     */
    public int updatePxChatMessage(PxChatMessage pxChatMessage);

    /**
     * 删除聊天记录
     *
     * @param id 聊天记录ID
     * @return 结果
     */
    public int deletePxChatMessageById(Long id);

    /**
     * 批量删除聊天记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxChatMessageByIds(Long[] ids);

    long count();
}
