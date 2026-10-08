package com.pnkx.web.controller.system;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.framework.sso.SysSsoClientService;
import com.pnkx.framework.sso.SysSsoClientService.SysSsoClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * SSO 应用管理（仅超管）
 *
 * @author phy
 */
@RestController
@RequestMapping("/system/sso")
public class SysSsoClientController extends BaseController {

    @Resource
    private SysSsoClientService ssoClientService;

    /**
     * 应用列表
     */
    @GetMapping("/list")
    public AjaxResult list() {
        assertAdmin();
        List<SysSsoClient> clients = ssoClientService.listClients();
        return AjaxResult.success(clients);
    }

    /**
     * 新增应用，返回一次性明文密钥（公共客户端无密钥）
     */
    @Log(title = "SSO应用管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysSsoClient client) {
        assertAdmin();
        String rawSecret = ssoClientService.addClient(client);
        if (rawSecret != null) {
            return AjaxResult.success("密钥仅在本次返回，请立即保存", rawSecret);
        }
        return AjaxResult.success();
    }

    /**
     * 修改应用；传入新密钥则轮换并返回明文
     */
    @Log(title = "SSO应用管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysSsoClient client) {
        assertAdmin();
        String rotatedSecret = ssoClientService.updateClient(client);
        if (rotatedSecret != null) {
            return AjaxResult.success("新密钥仅在本次返回，请立即保存", rotatedSecret);
        }
        return AjaxResult.success();
    }

    /**
     * 删除应用
     */
    @Log(title = "SSO应用管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable String id) {
        assertAdmin();
        ssoClientService.deleteClient(id);
        return AjaxResult.success();
    }

    /**
     * 应用管理可签发登录凭证，仅允许超管操作
     */
    private void assertAdmin() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || loginUser.getUser() == null
                || !SecurityUtils.isAdmin(loginUser.getUser().getUserId())) {
            throw new ServiceException("仅超级管理员可管理 SSO 应用");
        }
    }
}
