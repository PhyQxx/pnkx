package com.ruoyi.web.controller.px.curtomer;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.domain.po.PxPhoto;
import com.ruoyi.px.customer.service.IPxPhotoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 相册Controller
 *
 * @author ruoyi
 * @date 2021-02-05
 */
@RestController
@RequestMapping("/customer/photo")
public class PxPhotoController extends BaseController
{
    @Resource
    private IPxPhotoService pxPhotoService;

    /**
     * 查询相册列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxPhoto pxPhoto)
    {
        startPage();
        List<PxPhoto> list = pxPhotoService.selectPxPhotoList(pxPhoto);
        return getDataTable(list);
    }

    /**
     * 导出相册列表
     */
    @Log(title = "相册", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxPhoto pxPhoto)
    {
        List<PxPhoto> list = pxPhotoService.selectPxPhotoList(pxPhoto);
        ExcelUtil<PxPhoto> util = new ExcelUtil<PxPhoto>(PxPhoto.class);
        return util.exportExcel(list, "photo");
    }

    /**
     * 获取相册详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return AjaxResult.success(pxPhotoService.selectPxPhotoById(id));
    }

    /**
     * 新增相册
     */
    @Log(title = "相册", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxPhoto pxPhoto)
    {
        return toAjax(pxPhotoService.insertPxPhoto(pxPhoto));
    }

    /**
     * 修改相册
     */
    @Log(title = "相册", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxPhoto pxPhoto)
    {
        return toAjax(pxPhotoService.updatePxPhoto(pxPhoto));
    }

    /**
     * 删除相册
     */
    @Log(title = "相册", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(pxPhotoService.deletePxPhotoByIds(ids));
    }

    /**
     * 获取相册列表
     * @param params
     * @return
     */
    @RequestMapping("/getAlbumList")
    public AjaxResult getAlbumList(@RequestBody Map<String, Object> params) {
        return AjaxResult.success(pxPhotoService.getAlbumList(params));
    }
}
