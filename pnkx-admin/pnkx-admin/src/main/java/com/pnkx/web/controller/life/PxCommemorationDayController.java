package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxCommemorationDay;
import com.pnkx.service.IPxCommemorationDayService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxAdminCommemorationDayController
 * @data 2021/11/29 16:34
 * @description 纪念日Controller
 */
@RestController
@RequestMapping("/commemorationDay")
public class PxCommemorationDayController extends BaseController {
    @Resource
    private IPxCommemorationDayService pxCommemorationDayService;

    /**
     * 查询纪念日列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxCommemorationDay pxCommemorationDay) {
        startPage();
        List<PxCommemorationDay> list = pxCommemorationDayService.selectPxCommemorationDayList(pxCommemorationDay);
        return getDataTable(list);
    }

    /**
     * 获取纪念日详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxCommemorationDayService.selectPxCommemorationDayById(id));
    }

    /**
     * 新增纪念日
     */
    @Log(title = "纪念日", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxCommemorationDay pxCommemorationDay) {
        int rows = pxCommemorationDayService.insertPxCommemorationDay(pxCommemorationDay);
        if (rows > 0) {
            return AjaxResult.success(pxCommemorationDay.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改纪念日
     */
    @Log(title = "纪念日", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxCommemorationDay pxCommemorationDay) {
        return toAjax(pxCommemorationDayService.updatePxCommemorationDay(pxCommemorationDay));
    }

    /**
     * 删除纪念日
     */
    @Log(title = "纪念日", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxCommemorationDayService.deletePxCommemorationDayByIds(ids));
    }
}
