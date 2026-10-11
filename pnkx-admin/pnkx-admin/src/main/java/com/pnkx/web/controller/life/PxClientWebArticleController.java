package com.pnkx.web.controller.life;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.life.domain.po.PxWebArticle;
import com.pnkx.life.service.IPxWebArticleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxClientWebArticleController
 * @description 网址收藏Controller（客户端，需登录）
 * <p>
 * /client/** 整体匿名放行，但剪藏为用户私有数据，
 * 所有接口内部校验登录态（401）并按 create_by 隔离，与书城一致。
 */
@RestController
@RequestMapping("/client/webArticle")
public class PxClientWebArticleController extends BaseController {

    @Resource
    private IPxWebArticleService pxWebArticleService;

    /**
     * 查询当前用户网址收藏列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxWebArticle pxWebArticle) {
        if (requireLogin() == null) {
            return new TableDataInfo(List.of(), 0);
        }
        startPage();
        List<PxWebArticle> list = pxWebArticleService.selectPxWebArticleList(pxWebArticle);
        return getDataTable(list);
    }

    /**
     * 获取详情（含正文快照）
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        if (requireLogin() == null) {
            return unauthorized();
        }
        return AjaxResult.success(pxWebArticleService.selectPxWebArticleById(id));
    }

    /**
     * 收藏网址（抓取与AI分析异步进行）
     */
    @PostMapping
    public AjaxResult add(@RequestBody PxWebArticle pxWebArticle) {
        if (requireLogin() == null) {
            return unauthorized();
        }
        return AjaxResult.success(pxWebArticleService.addArticleByUrl(pxWebArticle.getUrl(), pxWebArticle.getRemark()));
    }

    /**
     * 人工修正
     */
    @PutMapping
    public AjaxResult edit(@RequestBody PxWebArticle pxWebArticle) {
        if (requireLogin() == null) {
            return unauthorized();
        }
        return AjaxResult.success(pxWebArticleService.updatePxWebArticle(pxWebArticle));
    }

    /**
     * 删除
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        if (requireLogin() == null) {
            return unauthorized();
        }
        return toAjax(pxWebArticleService.deletePxWebArticleByIds(ids));
    }

    /**
     * 标记已读/未读
     */
    @PutMapping("/read/{id}/{readFlag}")
    public AjaxResult markRead(@PathVariable Long id, @PathVariable String readFlag) {
        if (requireLogin() == null) {
            return unauthorized();
        }
        return toAjax(pxWebArticleService.updateReadFlag(id, readFlag) ? 1 : 0);
    }

    /**
     * 星标/取消
     */
    @PutMapping("/star/{id}/{starFlag}")
    public AjaxResult markStar(@PathVariable Long id, @PathVariable String starFlag) {
        if (requireLogin() == null) {
            return unauthorized();
        }
        return toAjax(pxWebArticleService.updateStarFlag(id, starFlag) ? 1 : 0);
    }

    /**
     * 重新抓取并重新分析
     */
    @PutMapping("/refetch/{id}")
    public AjaxResult refetch(@PathVariable Long id) {
        if (requireLogin() == null) {
            return unauthorized();
        }
        return toAjax(pxWebArticleService.retryFetch(id) ? 1 : 0);
    }

    /**
     * 仅重新 AI 分析
     */
    @PutMapping("/reanalyze/{id}")
    public AjaxResult reanalyze(@PathVariable Long id) {
        if (requireLogin() == null) {
            return unauthorized();
        }
        return toAjax(pxWebArticleService.retryAnalyze(id) ? 1 : 0);
    }

    /**
     * 登录态校验：SecurityUtils.getUserId() 未登录时返回空串（内部吞异常），
     * 而非 null，必须归一化，否则 == null 判断全部失效（空串绕过）
     */
    private String requireLogin() {
        String userId = SecurityUtils.getUserId();
        return StringUtils.isEmpty(userId) ? null : userId;
    }

    private AjaxResult unauthorized() {
        return AjaxResult.error(401, "请先登录");
    }
}
