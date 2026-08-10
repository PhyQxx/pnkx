package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxDiary;
import com.pnkx.service.IPxDiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxDiaryController
 * @data 2021/12/30 0030 17:46
 * @description 日记Controller
 */
@RestController
@RequestMapping("/admin/diary")
public class PxDiaryController extends BaseController {
    @Resource
    private IPxDiaryService pxDiaryService;

    /**
     * 查询日记列表
     */
    @GetMapping("/retrieval")
    public AjaxResult retrieval(String searchCode) {
        logger.info("检索条件为：" + searchCode);
        return AjaxResult.success(pxDiaryService.retrieval(searchCode));
    }

    /**
     * 查询日记列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxDiary pxDiary) {
        startPage();
        List<PxDiary> list = pxDiaryService.selectPxDiaryList(pxDiary);
        return getDataTable(list);
    }

    /**
     * 导出日记列表
     */
    @Log(title = "日记", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxDiary pxDiary) {
        List<PxDiary> list = pxDiaryService.selectPxDiaryList(pxDiary);
        ExcelUtil<PxDiary> util = new ExcelUtil<PxDiary>(PxDiary. class);
        return util.exportExcel(list, "diary");
    }

    /**
     * 获取日记详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxDiaryService.selectPxDiaryById(id));
    }

    /**
     * 新增日记
     */
    @Log(title = "日记", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxDiary pxDiary) {
        int rows = pxDiaryService.insertPxDiary(pxDiary);
        if (rows > 0) {
            return AjaxResult.success(pxDiary.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改日记
     */
    @Log(title = "日记", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxDiary pxDiary) {
        return toAjax(pxDiaryService.updatePxDiary(pxDiary));
    }

    /**
     * 删除日记
     */
    @Log(title = "日记", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxDiaryService.deletePxDiaryByIds(ids));
    }
}
