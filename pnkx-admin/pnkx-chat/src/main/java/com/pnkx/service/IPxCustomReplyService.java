package com.pnkx.service;

import com.pnkx.domain.po.PxCustomReplyRule;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 自定义回复服务接口
 * 
 * @author pnkx
 */
public interface IPxCustomReplyService {

    /**
     * 根据消息内容匹配自定义回复规则
     *
     * @param messageContent 消息内容
     * @return 匹配的回复内容，未匹配到则返回null
     */
    String matchCustomReply(String messageContent);

    /**
     * 查询自定义回复规则
     *
     * @param id 自定义回复规则ID
     * @return 自定义回复规则
     */
    PxCustomReplyRule selectPxCustomReplyRuleById(@NotNull Long id);

    /**
     * 查询自定义回复规则列表
     *
     * @param pxCustomReplyRule 自定义回复规则查询条件
     * @return 自定义回复规则集合
     */
    List<PxCustomReplyRule> selectPxCustomReplyRuleList(PxCustomReplyRule pxCustomReplyRule);

    /**
     * 新增自定义回复规则
     *
     * @param pxCustomReplyRule 自定义回复规则
     * @return 影响的行数
     */
    int insertPxCustomReplyRule(@NotNull PxCustomReplyRule pxCustomReplyRule);

    /**
     * 修改自定义回复规则
     *
     * @param pxCustomReplyRule 自定义回复规则
     * @return 影响的行数
     */
    int updatePxCustomReplyRule(@NotNull PxCustomReplyRule pxCustomReplyRule);

    /**
     * 批量删除自定义回复规则
     *
     * @param ids 需要删除的自定义回复规则ID数组
     * @return 影响的行数
     */
    int deletePxCustomReplyRuleByIds(@NotNull Long[] ids);

    /**
     * 删除自定义回复规则信息
     *
     * @param id 自定义回复规则ID
     * @return 影响的行数
     */
    int deletePxCustomReplyRuleById(@NotNull Long id);

    /**
     * 搜索知识库并回复
     */
    String searchKnowledgeAndReply(String question);
}
