package com.pnkx.web.controller.blog.client;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.domain.vo.PxArticleVo;
import com.pnkx.service.IPxArticleService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 文章表 前端控制器
 *
 * @author 裴浩宇
 * @since 2021-01-10
 */
@RestController
@RequestMapping("/client/article")
public class PxClientArticleController extends BaseController {

    @Resource
    private IPxArticleService pxArticleService;

    /**
     * 获取文章详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return AjaxResult.success(pxArticleService.getArticleById(id));
    }

    /**
     * 查询文章列表
     */
    @Operation(summary = "查询文章列表")
    @Log(title = "查询文章列表")
    @GetMapping("/list")
    public TableDataInfo list(PxArticleVo pxArticle) {
        pxArticle.setOpen("1");
        startPage();
        List<PxArticleVo> list = pxArticleService.selectPxArticleList(pxArticle);
        return getDataTable(list);
    }

    /**
     * 查询文章列表不包括内容
     */
    @GetMapping("/listNotContent")
    public TableDataInfo listNotContent(PxArticleVo pxArticle) {
        pxArticle.setOpen("1");
        startPage();
        List<PxArticleVo> list = pxArticleService.selectPxArticleNotContent(pxArticle);
        return getDataTable(list);
    }

    /**
     * 查询文章列表格式化内容
     */
    @GetMapping("/listOrdinaryContent")
    public TableDataInfo listOrdinaryContent(PxArticleVo pxArticle) {
        pxArticle.setOpen("1");
        startPage();
        List<PxArticleVo> list = pxArticleService.selectPxArticleOrdinaryContent(pxArticle);
        return getDataTable(list);
    }

    /**
     * 获取推荐文章
     *
     * @return
     */
    @GetMapping("/getHotArticle")
    public AjaxResult getHotArticle() {
        return AjaxResult.success("获取推荐文章成功", pxArticleService.getHotArticle());
    }

    /**
     * 文章按类型分组
     */
    @GetMapping("/getArticleListGroupByType")
    public AjaxResult getArticleListGroupByType() {
        return AjaxResult.success(pxArticleService.getArticleListGroupByType());
    }

    /**
     * 文章按类型分组
     */
    @GetMapping("/getArticleUrls")
    public String getArticleUrls() {
        return pxArticleService.getArticleUrls();
    }
}
