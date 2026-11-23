package com.pnkx.web.controller.tool;

import com.pnkx.common.constant.RedisConstants;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.web.mapper.SysToolUseLogMapper;
import com.pnkx.web.service.tools.ToolboxRateLimiter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工具使用打点（工具箱）
 *
 * POST /tools/use/log/record  记录一次使用 —— permitAll 放行（免登录工具），IP 限流
 * GET  /tools/use/log/stats   使用统计 —— 默认需登录（后台管理用）
 *
 * @author PHY
 */
@RestController
@RequestMapping("/tools/use/log")
public class PnkxToolUseLogController extends BaseController {

    /** 单 IP 每小时记录上限（正常使用远达不到，仅防刷） */
    private static final int RATE_LIMIT = 120;

    /** 限流窗口（秒） */
    private static final int RATE_WINDOW_SECONDS = 3600;

    /** toolId 最大长度（与表字段一致） */
    private static final int TOOL_ID_MAX = 50;

    /** toolName 最大长度 */
    private static final int TOOL_NAME_MAX = 100;

    /** source 白名单 */
    private static final java.util.Set<String> SOURCE_WHITE =
            new java.util.HashSet<>(java.util.Arrays.asList("mp", "h5", "app"));

    @Resource
    private SysToolUseLogMapper toolUseLogMapper;

    @Resource
    private ToolboxRateLimiter rateLimiter;

    /**
     * 记录一次工具使用（匿名）
     *
     * @param body {toolId, toolName, source}
     */
    @PostMapping("/record")
    public AjaxResult record(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        if (!rateLimiter.tryAcquire(RedisConstants.TOOL_USE_LOG_RATE_LIMIT + ip, RATE_LIMIT, RATE_WINDOW_SECONDS)) {
            // 防刷超限直接返回成功（打点失败不应打扰用户）
            return AjaxResult.success();
        }
        try {
            String toolId = sanitize(asString(body, "toolId"), TOOL_ID_MAX);
            String toolName = sanitize(asString(body, "toolName"), TOOL_NAME_MAX);
            String source = sanitize(asString(body, "source"), 10).toLowerCase();
            if (toolId.isEmpty()) {
                return AjaxResult.error("toolId 不能为空");
            }
            if (!SOURCE_WHITE.contains(source)) {
                source = "mp";
            }
            toolUseLogMapper.insertUseLog(toolId, toolName, ip, source);
            return AjaxResult.success();
        } catch (Exception e) {
            // 打点失败静默，不影响前端
            logger.error("工具使用打点失败", e);
            return AjaxResult.success();
        }
    }

    /**
     * 使用统计（登录态，支持时间范围筛选）
     */
    @GetMapping("/stats")
    public AjaxResult stats(@RequestParam(required = false) String beginTime,
                            @RequestParam(required = false) String endTime) {
        return AjaxResult.success(toolUseLogMapper.selectUseStatsFiltered(beginTime, endTime));
    }

    /**
     * 管理端：使用明细（分页，支持工具/时间筛选）
     */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String toolId,
                              @RequestParam(required = false) String beginTime,
                              @RequestParam(required = false) String endTime) {
        startPage();
        return getDataTable(toolUseLogMapper.selectUseLogList(toolId, beginTime, endTime));
    }

    private String asString(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String sanitize(String value, int max) {
        String cleaned = value.replaceAll("[\\r\\n\"'`<>]", "");
        return cleaned.length() > max ? cleaned.substring(0, max) : cleaned;
    }
}
