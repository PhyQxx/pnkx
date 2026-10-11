package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.life.domain.po.PxWebRssFeed;
import com.pnkx.life.service.IPxWebRssFeedService;
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

/**
 * @author PHY
 * @classname PxWebRssFeedController
 * @description 网址收藏RSS订阅Controller（需登录，订阅为用户私有数据）
 * <p>
 * 订阅的新文章由定时任务（rssFetchTask，V1.6.1 注册）自动拉取收藏，
 * 也可通过 /fetch/{id} 立即抓取。
 */
@RestController
@RequestMapping("/webArticle/feed")
public class PxWebRssFeedController extends BaseController {

    @Resource
    private IPxWebRssFeedService pxWebRssFeedService;

    /**
     * 查询订阅列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxWebRssFeed pxWebRssFeed) {
        startPage();
        List<PxWebRssFeed> list = pxWebRssFeedService.selectPxWebRssFeedList(pxWebRssFeed);
        return getDataTable(list);
    }

    /**
     * 新增订阅
     */
    @Log(title = "RSS订阅", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxWebRssFeed pxWebRssFeed) {
        return AjaxResult.success(pxWebRssFeedService.insertPxWebRssFeed(pxWebRssFeed));
    }

    /**
     * 修改订阅（名称/链接/启用状态/备注）
     */
    @Log(title = "RSS订阅", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxWebRssFeed pxWebRssFeed) {
        return toAjax(pxWebRssFeedService.updatePxWebRssFeed(pxWebRssFeed) ? 1 : 0);
    }

    /**
     * 删除订阅
     */
    @Log(title = "RSS订阅", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxWebRssFeedService.deletePxWebRssFeedByIds(ids));
    }

    /**
     * 立即抓取该订阅（同步返回新收录条数，单源最多20条）
     */
    @Log(title = "RSS订阅", businessType = BusinessType.UPDATE)
    @PutMapping("/fetch/{id}")
    public AjaxResult fetchNow(@PathVariable Long id) {
        int added = pxWebRssFeedService.fetchFeedNow(id);
        AjaxResult ajax = AjaxResult.success("抓取完成");
        ajax.put("added", added);
        return ajax;
    }

    /**
     * 导出当前用户全部订阅为 OPML（可导入其他 RSS 阅读器）
     */
    @Log(title = "RSS订阅", businessType = BusinessType.EXPORT)
    @GetMapping("/export/opml")
    public void exportOpml(HttpServletResponse response) throws IOException {
        List<PxWebRssFeed> feeds = pxWebRssFeedService.selectPxWebRssFeedList(new PxWebRssFeed());
        StringBuilder opml = new StringBuilder();
        opml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<opml version=\"2.0\">\n  <head>\n    <title>pnkx RSS 订阅</title>\n  </head>\n  <body>\n");
        for (PxWebRssFeed feed : feeds) {
            String title = feed.getName() == null ? "" : feed.getName()
                    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
            String url = feed.getUrl() == null ? "" : feed.getUrl()
                    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
            opml.append("    <outline type=\"rss\" text=\"").append(title)
                    .append("\" title=\"").append(title)
                    .append("\" xmlUrl=\"").append(url).append("\"/>\n");
        }
        opml.append("  </body>\n</opml>\n");
        response.setContentType("text/xml; charset=utf-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + URLEncoder.encode("pnkx_rss_" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + ".opml", StandardCharsets.UTF_8));
        response.getOutputStream().write(opml.toString().getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }
}
