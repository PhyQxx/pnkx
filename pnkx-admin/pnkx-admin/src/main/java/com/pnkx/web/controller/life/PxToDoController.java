package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxToDo;
import com.pnkx.service.IPxToDoService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 待办事项Controller
 *
 * @author phy
 * @date 2021-04-13
 */
@RestController
@RequestMapping("/admin/toDo")
public class PxToDoController extends BaseController {
    @Resource
    private IPxToDoService pxToDoService;

    /**
     * 查询待办事项列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxToDo pxToDo) {
        startPage();
        List<PxToDo> list = pxToDoService.selectPxToDoList(pxToDo);
        return getDataTable(list);
    }

    /**
     * 导出待办事项列表
     */
    @Log(title = "待办事项", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxToDo pxToDo) {
        List<PxToDo> list = pxToDoService.selectPxToDoList(pxToDo);
        ExcelUtil<PxToDo> util = new ExcelUtil<PxToDo>(PxToDo.class);
        return util.exportExcel(list, "do");
    }

    /**
     * 获取待办事项详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxToDoService.selectPxToDoById(id));
    }

    /**
     * 新增待办事项
     */
    @Log(title = "待办事项", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxToDo pxToDo) {
        pxToDo.setCreateBy(SecurityUtils.getUserId());
        pxToDo.setPerformer(SecurityUtils.getUserId());
        int rows = pxToDoService.insertPxToDo(pxToDo);
        if (rows > 0) {
            return AjaxResult.success(pxToDo.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改待办事项
     */
    @Log(title = "待办事项", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxToDo pxToDo) {
        // 与 add 保持一致：从 SecurityContext 取身份。
        // 不再用 tokenService.getLoginUser——集成令牌（X-Integration-Token）
        // 只建立 SecurityContext、没有 Redis 会话，走会话会 NPE。
        pxToDo.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        return toAjax(pxToDoService.updatePxToDo(pxToDo));
    }

    /**
     * 删除待办事项
     */
    @Log(title = "待办事项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxToDoService.deletePxToDoByIds(ids));
    }

    /**
     * 获取待办事项标签列表
     */
    @GetMapping(value = "/getLabelList")
    public AjaxResult getLabelList() {
        return AjaxResult.success(pxToDoService.getLabelList());
    }

    /**
     * 看板查询：返回三栏（todo/doing/done）
     */
    @GetMapping("/kanban")
    public AjaxResult kanban(PxToDo pxToDo) {
        return AjaxResult.success(pxToDoService.selectKanbanList(pxToDo));
    }

    /**
     * 子任务查询
     */
    @GetMapping("/subtask/{parentId}")
    public AjaxResult subtask(@PathVariable("parentId") Long parentId) {
        return AjaxResult.success(pxToDoService.selectSubTasks(parentId));
    }

    /**
     * 批量更新看板状态+排序（拖拽）
     * Body: [{id, kanbanStatus, sortOrder}, ...]
     */
    @Log(title = "待办看板排序", businessType = BusinessType.UPDATE)
    @PutMapping("/sort")
    public AjaxResult sort(@RequestBody List<PxToDo> list) {
        return toAjax(pxToDoService.updateKanbanSort(list));
    }
}
