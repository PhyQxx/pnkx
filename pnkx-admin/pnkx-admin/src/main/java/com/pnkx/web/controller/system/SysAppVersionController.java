package com.pnkx.web.controller.system;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.system.domain.SysAppVersion;
import com.pnkx.system.service.ISysAppVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * App版本管理Controller
 *
 * @author pnkx
 * @date 2024-07-25
 */
@RestController
@RequestMapping("/system/version")
public class SysAppVersionController extends BaseController {

    @Autowired
    private ISysAppVersionService sysAppVersionService;

    /**
     * 查询App版本列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysAppVersion sysAppVersion) {
        startPage();
        List<SysAppVersion> list = sysAppVersionService.selectSysAppVersionList(sysAppVersion);
        return getDataTable(list);
    }

    /**
     * 获取App版本详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(sysAppVersionService.selectSysAppVersionById(id));
    }

    /**
     * 新增App版本
     */
    @Log(title = "App版本管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysAppVersion sysAppVersion) {
        return toAjax(sysAppVersionService.insertSysAppVersion(sysAppVersion));
    }

    /**
     * 修改App版本
     */
    @Log(title = "App版本管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysAppVersion sysAppVersion) {
        return toAjax(sysAppVersionService.updateSysAppVersion(sysAppVersion));
    }

    /**
     * 删除App版本
     */
    @Log(title = "App版本管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(sysAppVersionService.deleteSysAppVersionByIds(ids));
    }
}
