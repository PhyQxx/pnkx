package com.pnkx.mapper;

import com.pnkx.domain.po.PxCustomReplyContent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author pnkx
 */
@Mapper
public interface PxCustomReplyContentMapper {

    /**
     * 查询回复内容
     *
     * @param id 回复内容ID
     * @return 回复内容
     */
    public PxCustomReplyContent selectPxCustomReplyContentById(Long id);

    /**
     * 根据规则ID查询回复内容列表
     *
     * @param ruleId 规则ID
     * @return 回复内容集合
     */
    public List<PxCustomReplyContent> selectPxCustomReplyContentByRuleId(Long ruleId);

    /**
     * 新增回复内容
     *
     * @param pxCustomReplyContent 回复内容
     * @return 结果
     */
    public int insertPxCustomReplyContent(PxCustomReplyContent pxCustomReplyContent);

    /**
     * 修改回复内容
     *
     * @param pxCustomReplyContent 回复内容
     * @return 结果
     */
    public int updatePxCustomReplyContent(PxCustomReplyContent pxCustomReplyContent);

    /**
     * 删除回复内容
     *
     * @param id 回复内容ID
     * @return 结果
     */
    public int deletePxCustomReplyContentById(Long id);

    /**
     * 根据规则ID删除回复内容
     *
     * @param ruleId 规则ID
     * @return 结果
     */
    public int deletePxCustomReplyContentByRuleId(Long ruleId);

    /**
     * 批量删除回复内容
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxCustomReplyContentByIds(Long[] ids);
}
