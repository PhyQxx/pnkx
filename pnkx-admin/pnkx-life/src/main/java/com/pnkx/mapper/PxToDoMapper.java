package com.pnkx.mapper;

/**
 * @author by PHY
 * @Classname PxToDoMapper
 * @date 2021-04-13 11:49
 */

import com.pnkx.domain.po.PxToDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 待办事项Mapper接口
 *
 * @author phy
 * @date 2021-04-13
 */
public interface PxToDoMapper {
    /**
     * 查询待办事项
     *
     * @param id 待办事项ID
     * @return 待办事项
     */
    public PxToDo selectPxToDoById(Long id);

    /**
     * 查询待办事项列表
     *
     * @param pxToDo 待办事项
     * @return 待办事项集合
     */
    public List<PxToDo> selectPxToDoList(PxToDo pxToDo);

    /**
     * 新增待办事项
     *
     * @param pxToDo 待办事项
     * @return 结果
     */
    public int insertPxToDo(PxToDo pxToDo);

    /**
     * 修改待办事项
     *
     * @param pxToDo 待办事项
     * @return 结果
     */
    public int updatePxToDo(PxToDo pxToDo);

    /**
     * 删除待办事项
     *
     * @param id 待办事项ID
     * @return 结果
     */
    public int deletePxToDoById(Long id);

    /**
     * 批量删除待办事项
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxToDoByIds(Long[] ids);

    /**
     * 获取待办事项标签列表（params 携带数据权限）
     *
     * @param pxToDo 数据权限载体
     * @return 标签列表
     */
    List<String> getLabelList(PxToDo pxToDo);

    /**
     * 根据客户端唯一标识查询待办（幂等去重）
     */
    PxToDo selectByClientUuid(@Param("clientUuid") String clientUuid);

    /**
     * 增量查询待办（离线同步用）
     */
    List<PxToDo> selectIncremental(@Param("createBy") String createBy, @Param("since") String since, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 看板查询：按 kanban_status 分组返回（含数据权限），排除子任务
     *
     * @param pxToDo 数据权限/筛选载体
     * @return 顶级任务集合（按 状态+排序+创建时间 排序）
     */
    List<PxToDo> selectKanbanList(PxToDo pxToDo);

    /**
     * 子任务查询
     *
     * @param parentId 父任务ID
     * @return 子任务集合
     */
    List<PxToDo> selectSubTasks(@Param("parentId") Long parentId);

    /**
     * 更新看板状态+排序（拖拽用）
     *
     * @param id           待办ID
     * @param kanbanStatus 看板状态
     * @param sortOrder    排序
     * @return 结果
     */
    int updateKanbanSort(@Param("id") Long id, @Param("kanbanStatus") Integer kanbanStatus, @Param("sortOrder") Integer sortOrder);
}
