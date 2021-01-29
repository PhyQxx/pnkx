package com.ruoyi.web.controller.px.curtomer;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.px.service.impl.PxArticleServiceImpl;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
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
@RequestMapping("/px-article")
public class PxArticleController {

    @Resource
    PxArticleServiceImpl pxArticleService;

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
}
