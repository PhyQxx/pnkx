package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.life.domain.po.PxWebArticle;
import com.pnkx.life.service.IPxWebArticleService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author PHY
 * @classname PxWebArticleController
 * @description 网址收藏Controller（需登录，「我的应用」下个人数据）
 * <p>
 * 抓取与 AI 分析在提交后异步进行，前端通过 fetchStatus / aiStatus 字段轮询展示进度。
 */
@RestController
@RequestMapping("/webArticle")
public class PxWebArticleController extends BaseController {

    @Resource
    private IPxWebArticleService pxWebArticleService;

    /**
     * 查询网址收藏列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxWebArticle pxWebArticle) {
        startPage();
        List<PxWebArticle> list = pxWebArticleService.selectPxWebArticleList(pxWebArticle);
        return getDataTable(list);
    }

    /**
     * 查询当前用户的全部标签（筛选下拉用）
     */
    @GetMapping("/tags")
    public AjaxResult tags() {
        return AjaxResult.success(pxWebArticleService.selectTags());
    }

    /**
     * 标签统计（标签名 + 文章数）
     */
    @GetMapping("/tags/stats")
    public AjaxResult tagStats() {
        return AjaxResult.success(pxWebArticleService.selectTagStats());
    }

    /**
     * 标签重命名（新名已存在时合并），返回受影响文章数
     */
    @Log(title = "网址收藏", businessType = BusinessType.UPDATE)
    @PutMapping("/tags/rename")
    public AjaxResult renameTag(@RequestBody Map<String, String> body) {
        int affected = pxWebArticleService.renameTag(body.get("oldTag"), body.get("newTag"));
        AjaxResult ajax = AjaxResult.success("重命名完成");
        ajax.put("affected", affected);
        return ajax;
    }

    /**
     * 删除标签（从所有文章移除），返回受影响文章数
     */
    @Log(title = "网址收藏", businessType = BusinessType.DELETE)
    @DeleteMapping("/tags/{tag}")
    public AjaxResult deleteTag(@PathVariable String tag) {
        int affected = pxWebArticleService.deleteTag(tag);
        AjaxResult ajax = AjaxResult.success("标签已移除");
        ajax.put("affected", affected);
        return ajax;
    }

    /**
     * 运营统计（总览/分类分布/近30天趋势）
     */
    @GetMapping("/stats")
    public AjaxResult stats() {
        return AjaxResult.success(pxWebArticleService.selectStats());
    }

    /**
     * 获取网址收藏详情（含正文快照）
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxWebArticleService.selectPxWebArticleById(id));
    }

    /**
     * 收藏网址（抓取与AI分析异步进行）
     *
     * @param pxWebArticle url 必填，remark 可选
     */
    @Log(title = "网址收藏", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxWebArticle pxWebArticle) {
        return AjaxResult.success(pxWebArticleService.addArticleByUrl(pxWebArticle.getUrl(), pxWebArticle.getRemark()));
    }

    /**
     * 人工修正（标题/摘要/标签/分类/已读/星标/备注）
     */
    @Log(title = "网址收藏", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxWebArticle pxWebArticle) {
        return AjaxResult.success(pxWebArticleService.updatePxWebArticle(pxWebArticle));
    }

    /**
     * 删除网址收藏
     */
    @Log(title = "网址收藏", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxWebArticleService.deletePxWebArticleByIds(ids));
    }

    /**
     * 标记已读/未读
     *
     * @param readFlag 0未读 1已读
     */
    @PutMapping("/read/{id}/{readFlag}")
    public AjaxResult markRead(@PathVariable Long id, @PathVariable String readFlag) {
        return toAjax(pxWebArticleService.updateReadFlag(id, readFlag) ? 1 : 0);
    }

    /**
     * 星标/取消星标
     *
     * @param starFlag 0取消 1星标
     */
    @PutMapping("/star/{id}/{starFlag}")
    public AjaxResult markStar(@PathVariable Long id, @PathVariable String starFlag) {
        return toAjax(pxWebArticleService.updateStarFlag(id, starFlag) ? 1 : 0);
    }

    /**
     * 重新抓取正文并重新AI分析
     */
    @Log(title = "网址收藏", businessType = BusinessType.UPDATE)
    @PutMapping("/refetch/{id}")
    public AjaxResult refetch(@PathVariable Long id) {
        return toAjax(pxWebArticleService.retryFetch(id) ? 1 : 0);
    }

    /**
     * 仅重新 AI 分析（正文不变）
     */
    @Log(title = "网址收藏", businessType = BusinessType.UPDATE)
    @PutMapping("/reanalyze/{id}")
    public AjaxResult reanalyze(@PathVariable Long id) {
        return toAjax(pxWebArticleService.retryAnalyze(id) ? 1 : 0);
    }

    /**
     * 导出全部收藏为 Markdown 文档
     */
    @Log(title = "网址收藏", businessType = BusinessType.EXPORT)
    @GetMapping("/export/markdown")
    public void exportMarkdown(HttpServletResponse response) throws IOException {
        String markdown = pxWebArticleService.exportMarkdown();
        response.setContentType("text/markdown; charset=utf-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + URLEncoder.encode("网址收藏_" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + ".md", StandardCharsets.UTF_8));
        response.getOutputStream().write(markdown.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }
}
