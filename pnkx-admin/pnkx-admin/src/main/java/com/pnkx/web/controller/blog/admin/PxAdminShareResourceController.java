package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxShareResource;
import com.pnkx.service.IPxShareResourceService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 分享资源Controller
 *
 * @author Codex
 * @date 2026-07-03
 */
@RestController
@RequestMapping("/admin/share")
public class PxAdminShareResourceController extends BaseController {
    @Resource
    private IPxShareResourceService pxShareResourceService;

    /**
     * 查询分享资源列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxShareResource pxShareResource) {
        startPage();
        List<PxShareResource> list = pxShareResourceService.selectPxShareResourceList(pxShareResource);
        return getDataTable(list);
    }

    /**
     * 导出分享资源列表
     */
    @Log(title = "分享资源", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxShareResource pxShareResource) {
        List<PxShareResource> list = pxShareResourceService.selectPxShareResourceList(pxShareResource);
        ExcelUtil<PxShareResource> util = new ExcelUtil<>(PxShareResource.class);
        return util.exportExcel(list, "share");
    }

    /**
     * 获取分享资源详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxShareResourceService.selectPxShareResourceById(id));
    }

    /**
     * 新增分享资源
     */
    @Log(title = "分享资源", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxShareResource pxShareResource) {
        return toAjax(pxShareResourceService.insertPxShareResource(pxShareResource));
    }

    /**
     * 修改分享资源
     */
    @Log(title = "分享资源", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxShareResource pxShareResource) {
        return toAjax(pxShareResourceService.updatePxShareResource(pxShareResource));
    }

    /**
     * 删除分享资源
     */
    @Log(title = "分享资源", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxShareResourceService.deletePxShareResourceByIds(ids));
    }

    /**
     * 获取分享资源标签列表
     */
    @GetMapping(value = "/getLabelList")
    public AjaxResult getLabelList() {
        return AjaxResult.success(pxShareResourceService.getLabelList());
    }
}
