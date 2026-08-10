package com.pnkx.web.controller.chat;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxChatMessage;
import com.pnkx.service.IPxChatMessageService;
import com.pnkx.service.IPxChatRecordService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/chat/message")
public class PxChatRecordController extends BaseController {

    @Resource
    private IPxChatRecordService pxChatMessageService;

    /**
     * 查询聊天记录列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxChatMessage pxChatMessage) {
        startPage();
        List<PxChatMessage> list = pxChatMessageService.selectPxChatMessageList(pxChatMessage);
        return getDataTable(list);
    }

    /**
     * 导出聊天记录列表
     */
    @Log(title = "聊天记录", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxChatMessage pxChatMessage) {
        List<PxChatMessage> list = pxChatMessageService.selectPxChatMessageList(pxChatMessage);
        ExcelUtil<PxChatMessage> util = new ExcelUtil<PxChatMessage>(PxChatMessage. class);
        return util.exportExcel(list, "message");
    }

    /**
     * 获取聊天记录详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxChatMessageService.selectPxChatMessageById(id));
    }

    /**
     * 新增聊天记录
     */
    @Log(title = "聊天记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxChatMessage pxChatMessage) {
        return toAjax(pxChatMessageService.insertPxChatMessage(pxChatMessage));
    }

    /**
     * 修改聊天记录
     */
    @Log(title = "聊天记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxChatMessage pxChatMessage) {
        return toAjax(pxChatMessageService.updatePxChatMessage(pxChatMessage));
    }

    /**
     * 删除聊天记录
     */
    @Log(title = "聊天记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxChatMessageService.deletePxChatMessageByIds(ids));
    }
}
