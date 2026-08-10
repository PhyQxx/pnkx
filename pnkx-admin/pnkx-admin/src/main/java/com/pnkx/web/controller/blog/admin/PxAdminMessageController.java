package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ip.IpLocation;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.domain.po.PxLeaveMessage;
import com.pnkx.service.IPxMessageService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 留言Controller
 *
 * @author phy
 * @date 2021-01-26
 */
@RestController
@RequestMapping("/admin/message")
public class PxAdminMessageController extends BaseController {

    @Resource
    private IPxMessageService pxMessageService;

    /**
     * 留言
     *
     * @param pxLeaveMessage 参数
     * @return 留言结果
     */
    @RequestMapping("/addMessage")
    public AjaxResult addMessage(HttpServletRequest request, @RequestBody PxLeaveMessage pxLeaveMessage) {
        String ipAddr = IpUtils.getIpAddr(request);
        IpLocation location = IpUtils.getLocation(ipAddr);
        pxLeaveMessage.setIp(ipAddr);
        String rectangle = IpUtils.getRectangle(ipAddr);
        pxLeaveMessage.setLocation(rectangle);
        pxLeaveMessage.setCountry(location.getCountry());
        pxLeaveMessage.setProvince(location.getProvince());
        pxLeaveMessage.setCity(location.getCity());
        return AjaxResult.success("留言成功", pxMessageService.addMessage(pxLeaveMessage));
    }

    /**
     * 查询留言列表
     */
    @GetMapping("/getMessageList")
    public TableDataInfo list(PxLeaveMessage pxLeaveMessage) {
        startPage();
        List<PxLeaveMessage> list = pxMessageService.selectPxLeaveMessageList(pxLeaveMessage);
        return getDataTable(list);
    }

    /**
     * 查询留言审核
     */
    @GetMapping("/getMessageExamine")
    public TableDataInfo getMessageExamine(PxLeaveMessage pxLeaveMessage) {
        startPage();
        List<PxLeaveMessage> list = pxMessageService.selectPxLeaveMessageExamine(pxLeaveMessage);
        return getDataTable(list);
    }

    /**
     * 修改留言
     */
    @Log(title = "留言", businessType = BusinessType.UPDATE)
    @PutMapping("updateMessage")
    public AjaxResult edit(@RequestBody PxLeaveMessage pxLeaveMessage) {
        return toAjax(pxMessageService.updatePxLeaveMessage(pxLeaveMessage));
    }
}
