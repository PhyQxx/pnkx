package com.pnkx.web.controller.blog.client;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ip.IpLocation;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.domain.po.PxVisits;
import com.pnkx.service.IPxVisitsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 访客Controller
 *
 * @author phy
 * @date 2021-10-30
 */
@RestController
@RequestMapping("/client/visits")
public class PxClientVisitsController extends BaseController {

    @Resource
    private IPxVisitsService pxVisitsService;

    /**
     * 新增访客
     */
    @Log(title = "访客", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        IpLocation location = IpUtils.getLocation(ip);
        PxVisits pxVisits = new PxVisits();
        pxVisits.setIp(ip);
        pxVisits.setCountry(location.getCountry());
        pxVisits.setProvince(location.getProvince());
        pxVisits.setCity(location.getCity());
        pxVisits.setIsp(location.getIsp());
        pxVisits.setLocation(IpUtils.getRectangle(ip));
        return toAjax(pxVisitsService.insertPxVisits(pxVisits));
    }
}
