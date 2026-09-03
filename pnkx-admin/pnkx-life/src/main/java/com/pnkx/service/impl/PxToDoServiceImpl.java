package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.domain.po.PxToDo;
import com.pnkx.mapper.PxToDoMapper;
import com.pnkx.service.IPxToDoService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 待办事项Service业务层处理
 *
 * @author phy
 * @date 2021-04-13
 */
@Service
public class PxToDoServiceImpl implements IPxToDoService {

    @Resource
    private PxToDoMapper pxToDoMapper;

    @Resource
    private DataPermissionService dataPermissionService;

    /**
     * 查询待办事项
     *
     * @param id 待办事项ID
     * @return 待办事项
     */
    @Override
    public PxToDo selectPxToDoById(Long id) {
        return pxToDoMapper.selectPxToDoById(id);
    }

    /**
     * 查询待办事项列表
     *
     * @param pxToDo 待办事项
     * @return 待办事项
     */
    @Override
    @DataScopeSelf
    public List<PxToDo> selectPxToDoList(PxToDo pxToDo) {
        if (pxToDo.getParams().containsKey("date")) {
            String date = pxToDo.getParams().get("date").toString();
            // 创建一个日期时间格式化对象
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 解析字符串到LocalDateTime对象
            LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
            // 加一个月
            LocalDateTime plusMonth = dateTime.plusMonths(1);
            pxToDo.getParams().put("endDate", plusMonth.format(formatter));
            // 减一个月
            LocalDateTime minusMonth = dateTime.minusMonths(1);
            pxToDo.getParams().put("startDate", minusMonth.format(formatter));
        }
        return pxToDoMapper.selectPxToDoList(pxToDo);
    }

    /**
     * 新增待办事项
     *
     * @param pxToDo 待办事项
     * @return 结果
     */
    @Override
    public int insertPxToDo(PxToDo pxToDo) {
        if (pxToDo.getClientUuid() != null && !pxToDo.getClientUuid().isBlank()) {
            PxToDo existing = pxToDoMapper.selectByClientUuid(pxToDo.getClientUuid());
            if (existing != null) {
                pxToDo.setId(existing.getId());
                return 1;
            }
        }
        pxToDo.setCreateTime(DateUtils.getNowDate());
        return pxToDoMapper.insertPxToDo(pxToDo);
    }

    /**
     * 修改待办事项
     *
     * @param pxToDo 待办事项
     * @return 结果
     */
    @Override
    public int updatePxToDo(PxToDo pxToDo) {
        pxToDo.setUpdateTime(DateUtils.getNowDate());
        return pxToDoMapper.updatePxToDo(pxToDo);
    }

    /**
     * 批量删除待办事项
     *
     * @param ids 需要删除的待办事项ID
     * @return 结果
     */
    @Override
    public int deletePxToDoByIds(Long[] ids) {
        return pxToDoMapper.deletePxToDoByIds(ids);
    }

    /**
     * 删除待办事项信息
     *
     * @param id 待办事项ID
     * @return 结果
     */
    @Override
    public int deletePxToDoById(Long id) {
        return pxToDoMapper.deletePxToDoById(id);
    }

    /**
     * 获取待办事项标签列表
     * @return
     */
    @Override
    public List<String> getLabelList() {
        List<String> result = new ArrayList<>();
        PxToDo query = new PxToDo();
        // 数据权限：管理员不限；否则仅本人+群组成员
        List<Long> visibleUserIds = dataPermissionService.getVisibleUserIds();
        if (visibleUserIds == null) {
            query.getParams().put(DataScopeSelf.SCOPE_ALL, true);
        } else {
            query.getParams().put(DataScopeSelf.SCOPE_ALL, false);
            query.getParams().put(DataScopeSelf.SCOPE_USER_IDS, visibleUserIds);
        }
        List<String> labelList = pxToDoMapper.getLabelList(query);
        labelList.forEach(item -> {
            String[] split = item.split(",");
            for (String s : split) {
                if (!result.contains(s)) {
                    result.add(s);
                }
            }
        });
        return result;
    }

    /** 已完成列默认最多返回条数（避免数据量大时一次拉取上百条） */
    private static final int DONE_MAX_RETURN = 50;

    /**
     * 看板查询：返回三栏（待办/进行中/已完成）
     */
    @DataScopeSelf
    @Override
    public Map<String, Object> selectKanbanList(PxToDo pxToDo) {
        List<PxToDo> all = pxToDoMapper.selectKanbanList(pxToDo);
        List<PxToDo> todoList = new ArrayList<>();
        List<PxToDo> doingList = new ArrayList<>();
        List<PxToDo> doneList = new ArrayList<>();
        for (PxToDo t : all) {
            Integer ks = t.getKanbanStatus();
            if (ks == null) {
                // 兼容旧数据：status=1(已完成) → done，否则 todo
                ks = (t.getStatus() != null && t.getStatus()) ? 2 : 0;
            }
            switch (ks) {
                case 1:
                    doingList.add(t);
                    break;
                case 2:
                    doneList.add(t);
                    break;
                default:
                    todoList.add(t);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("todo", todoList);
        result.put("doing", doingList);
        // 已完成总数（截断前的真实数量，供前端展示）
        result.put("doneTotal", doneList.size());
        // 已完成列截断：默认只返回最近 DONE_MAX_RETURN 条（mapper 已按 create_time desc）
        result.put("done", doneList.size() > DONE_MAX_RETURN
                ? new ArrayList<>(doneList.subList(0, DONE_MAX_RETURN))
                : doneList);
        return result;
    }

    /**
     * 子任务查询
     */
    @Override
    public List<PxToDo> selectSubTasks(Long parentId) {
        return pxToDoMapper.selectSubTasks(parentId);
    }

    /**
     * 批量更新看板状态+排序（拖拽）
     */
    @Override
    public int updateKanbanSort(List<PxToDo> list) {
        int count = 0;
        for (PxToDo t : list) {
            if (t.getId() == null) {
                continue;
            }
            count += pxToDoMapper.updateKanbanSort(t.getId(), t.getKanbanStatus(), t.getSortOrder());
        }
        return count;
    }
}
