package com.pnkx.web.controller.blog.client;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.domain.po.PxShareResource;
import com.pnkx.service.IPxShareResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 分享资源Controller
 *
 * @author Codex
 * @date 2026-07-03
 */
@RestController
@RequestMapping("/client/share")
public class PxClientShareResourceController extends BaseController {
    @Resource
    private IPxShareResourceService pxShareResourceService;

    /**
     * 查询启用分享资源列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxShareResource pxShareResource) {
        startPage();
        List<PxShareResource> list = pxShareResourceService.selectClientShareResourceList(pxShareResource);
        return getDataTable(list);
    }

    /**
     * 记录分享资源点击次数
     */
    @PostMapping("/click/{id}")
    public AjaxResult click(@PathVariable("id") Long id) {
        pxShareResourceService.incrementClickCount(id);
        return AjaxResult.success();
    }
}
