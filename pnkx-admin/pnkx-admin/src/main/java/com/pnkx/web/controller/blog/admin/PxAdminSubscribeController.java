package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxEmailSubscribe;
import com.pnkx.service.IPxEmailSubscribeService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author by PHY
 * @Classname PxAdminSubscribeController
 * @date 2021-06-17 15:41
 */
@RestController
@RequestMapping("/admin/subscribe")
public class PxAdminSubscribeController extends BaseController {

    @Resource
    IPxEmailSubscribeService pxEmailSubscribeService;

    /**
     * 查询订阅列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxEmailSubscribe pxEmailSubscribe) {
        startPage();
        List<PxEmailSubscribe> list = pxEmailSubscribeService.selectPxEmailSubscribeList(pxEmailSubscribe);
        return getDataTable(list);
    }

    /**
     * 获取订阅详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxEmailSubscribeService.selectPxEmailSubscribeById(id));
    }

    /**
     * 新增订阅
     */
    @Log(title = "订阅", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxEmailSubscribe pxEmailSubscribe) {
        return toAjax(pxEmailSubscribeService.insertPxEmailSubscribe(pxEmailSubscribe));
    }

    /**
     * 修改订阅
     */
    @Log(title = "订阅", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxEmailSubscribe pxEmailSubscribe) {
        return toAjax(pxEmailSubscribeService.updatePxEmailSubscribe(pxEmailSubscribe));
    }

    /**
     * 删除订阅
     */
    @Log(title = "订阅", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxEmailSubscribeService.deletePxEmailSubscribeByIds(ids));
    }
}
