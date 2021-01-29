package com.ruoyi.web.controller.px.admin;

import java.util.List;

import com.ruoyi.domain.po.PxArticle;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.admin.service.IPxAdminArticleService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 文章Controller
 *
 * @author ruoyi
 * @date 2021-01-26
 */
@RestController
@RequestMapping("/px/article")
public class PxAdminArticleController extends BaseController
{
    @Autowired
    private IPxAdminArticleService pxArticleService;

    /**
     * 查询文章列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxArticle pxArticle)
    {
        startPage();
        List<PxArticle> list = pxArticleService.selectPxArticleList(pxArticle);
        return getDataTable(list);
    }

    /**
     * 导出文章列表
     */
    @Log(title = "文章", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxArticle pxArticle)
    {
        List<PxArticle> list = pxArticleService.selectPxArticleList(pxArticle);
        ExcelUtil<PxArticle> util = new ExcelUtil<PxArticle>(PxArticle.class);
        return util.exportExcel(list, "article");
    }

    /**
     * 获取文章详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return AjaxResult.success(pxArticleService.selectPxArticleById(id));
    }

    /**
     * 新增文章
     */
    @Log(title = "文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxArticle pxArticle)
    {
        return toAjax(pxArticleService.insertPxArticle(pxArticle));
    }

    /**
     * 修改文章
     */
    @Log(title = "文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxArticle pxArticle)
    {
        return toAjax(pxArticleService.updatePxArticle(pxArticle));
    }

    /**
     * 删除文章
     */
    @Log(title = "文章", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(pxArticleService.deletePxArticleByIds(ids));
    }
}
