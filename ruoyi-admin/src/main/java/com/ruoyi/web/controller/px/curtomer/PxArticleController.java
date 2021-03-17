package com.ruoyi.web.controller.px.curtomer;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.domain.po.PxLeaveMessage;
import com.ruoyi.domain.vo.PxArticleVo;
import com.ruoyi.px.customer.service.impl.PxArticleServiceImpl;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;

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
    public TableDataInfo getArticleTypeNumber(@RequestBody Map<String, Object> params) {
        startPage();
        List<Map<String, Object>> list = pxArticleService.getArticleTypeNumber(params);
        return getDataTable(list);
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
    public TableDataInfo list(PxArticleVo pxArticle)
    {
        startPage();
        List<PxArticleVo> list = pxArticleService.selectPxArticleList(pxArticle);
        list.forEach(article -> {
            article.setRichText(Jsoup.parse(article.getRichText()).text());
        });
        return getDataTable(list);
    }

    /**
     * 查询文章列表不包括内容
     */
    @GetMapping("/listNotContent")
    public TableDataInfo listNotContent(PxArticleVo pxArticle)
    {
        startPage();
        List<PxArticleVo> list = pxArticleService.selectPxArticleNotContentList(pxArticle);
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
     * 根据字典项type获取列表
     * @param params
     * @return
     */
    @RequestMapping("/getArticleTypeList")
    public AjaxResult getArticleTypeList(@RequestBody Map<String, Object> params) {
        return AjaxResult.success("根据字典项type获取列表列表成功", pxArticleService.getArticleTypeList(params));
    }

    /**
     * 查询留言列表
     */
    @GetMapping("/getMessageList")
    public TableDataInfo list(PxLeaveMessage pxLeaveMessage)
    {
        startPage();
        List<PxLeaveMessage> list = pxArticleService.selectPxLeaveMessageList(pxLeaveMessage);
        return getDataTable(list);
    }

    /**
     * 获取首页最热文章
     * @return
     */
    @GetMapping("/getHotArticle")
    public AjaxResult getHotArticle() {
        return AjaxResult.success("获取首页最文章成功", pxArticleService.getHotArticle());
    }


    /**
     * 去除文章的图片内容方法
     * @param body
     * @param str1
     * @param str2
     * @return
     */
    public String subRangeString(String body, String str1, String str2) {
        while (true) {
            int index1 = body.indexOf(str1);
            if (index1 != -1) {
                int index2 = body.indexOf(str2, index1);
                if (index2 != -1) {
                    body = body.substring(0, index1) + body.substring(index2 +    str2.length(), body.length());
                }else {
                    return body;
                }
            }else {
                return body;
            }
        }
    }
}
