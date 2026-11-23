package com.pnkx.web.controller.tool;

import com.pnkx.common.constant.RedisConstants;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.web.service.tools.PnkxExchangeRateService;
import com.pnkx.web.service.tools.ToolboxRateLimiter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 汇率查询（工具箱·免登录）
 *
 * GET /tools/exrate/list?base=CNY
 * permitAll 放行（登录/未登录均可），按 IP 固定窗口限流
 *
 * @author PHY
 */
@RestController
@RequestMapping("/tools/exrate")
public class PnkxExchangeRateController extends BaseController {

    /** 单 IP 每小时查询上限 */
    private static final int RATE_LIMIT = 60;

    /** 限流窗口（秒） */
    private static final int RATE_WINDOW_SECONDS = 3600;

    @Resource
    private PnkxExchangeRateService exchangeRateService;

    @Resource
    private ToolboxRateLimiter rateLimiter;

    /**
     * 查询以 base 为基准的牌价
     *
     * @param base 基准币种（默认 CNY）
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "CNY") String base, HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        if (!rateLimiter.tryAcquire(RedisConstants.TOOL_EXRATE_RATE_LIMIT + ip, RATE_LIMIT, RATE_WINDOW_SECONDS)) {
            return AjaxResult.error("查询太频繁啦，休息一下每小时再来 ~");
        }
        try {
            Map<String, Object> data = exchangeRateService.getRates(base);
            return AjaxResult.success(data);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error("汇率查询失败 base={}", base, e);
            return AjaxResult.error("汇率服务暂不可用，请稍后再试");
        }
    }
}
