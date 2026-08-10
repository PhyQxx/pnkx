package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.entity.SysDictData;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxArticle;
import com.pnkx.domain.vo.PxArticleTypeVo;
import com.pnkx.domain.vo.PxArticleVo;
import com.pnkx.service.IPxArticleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 文章Controller
 *
 * @author phy
 * @date 2021-01-26
 */
@Tag(name = "博客管理-文章管理")
@RestController
@RequestMapping("/admin/article")
public class PxAdminArticleController extends BaseController {

    @Resource
    private IPxArticleService pxArticleService;

    /**
     * 查询文章列表
     */
    @Operation(summary = "查询文章列表")
    @Log(title = "查询文章列表")
    @GetMapping("/list")
    public TableDataInfo list(PxArticleVo pxArticle) {
        startPage();
        List<PxArticleVo> list = pxArticleService.selectPxArticleList(pxArticle);
        return getDataTable(list);
    }

    /**
     * 查询文章列表不包含内容
     */
    @Operation(summary = "查询文章列表不包含内容")
    @Log(title = "查询文章列表不包含内容")
    @GetMapping("/listArticleNotContent")
    public TableDataInfo listArticleNotContent(PxArticleVo pxArticle) {
        startPage();
        List<PxArticleVo> list = pxArticleService.selectPxArticleNotContent(pxArticle);
        return getDataTable(list);
    }

    /**
     * 导出文章列表
     */
    @Operation(summary = "导出文章列表")
    @Log(title = "导出文章列表", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxArticle pxArticle) {
        List<PxArticleVo> list = pxArticleService.selectPxArticleList(pxArticle);
        ExcelUtil<PxArticleVo> util = new ExcelUtil<>(PxArticleVo.class);
        return util.exportExcel(list, "article");

    }

    /**
     * 获取文章详细信息
     */
    @Operation(summary = "获取文章详细信息")
    @Log(title = "获取文章详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return AjaxResult.success(pxArticleService.getArticleById(id));
    }

    /**
     * 新增文章
     */
    @Log(title = "文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxArticleVo pxArticle) {
        return AjaxResult.success(pxArticleService.insertPxArticle(pxArticle));
    }

    /**
     * 修改文章
     */
    @Log(title = "文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxArticleVo pxArticle) {
        return toAjax(pxArticleService.updatePxArticle(pxArticle));
    }

    /**
     * 删除文章
     */
    @Log(title = "文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(pxArticleService.deletePxArticleByIds(ids));
    }

    /**
     * 校验字典项标签，键值唯一性
     *
     * @param dictData
     * @return
     */
    @RequestMapping("/dictDataCheckUniqueness")
    public AjaxResult dictDataCheckUniqueness(SysDictData dictData) {
        Integer res = pxArticleService.dictDataCheckUniqueness(dictData);
        return AjaxResult.success(res);
    }

    /**
     * 获取文章类型列表
     *
     * @param dictData
     * @return
     */
    @GetMapping("/getArticleTypeList")
    public TableDataInfo list(SysDictData dictData) {
        List<PxArticleTypeVo> list = pxArticleService.selectPxArticleByType(dictData);
        return getDataTable(list);
    }
    /**
     * 获取待办事项标签列表
     */
    @PostMapping(value = "/getLabelList")
    public AjaxResult getLabelList() {
        return AjaxResult.success(pxArticleService.getLabelList());
    }

}
