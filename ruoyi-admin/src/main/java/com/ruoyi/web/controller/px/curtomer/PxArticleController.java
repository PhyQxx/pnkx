package com.ruoyi.web.controller.px.curtomer;

import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.px.admin.service.IPxAdminArticleService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.domain.po.PxArticle;
import com.ruoyi.px.customer.service.impl.PxArticleServiceImpl;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章表 前端控制器
 * </p>
 *
 * @author 裴浩宇
 * @since 2021-01-10
 */
@RestController
@RequestMapping("/customer")
public class PxArticleController extends BaseController {

    @Resource
    private PxArticleServiceImpl pxArticleService;
    @Resource
    private IPxAdminArticleService iPxAdminArticleService;

    /**
     * 获取文章列表
     * @param params 参数
     * @return 文章列表
     */
    @RequestMapping("/getArticleList")
    public AjaxResult getArticleList(@RequestBody Map<String, Object> params) {
        return AjaxResult.success("获取文章列表成功", pxArticleService.getArticleList(params));
    }

    /**
     * 获取文章分类分组数据
     * @param params 参数
     * @return 文章分类分组数据
     */
    @RequestMapping("/getArticleTypeNumber")
    public AjaxResult getArticleTypeNumber(@RequestBody Map<String, Object> params) {
        return AjaxResult.success("获取文章列表成功", pxArticleService.getArticleTypeNumber(params));
    }

    /**
     * 根据id获取文章留言列表数据
     * @param params 参数
     * @return 文章留言列表数据
     */
    @RequestMapping("/getLeaveMessageByArticleId")
    public AjaxResult getLeaveMessageByArticleId(@RequestBody Map<String, Object> params) {
        return AjaxResult.success("获取文章留言列表成功", pxArticleService.getLeaveMessageByArticleId(params));
    }

    /**
     * 留言
     * @param params 参数
     * @return 留言结果
     */
    @RequestMapping("/addMessage")
    public AjaxResult addMessage(@RequestBody Map<String, Object> params) {
        return AjaxResult.success("留言成功", pxArticleService.addMessage(params));
    }

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
     * 获取文章详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return AjaxResult.success(pxArticleService.selectPxArticleById(id));
    }

    /**
     * 获取文章分类列表
     * @param params
     * @return
     */
    @RequestMapping("/getArticleTypeList")
    public AjaxResult getArticleTypeList(@RequestBody Map<String, Object> params) {
        return AjaxResult.success("获取文章类型列表成功", pxArticleService.getArticleTypeList(params));
    }
}
