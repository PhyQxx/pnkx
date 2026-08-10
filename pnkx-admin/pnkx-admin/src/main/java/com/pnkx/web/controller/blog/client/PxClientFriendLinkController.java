package com.pnkx.web.controller.blog.client;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.template.TemplateUtils;
import com.pnkx.domain.po.PxFriendLink;
import com.pnkx.service.IPxFriendLinkService;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.service.ISysConfigService;
import com.pnkx.system.service.ISysEmailService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * @author by PHY
 * @Classname 友链Controller
 * @date 2021-04-30 11:42
 */
@RestController
@RequestMapping("/client/link")
public class PxClientFriendLinkController extends BaseController {
    @Resource
    private IPxFriendLinkService pxFriendLinkService;
    @Resource
    private ISysEmailService sysEmailService;
    @Resource
    private ISysConfigService configService;

    /**
     * 获取友情链接
     *
     * @return
     */
    @GetMapping("/list")
    public AjaxResult getFriendLink(PxFriendLink pxFriendLink) {
        return AjaxResult.success(pxFriendLinkService.selectPxFriendLinkList(pxFriendLink));
    }

    /**
     * 新增友链
     */
    @Log(title = "友链", businessType = BusinessType.INSERT)
    @PostMapping("/addLink")
    public AjaxResult add(@RequestBody PxFriendLink pxFriendLink) {
        SysEmail sysEmail = new SysEmail();
        String activationTemplate = TemplateUtils.getTemplate("link");
        activationTemplate = activationTemplate.replace("template-lineTheme", pxFriendLink.getTitle() + "的博主申请友链");
        activationTemplate = activationTemplate.replace("template-lineTitle", pxFriendLink.getTitle());
        activationTemplate = activationTemplate.replace("template-lineRemark", pxFriendLink.getRemark());
        activationTemplate = activationTemplate.replace("template-lineUrl", pxFriendLink.getUrl());
        activationTemplate = activationTemplate.replace("template-lineImg", pxFriendLink.getImg());
        activationTemplate = activationTemplate.replace("template-lineEmail", pxFriendLink.getEmail());
        String siteEmail = configService.selectConfigByKey("siteEmail");
        String[] emails = siteEmail.split(",");
        sysEmail.setReceiverEmail(emails[0]);
        sysEmail.setCcEmail(emails[1]);
        sysEmail.setSubject("\uD83D\uDC49 叮咚！「Pei你看雪博客」上有人申请友链");
        sysEmail.setContent(activationTemplate);
        try {
            sysEmailService.sendMail(sysEmail);
        } catch (Exception e) {
            throw new ServiceException("发送邮件异常");
        }
        return toAjax(pxFriendLinkService.insertPxFriendLink(pxFriendLink));
    }
}
