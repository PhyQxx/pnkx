package com.pnkx.web.controller.blog.client;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.domain.po.PxLeaveMessage;
import com.pnkx.service.IPxMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 留言Controller
 *
 * @author phy
 * @date 2021-01-26
 */
@RestController
@RequestMapping("/client/message")
public class PxClientMessageController extends BaseController {
    @Resource
    private IPxMessageService pxAdminMessageService;

    /**
     * 查询留言列表
     */
    @GetMapping("/getMessageList")
    public TableDataInfo list(PxLeaveMessage pxLeaveMessage) {
        startPage();
        List<PxLeaveMessage> list = pxAdminMessageService.selectPxLeaveMessageList(pxLeaveMessage);
        return getDataTable(list);
    }
}
