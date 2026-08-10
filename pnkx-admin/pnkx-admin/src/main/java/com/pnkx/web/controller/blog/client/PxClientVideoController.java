package com.pnkx.web.controller.blog.client;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.domain.po.PxVideo;
import com.pnkx.service.IPxVideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 视频模块Controller
 *
 * @author 裴大头
 * @date 2023-04-19
 */
@RestController
@RequestMapping("/client/video")
public class PxClientVideoController extends BaseController {
    @Resource
    private IPxVideoService pxVideoService;

    /**
     * 查询视频模块列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxVideo pxVideo) {
        startPage();
        List<PxVideo> list = pxVideoService.selectPxVideoList(pxVideo);
        return getDataTable(list);
    }

    /**
     * 获取视频模块详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxVideoService.selectPxVideoById(id));
    }
}
