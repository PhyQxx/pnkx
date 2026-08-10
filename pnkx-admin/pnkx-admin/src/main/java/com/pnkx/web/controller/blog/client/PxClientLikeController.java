package com.pnkx.web.controller.blog.client;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxLikeRecord;
import com.pnkx.service.IPxLikeRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 点赞记录Controller
 *
 * @author pnkx
 * @date 2023-08-25
 */
@RestController
@RequestMapping("/client/like")
public class PxClientLikeController extends BaseController {

    @Resource
    private IPxLikeRecordService pxLikeRecordService;

    /**
     * 查询点赞记录列表
     */
    @GetMapping("/list")
    public AjaxResult list(PxLikeRecord pxLikeRecord) {
        List<PxLikeRecord> list = pxLikeRecordService.selectPxLikeRecordList(pxLikeRecord);
        return AjaxResult.success(list);
    }

    /**
     * 文章点赞
     */
    @Log(title = "点赞记录", businessType = BusinessType.INSERT)
    @GetMapping("/likeArticle/{id}")
    public AjaxResult likeArticle(@PathVariable("id") Long id) {
        return AjaxResult.success(pxLikeRecordService.like(id, "0"));
    }

    /**
     * 评论点赞
     */
    @Log(title = "点赞记录", businessType = BusinessType.INSERT)
    @GetMapping("/likeComment/{id}")
    public AjaxResult likeComment(@PathVariable("id") Long id) {
        return AjaxResult.success(pxLikeRecordService.like(id, "1"));
    }
}
