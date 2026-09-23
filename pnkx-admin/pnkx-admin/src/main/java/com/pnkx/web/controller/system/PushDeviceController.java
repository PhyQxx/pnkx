package com.pnkx.web.controller.system;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.system.service.impl.UniPushService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * App 推送设备登记（客户端启动时上报 uniPush clientId）
 *
 * @author PHY
 * @date 2026-09-23
 */
@RestController
@RequestMapping("/system/push")
public class PushDeviceController extends BaseController {

    @Resource
    private UniPushService uniPushService;

    /**
     * 登记当前登录用户的推送设备
     * body: { clientId, platform, appVersion }
     */
    @PostMapping("/device")
    public AjaxResult register(@RequestBody Map<String, String> body) {
        String clientId = body.get("clientId");
        if (clientId == null || clientId.isBlank()) {
            return AjaxResult.error("clientId 不能为空");
        }
        uniPushService.registerDevice(SecurityUtils.getUserId(), clientId,
                body.getOrDefault("platform", ""), body.getOrDefault("appVersion", ""));
        return AjaxResult.success();
    }
}
