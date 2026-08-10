package com.pnkx.mapper;

import com.pnkx.domain.po.PxCustomReplyRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author pnkx
 */
@Mapper
public interface PxCustomReplyRuleMapper {

    /**
     * 查询自定义回复规则
     *
     * @param id 自定义回复规则ID
     * @return 自定义回复规则
     */
    public PxCustomReplyRule selectPxCustomReplyRuleById(Long id);

    /**
     * 查询自定义回复规则列表
     *
     * @param pxCustomReplyRule 自定义回复规则
     * @return 自定义回复规则集合
     */
    public List<PxCustomReplyRule> selectPxCustomReplyRuleList(PxCustomReplyRule pxCustomReplyRule);

    /**
     * 新增自定义回复规则
     *
     * @param pxCustomReplyRule 自定义回复规则
     * @return 结果
     */
    public int insertPxCustomReplyRule(PxCustomReplyRule pxCustomReplyRule);

    /**
     * 修改自定义回复规则
     *
     * @param pxCustomReplyRule 自定义回复规则
     * @return 结果
     */
    public int updatePxCustomReplyRule(PxCustomReplyRule pxCustomReplyRule);

    /**
     * 删除自定义回复规则
     *
     * @param id 自定义回复规则ID
     * @return 结果
     */
    public int deletePxCustomReplyRuleById(Long id);

    /**
     * 批量删除自定义回复规则
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxCustomReplyRuleByIds(Long[] ids);
}
