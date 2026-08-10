package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxVisits;
import com.pnkx.service.IPxVisitsService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 访客Controller
 *
 * @author phy
 * @date 2021-10-30
 */
@RestController
@RequestMapping("/admin/visits")
public class PxAdminVisitsController extends BaseController {

    @Resource
    private IPxVisitsService pxVisitsService;

    /**
     * 查询访客列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxVisits pxVisits) {
        startPage();
        List<PxVisits> list = pxVisitsService.selectPxVisitsList(pxVisits);
        return getDataTable(list);
    }

    /**
     * 导出访客列表
     */
    @Log(title = "访客", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxVisits pxVisits) {
        List<PxVisits> list = pxVisitsService.selectPxVisitsList(pxVisits);
        ExcelUtil<PxVisits> util = new ExcelUtil<PxVisits>(PxVisits.class);
        return util.exportExcel(list, "visits");
    }

    /**
     * 删除访客
     */
    @Log(title = "访客", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxVisitsService.deletePxVisitsByIds(ids));
    }
}
