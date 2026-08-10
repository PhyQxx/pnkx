package com.pnkx.web.controller.system;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.service.ISysEmailService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author by PHY
 * @Classname EmailController
 * @date 2021-05-13 11:08
 */
@RestController
@RequestMapping("/system/email")
public class SysEmailController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(SysEmailController.class);

    @Resource
    private ISysEmailService sysEmailService;

    /**
     * 查询邮件记录列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysEmail pxEmail) {
        startPage();
        List<SysEmail> list = sysEmailService.selectSysEmailList(pxEmail);
        return getDataTable(list);
    }

    /**
     * 导出邮件记录列表
     */
    @Log(title = "邮件记录", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(SysEmail pxEmail) {
        List<SysEmail> list = sysEmailService.selectSysEmailList(pxEmail);
        ExcelUtil<SysEmail> util = new ExcelUtil<>(SysEmail.class);
        return util.exportExcel(list, "email");
    }

    /**
     * 获取邮件记录详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(sysEmailService.selectSysEmailById(id));
    }

    /**
     * 修改邮件记录
     */
    @Log(title = "邮件记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysEmail pxEmail) {
        return toAjax(sysEmailService.updateSysEmail(pxEmail));
    }

    /**
     * 删除邮件记录
     */
    @Log(title = "邮件记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(sysEmailService.deleteSysEmailByIds(ids));
    }

    @Operation(summary = "发送HTML邮件")
    @PostMapping("/sendHtmlEmail")
    @Transactional()
    public AjaxResult sendHtmlEmail(@RequestBody SysEmail email) {
        try {
            sysEmailService.sendMail(email);
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            return AjaxResult.success("发送失败", false);
        }
        return AjaxResult.success("发送成功", true);
    }
}
