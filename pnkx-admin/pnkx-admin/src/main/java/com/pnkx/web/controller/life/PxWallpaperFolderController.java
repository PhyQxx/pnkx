package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxWallpaperFolder;
import com.pnkx.service.IPxWallpaperFolderService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxWallpaperFolderController
 * @description 壁纸文件夹Controller（需登录，用于后台维护）
 */
@RestController
@RequestMapping("/wallpaper/folder")
public class PxWallpaperFolderController extends BaseController {

    @Resource
    private IPxWallpaperFolderService pxWallpaperFolderService;

    /**
     * 查询壁纸文件夹列表（全量，供壁纸页左侧文件夹树加载）
     * 不分页：左树需一次性拿到全部文件夹及其子树壁纸数量（递归 CTE 计算），
     * PageHelper 对 CTE 的分页拦截不稳定，故此处不调用 startPage。
     */
    @GetMapping("/list")
    public TableDataInfo list(PxWallpaperFolder pxWallpaperFolder) {
        List<PxWallpaperFolder> list = pxWallpaperFolderService.selectPxWallpaperFolderList(pxWallpaperFolder);
        return getDataTable(list);
    }

    /**
     * 查询壁纸文件夹分页列表（供壁纸文件夹管理页表格分页）
     * 使用不含递归 CTE 的轻量查询，保证分页稳定。
     * 路径用 /pageList 而非 /page，避免被 /{id} 路径变量吞掉（"page" 无法转 Long）。
     */
    @GetMapping("/pageList")
    public TableDataInfo pageList(PxWallpaperFolder pxWallpaperFolder) {
        startPage();
        List<PxWallpaperFolder> list = pxWallpaperFolderService.selectPxWallpaperFolderPage(pxWallpaperFolder);
        return getDataTable(list);
    }

    /**
     * 导出壁纸文件夹列表
     */
    @Log(title = "壁纸文件夹", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxWallpaperFolder pxWallpaperFolder) {
        List<PxWallpaperFolder> list = pxWallpaperFolderService.selectPxWallpaperFolderList(pxWallpaperFolder);
        ExcelUtil<PxWallpaperFolder> util = new ExcelUtil<>(PxWallpaperFolder.class);
        return util.exportExcel(list, "wallpaperFolder");
    }

    /**
     * 获取壁纸文件夹详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxWallpaperFolderService.selectPxWallpaperFolderById(id));
    }

    /**
     * 新增壁纸文件夹
     */
    @Log(title = "壁纸文件夹", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxWallpaperFolder pxWallpaperFolder) {
        return AjaxResult.success(pxWallpaperFolderService.insertPxWallpaperFolder(pxWallpaperFolder));
    }

    /**
     * 修改壁纸文件夹
     */
    @Log(title = "壁纸文件夹", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxWallpaperFolder pxWallpaperFolder) {
        return AjaxResult.success(pxWallpaperFolderService.updatePxWallpaperFolder(pxWallpaperFolder));
    }

    /**
     * 删除壁纸文件夹
     */
    @Log(title = "壁纸文件夹", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxWallpaperFolderService.deletePxWallpaperFolderByIds(ids));
    }
}
