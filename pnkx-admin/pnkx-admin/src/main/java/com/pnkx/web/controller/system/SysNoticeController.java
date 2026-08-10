package com.pnkx.web.controller.system;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.system.domain.SysNotice;
import com.pnkx.system.domain.SysNoticeRead;
import com.pnkx.system.domain.vo.SysNoticeVo;
import com.pnkx.system.service.ISysNoticeService;
import com.pnkx.web.websocket.controller.WebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 公告 信息操作处理
 *
 * @author phy
 */
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController {
    @Resource
    private ISysNoticeService noticeService;
    @Resource
    WebSocketController webSocketController;

    /**
     * 获取通知公告列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysNotice notice) {
        startPage();
        List<SysNoticeVo> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }

    /**
     * 获取通知公告已读列表
     */
    @GetMapping("/readList")
    public TableDataInfo selectNoticeRead(SysNoticeRead sysNoticeRead) {
        startPage();
        List<SysNoticeRead> list = noticeService.selectNoticeRead(sysNoticeRead);
        return getDataTable(list);
    }

    /**
     * 查询通知公告未读读列表
     */
    @GetMapping("/getUnreadNoticeList")
    public List<SysNotice> getUnreadNoticeList(SysNoticeRead sysNoticeRead) {
        return noticeService.getUnreadNoticeList(sysNoticeRead);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(HttpServletRequest request, @PathVariable Long noticeId) {
        return AjaxResult.success(noticeService.selectNoticeById(request, noticeId));
    }

    /**
     * 新增通知公告
     */
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice) {
        notice.setCreateBy(SecurityUtils.getUserId());
        int sysNotice = noticeService.insertNotice(notice);
        webSocketController.sendAllMessage(notice);
        return toAjax(sysNotice);
    }

    /**
     * 修改通知公告
     */
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice) {
        notice.setUpdateBy(SecurityUtils.getUserName());
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 删除通知公告
     */
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds) {
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
