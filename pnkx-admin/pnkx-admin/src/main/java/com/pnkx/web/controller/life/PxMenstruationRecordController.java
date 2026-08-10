package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.service.IPxMenstruationRecordService;
import com.pnkx.domain.po.PxMenstruationRecord;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 姨妈记录Controller
 *
 * @author pnkx
 * @date 2021-12-03
 */
@RestController
@RequestMapping("/myTool/menstruationRecord")
public class PxMenstruationRecordController extends BaseController {
    @Resource
    private IPxMenstruationRecordService pxMenstruationRecordService;

    /**
     * 查询姨妈记录列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxMenstruationRecord pxMenstruationRecord) {
        startPage();
        List<PxMenstruationRecord> list = pxMenstruationRecordService.selectPxMenstruationRecordList(pxMenstruationRecord);
        return getDataTable(list);
    }

    /**
     * 查询姨妈记录列表
     */
    @GetMapping("/getPxMenstruationRecordList")
    public TableDataInfo getPxMenstruationRecordList(PxMenstruationRecord pxMenstruationRecord) {
        startPage();
        List<PxMenstruationRecord> list = pxMenstruationRecordService.getPxMenstruationRecordList(pxMenstruationRecord);
        return getDataTable(list);
    }

    /**
     * 导出姨妈记录列表
     */
    @Log(title = "姨妈记录", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxMenstruationRecord pxMenstruationRecord) {
        List<PxMenstruationRecord> list = pxMenstruationRecordService.selectPxMenstruationRecordList(pxMenstruationRecord);
        ExcelUtil<PxMenstruationRecord> util = new ExcelUtil<PxMenstruationRecord>(PxMenstruationRecord.class);
        return util.exportExcel(list, "menstruationRecord");
    }

    /**
     * 获取姨妈记录详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxMenstruationRecordService.selectPxMenstruationRecordById(id));
    }

    /**
     * 获取最后一次姨妈开始的记录
     */
    @GetMapping(value = "/getLastStartDate")
    public AjaxResult getLastStartDate() {
        return AjaxResult.success(pxMenstruationRecordService.getLastStartDate());
    }

    /**
     * 新增姨妈记录
     */
    @Log(title = "姨妈记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxMenstruationRecord pxMenstruationRecord) {
        return toAjax(pxMenstruationRecordService.insertPxMenstruationRecord(pxMenstruationRecord));
    }

    /**
     * 修改姨妈记录
     */
    @Log(title = "姨妈记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxMenstruationRecord pxMenstruationRecord) {
        return toAjax(pxMenstruationRecordService.updatePxMenstruationRecord(pxMenstruationRecord));
    }

    /**
     * 删除姨妈记录
     */
    @Log(title = "姨妈记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxMenstruationRecordService.deletePxMenstruationRecordByIds(ids));
    }
}
