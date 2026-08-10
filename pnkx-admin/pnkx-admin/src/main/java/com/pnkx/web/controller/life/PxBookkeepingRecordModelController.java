package com.pnkx.web.controller.life;


import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxBookkeepingRecordModel;
import com.pnkx.service.IPxBookkeepingRecordModelService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 账本记录模板Controller
 *
 * @author pnkx
 * @date 2021-12-08
 */
@RestController
@RequestMapping("/bookkeeping/recordModel")
public class PxBookkeepingRecordModelController extends BaseController {
    @Resource
    private IPxBookkeepingRecordModelService pxBookkeepingRecordModelService;

    /**
     * 查询账本记录模板列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxBookkeepingRecordModel pxBookkeepingRecordModel) {
        startPage();
        List<PxBookkeepingRecordModel> list = pxBookkeepingRecordModelService.selectPxBookkeepingRecordModelList(pxBookkeepingRecordModel);
        return getDataTable(list);
    }

    /**
     * 导出账本记录模板列表
     */
    @Log(title = "账本记录模板", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxBookkeepingRecordModel pxBookkeepingRecordModel) {
        List<PxBookkeepingRecordModel> list = pxBookkeepingRecordModelService.selectPxBookkeepingRecordModelList(pxBookkeepingRecordModel);
        ExcelUtil<PxBookkeepingRecordModel> util = new ExcelUtil<PxBookkeepingRecordModel>(PxBookkeepingRecordModel.class);
        return util.exportExcel(list, "model");
    }

    /**
     * 获取账本记录模板详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxBookkeepingRecordModelService.selectPxBookkeepingRecordModelById(id));
    }

    /**
     * 新增账本记录模板
     */
    @Log(title = "账本记录模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxBookkeepingRecordModel pxBookkeepingRecordModel) {
        return toAjax(pxBookkeepingRecordModelService.insertPxBookkeepingRecordModel(pxBookkeepingRecordModel));
    }

    /**
     * 修改账本记录模板
     */
    @Log(title = "账本记录模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxBookkeepingRecordModel pxBookkeepingRecordModel) {
        return toAjax(pxBookkeepingRecordModelService.updatePxBookkeepingRecordModel(pxBookkeepingRecordModel));
    }

    /**
     * 删除账本记录模板
     */
    @Log(title = "账本记录模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxBookkeepingRecordModelService.deletePxBookkeepingRecordModelByIds(ids));
    }
}
