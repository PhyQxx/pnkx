package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxNote;
import com.pnkx.domain.po.PxNoteFolder;
import com.pnkx.service.IPxNoteService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxNoteController
 * @data 2021/12/30 17:38
 * @description 笔记Controller
 */
@RestController
@RequestMapping("/note")
public class PxNoteController extends BaseController {
    @Resource
    private IPxNoteService pxNoteService;

    /**
     * 查询笔记列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxNote pxNote) {
        startPage();
        List<PxNoteFolder> list = pxNoteService.selectPxNoteList(pxNote);
        return getDataTable(list);
    }

    /**
     * 导出笔记列表
     */
    @Log(title = "笔记", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxNote pxNote) {
        List<PxNoteFolder> list = pxNoteService.selectPxNoteList(pxNote);
        ExcelUtil<PxNoteFolder> util = new ExcelUtil<>(PxNoteFolder. class);
        return util.exportExcel(list, "note");
    }

    /**
     * 获取笔记详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxNoteService.selectPxNoteById(id));
    }

    /**
     * 新增笔记
     */
    @Log(title = "笔记", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxNote pxNote) {
        return AjaxResult.success(pxNoteService.insertPxNote(pxNote));
    }

    /**
     * 修改笔记
     */
    @Log(title = "笔记", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxNote pxNote) {
        return AjaxResult.success(pxNoteService.updatePxNote(pxNote));
    }

    /**
     * 删除笔记
     */
    @Log(title = "笔记", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxNoteService.deletePxNoteByIds(ids));
    }
}
