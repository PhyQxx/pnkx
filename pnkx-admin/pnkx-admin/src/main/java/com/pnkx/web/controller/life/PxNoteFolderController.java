package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxNoteFolder;
import com.pnkx.service.IPxNoteFolderService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxNoteFolderController
 * @data 2021/12/30 17:37
 * @description 笔记文件夹Controller
 */
@RestController
@RequestMapping("/note/folder")
public class PxNoteFolderController extends BaseController {

    @Resource
    private IPxNoteFolderService pxNoteFolderService;

    /**
     * 查询笔记文件夹列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxNoteFolder pxNoteFolder) {
        startPage();
        List<PxNoteFolder> list = pxNoteFolderService.selectPxNoteFolderList(pxNoteFolder);
        return getDataTable(list);
    }

    /**
     * 查询笔记文件夹树形列表
     */
    @GetMapping("/treeList")
    public AjaxResult treeList(PxNoteFolder pxNoteFolder) {
        return AjaxResult.success(pxNoteFolderService.selectPxNoteFolderTreeList(pxNoteFolder));
    }

    /**
     * 获取笔记文件夹详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxNoteFolderService.selectPxNoteFolderById(id));
    }

    /**
     * 新增笔记文件夹
     */
    @Log(title = "笔记文件夹", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxNoteFolder pxNoteFolder) {
        return AjaxResult.success(pxNoteFolderService.insertPxNoteFolder(pxNoteFolder));
    }

    /**
     * 修改笔记文件夹
     */
    @Log(title = "笔记文件夹", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxNoteFolder pxNoteFolder) {
        return AjaxResult.success(pxNoteFolderService.updatePxNoteFolder(pxNoteFolder));
    }

    /**
     * 删除笔记文件夹
     */
    @Log(title = "笔记文件夹", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxNoteFolderService.deletePxNoteFolderByIds(ids));
    }
}
