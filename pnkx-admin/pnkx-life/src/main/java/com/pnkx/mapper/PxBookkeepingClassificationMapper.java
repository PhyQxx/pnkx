package com.pnkx.mapper;

import com.pnkx.domain.po.PxBookkeepingClassification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author PHY
 */
@Mapper
public interface PxBookkeepingClassificationMapper {
    /**
     * 查询账本分类
     *
     * @param id 账本分类ID
     * @return 账本分类
     */
    public PxBookkeepingClassification selectPxBookkeepingClassificationById(Long id);

    /**
     * 查询账本分类列表
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 账本分类集合
     */
    public List<PxBookkeepingClassification> selectPxBookkeepingClassificationList(PxBookkeepingClassification pxBookkeepingClassification);

    /**
     * 新增账本分类
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 结果
     */
    public int insertPxBookkeepingClassification(PxBookkeepingClassification pxBookkeepingClassification);

    /**
     * 修改账本分类
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 结果
     */
    public int updatePxBookkeepingClassification(PxBookkeepingClassification pxBookkeepingClassification);

    /**
     * 删除账本分类
     *
     * @param id 账本分类ID
     * @return 结果
     */
    public int deletePxBookkeepingClassificationById(Long id);

    /**
     * 批量删除账本分类
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxBookkeepingClassificationByIds(Long[] ids);

    /**
     * 查询最近使用分类列表
     *
     * @param pxBookkeepingClassification 分类
     * @return 分类列表
     */
    List<PxBookkeepingClassification> getLatelyTypeList(PxBookkeepingClassification pxBookkeepingClassification);

    /**
     * 获取一级分类下的二级分类
     *
     * @param parentId 父级ID
     * @return 二级分类列表
     */
    List<Long> getTypeListByParentId(Long parentId);

    /**
     * 增量查询（离线同步用）
     */
    List<PxBookkeepingClassification> selectIncremental(@Param("createBy") String createBy, @Param("since") String since, @Param("offset") int offset, @Param("limit") int limit);
}
