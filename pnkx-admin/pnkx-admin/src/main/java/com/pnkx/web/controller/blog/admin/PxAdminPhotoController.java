package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxPhoto;
import com.pnkx.service.IPxPhotoService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 相册Controller
 *
 * @author phy
 * @date 2021-02-05
 */
@RestController
@RequestMapping("/admin/photo")
public class PxAdminPhotoController extends BaseController {
    @Resource
    private IPxPhotoService pxPhotoService;

    /**
     * 获取相册列表
     *
     * @param params 参数
     * @return 相册列表
     */
    @PostMapping("/getAlbumList")
    public AjaxResult getAlbumList(@RequestBody Map<String, Object> params) {
        return AjaxResult.success(pxPhotoService.getAlbumList(params));
    }

    /**
     * 查询相册列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxPhoto pxPhoto) {
        startPage();
        List<PxPhoto> list = pxPhotoService.selectPxPhotoList(pxPhoto);
        return getDataTable(list);
    }

    /**
     * 导出相册列表
     */
    @Log(title = "相册", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxPhoto pxPhoto) {
        List<PxPhoto> list = pxPhotoService.selectPxPhotoList(pxPhoto);
        ExcelUtil<PxPhoto> util = new ExcelUtil<PxPhoto>(PxPhoto.class);
        return util.exportExcel(list, "photo");
    }

    /**
     * 获取相册详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return AjaxResult.success(pxPhotoService.selectPxPhotoById(id));
    }

    /**
     * 新增相册
     */
    @Log(title = "相册", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxPhoto pxPhoto) {
        return toAjax(pxPhotoService.insertPxPhoto(pxPhoto));
    }

    /**
     * 修改相册
     */
    @Log(title = "相册", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxPhoto pxPhoto) {
        return toAjax(pxPhotoService.updatePxPhoto(pxPhoto));
    }

    /**
     * 删除相册
     */
    @Log(title = "相册", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(pxPhotoService.deletePxPhotoByIds(ids));
    }
}
