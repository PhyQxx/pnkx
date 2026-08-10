package com.pnkx.service;


import com.pnkx.domain.po.PxToDo;

import java.util.List;

/**
 * 待办事项Service接口
 *
 * @author phy
 * @date 2021-04-13
 */
public interface IPxToDoService {
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
     * 批量删除待办事项
     *
     * @param ids 需要删除的待办事项ID
     * @return 结果
     */
    public int deletePxToDoByIds(Long[] ids);

    /**
     * 删除待办事项信息
     *
     * @param id 待办事项ID
     * @return 结果
     */
    public int deletePxToDoById(Long id);

    /**
     * 获取待办事项标签列表
     *
     * @return
     */
    List<String> getLabelList();

    /**
     * 看板查询：返回三栏（待办/进行中/已完成）
     *
     * @param pxToDo 筛选载体
     * @return 看板分组数据（todo/doing/done 为列表，doneTotal 为已完成总数）
     */
    java.util.Map<String, Object> selectKanbanList(PxToDo pxToDo);

    /**
     * 子任务查询
     *
     * @param parentId 父任务ID
     * @return 子任务集合
     */
    List<PxToDo> selectSubTasks(Long parentId);

    /**
     * 批量更新看板状态+排序（拖拽）
     *
     * @param list 待办列表（含 id/kanbanStatus/sortOrder）
     * @return 结果
     */
    int updateKanbanSort(List<PxToDo> list);
}
