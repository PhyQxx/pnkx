package com.pnkx.web.controller.blog.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxFriendLink;
import com.pnkx.service.IPxFriendLinkService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author by PHY
 * @Classname 友链Controller
 * @date 2021-04-30 11:42
 */
@RestController
@RequestMapping("/admin/link")
public class PxAdminFriendLinkController extends BaseController {
    @Resource
    private IPxFriendLinkService pxFriendLinkService;

    /**
     * 查询友链列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxFriendLink pxFriendLink) {
        Page<PxFriendLink> page = buildPage();
        IPage<PxFriendLink> list = pxFriendLinkService.selectPxFriendLinkList(page, pxFriendLink);
        return getDataTable(list);
    }

    /**
     * 导出友链列表
     */
    @Log(title = "友链", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxFriendLink pxFriendLink) {
        List<PxFriendLink> list = pxFriendLinkService.selectPxFriendLinkList(pxFriendLink);
        ExcelUtil<PxFriendLink> util = new ExcelUtil<>(PxFriendLink.class);
        return util.exportExcel(list, "link");
    }

    /**
     * 获取友链详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxFriendLinkService.selectPxFriendLinkById(id));
    }

    /**
     * 新增友链
     */
    @Log(title = "友链", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxFriendLink pxFriendLink) {
        return toAjax(pxFriendLinkService.insertPxFriendLink(pxFriendLink));
    }

    /**
     * 修改友链
     */
    @Log(title = "友链", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxFriendLink pxFriendLink) {
        return toAjax(pxFriendLinkService.updatePxFriendLink(pxFriendLink));
    }

    /**
     * 删除友链
     */
    @Log(title = "友链", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxFriendLinkService.deletePxFriendLinkByIds(ids));
    }
}
