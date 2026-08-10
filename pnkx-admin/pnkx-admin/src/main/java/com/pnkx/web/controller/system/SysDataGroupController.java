package com.pnkx.web.controller.system;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.system.domain.SysDataGroup;
import com.pnkx.system.service.ISysDataGroupService;

/**
 * 数据权限群组操作处理
 *
 * @author pnkx
 */
@RestController
@RequestMapping("/system/dataGroup")
public class SysDataGroupController extends BaseController {
    @Autowired
    private ISysDataGroupService dataGroupService;

    /**
     * 获取群组列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysDataGroup group) {
        startPage();
        List<SysDataGroup> list = dataGroupService.selectDataGroupList(group);
        return getDataTable(list);
    }

    /**
     * 获取群组详情（含成员用户ID）
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(dataGroupService.selectDataGroupById(id));
    }

    /**
     * 新增群组
     */
    @Log(title = "数据权限群组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDataGroup group) {
        group.setCreateBy(String.valueOf(SecurityUtils.getUserId()));
        return toAjax(dataGroupService.insertDataGroup(group));
    }

    /**
     * 修改群组
     */
    @Log(title = "数据权限群组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDataGroup group) {
        group.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        return toAjax(dataGroupService.updateDataGroup(group));
    }

    /**
     * 删除群组
     */
    @Log(title = "数据权限群组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(dataGroupService.deleteDataGroupById(id));
    }
}
