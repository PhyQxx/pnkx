package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxVideo;
import com.pnkx.service.IPxVideoService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 视频模块Controller
 *
 * @author 裴大头
 * @date 2023-04-19
 */
@RestController
@RequestMapping("/admin/video")
public class PxAdminVideoController extends BaseController {
    @Resource
    private IPxVideoService pxVideoService;

    /**
     * 查询视频模块列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxVideo pxVideo) {
        startPage();
        List<PxVideo> list = pxVideoService.selectPxVideoList(pxVideo);
        return getDataTable(list);
    }

    /**
     * 导出视频模块列表
     */
    @Log(title = "视频模块", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxVideo pxVideo) {
        List<PxVideo> list = pxVideoService.selectPxVideoList(pxVideo);
        ExcelUtil<PxVideo> util = new ExcelUtil<PxVideo>(PxVideo.class);
        return util.exportExcel(list, "video");
    }

    /**
     * 获取视频模块详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxVideoService.selectPxVideoById(id));
    }

    /**
     * 新增视频模块
     */
    @Log(title = "视频模块", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxVideo pxVideo) {
        return toAjax(pxVideoService.insertPxVideo(pxVideo));
    }

    /**
     * 修改视频模块
     */
    @Log(title = "视频模块", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxVideo pxVideo) {
        return toAjax(pxVideoService.updatePxVideo(pxVideo));
    }

    /**
     * 删除视频模块
     */
    @Log(title = "视频模块", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxVideoService.deletePxVideoByIds(ids));
    }

    /**
     * 获取视频标签列表
     */
    @GetMapping(value = "/getLabelList")
    public AjaxResult getLabelList() {
        return AjaxResult.success(pxVideoService.getLabelList());
    }
}
