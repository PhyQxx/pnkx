package com.pnkx.web.controller.blog.client;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.entity.SysDictData;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.domain.po.PxRegion;
import com.pnkx.domain.po.PxEmailSubscribe;
import com.pnkx.service.IPxRegionService;
import com.pnkx.service.IPxStatisticsService;
import com.pnkx.service.IPxEmailSubscribeService;
import com.pnkx.system.domain.SysConfig;
import com.pnkx.system.domain.SysFile;
import com.pnkx.system.domain.SysNotice;
import com.pnkx.system.domain.vo.SysNoticeVo;
import com.pnkx.system.service.*;
import com.pnkx.web.controller.tool.intent.ChatHandler;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author by PHY
 * @Classname PxCustomerController
 * @date 2021-03-24 11:44
 */
@RequestMapping("/client")
@RestController
public class PxClientController extends BaseController {

    @Resource
    private IPxEmailSubscribeService pxEmailSubscribeService;
    @Resource
    private IPxStatisticsService pxStatisticsService;
    @Resource
    private ISysConfigService sysConfigService;
    @Resource
    private ISysDictDataService sysDictDataService;
    @Resource
    private ISysFileService sysFileService;
    @Resource
    private ISysDictTypeService dictTypeService;
    @Resource
    private ISysNoticeService noticeService;
    @Resource
    private IPxRegionService pxRegionService;
    @Resource
    private ChatHandler chatHandler;

    /**
     * 游客 AI 对话。该入口只调用普通问答处理器，不执行任何写库意图。
     */
    @PostMapping(value = "/ai/chat/stream", produces = "text/event-stream")
    public void guestAiChat(@RequestBody Map<String, Object> body, HttpServletResponse response) throws IOException {
        String question = body == null ? null : String.valueOf(body.getOrDefault("question", "")).trim();
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        OutputStream out = response.getOutputStream();
        if (StringUtils.isEmpty(question)) {
            com.pnkx.web.controller.tool.intent.IntentHandler.writeSse(out, "请输入问题。");
            com.pnkx.web.controller.tool.intent.IntentHandler.writeSse(out, "[DONE]");
            return;
        }

        com.alibaba.fastjson.JSONObject context = new com.alibaba.fastjson.JSONObject();
        context.put("systemPrompt", buildGuestAiPrompt(body));
        chatHandler.handle(question, context, out);
    }

    private String buildGuestAiPrompt(Map<String, Object> body) {
        StringBuilder prompt = new StringBuilder("你是 Pei你看雪博客的访客 AI 助手。回答一般知识、博客阅读和生活建议类问题；"
                + "游客模式下不得执行记账、日记、待办、笔记等写入操作。请用简洁友好的中文回复。\n");
        Object messages = body.get("messages");
        if (messages instanceof List<?> list) {
            for (Object item : list.stream().skip(Math.max(0, list.size() - 6)).toList()) {
                if (item instanceof Map<?, ?> message) {
                    Object role = message.get("role");
                    Object content = message.get("content");
                    if (content != null) {
                        String text = String.valueOf(content);
                        prompt.append("user".equals(role) ? "用户：" : "助手：")
                                .append(text, 0, Math.min(text.length(), 500)).append('\n');
                    }
                }
            }
        }
        return prompt.toString();
    }

    /**
     * 获取ip
     *
     * @param request request
     * @return ip
     */
    @GetMapping("/getIpLocation")
    public AjaxResult getIp(HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        return AjaxResult.success(IpUtils.getLocation(ip));
    }

    /**
     * 添加订阅
     *
     * @param pxEmailSubscribe pxEmailSubscribe
     * @return 订阅结果
     */
    @PostMapping("/addSubscribe")
    public AjaxResult addSubscribe(@RequestBody PxEmailSubscribe pxEmailSubscribe) {
        return AjaxResult.success(pxEmailSubscribeService.insertPxEmailSubscribe(pxEmailSubscribe));
    }

    /**
     * 获取随机图片
     * @param sysFile 参数
     * @return 随机图片
     */
    @GetMapping("/getGalleryPicture")
    public TableDataInfo list(SysFile sysFile, Boolean isRandom) {
        sysFile.setType("tk");
        if (isRandom) {
            List<SysFile> list = sysFileService.getRandomPicture(15);
            return getDataTable(list);
        }
        startPage();
        List<SysFile> list = sysFileService.selectSysFileList(sysFile);
        return getDataTable(list);
    }

    /**
     * 获取博客信息
     *
     * @return 博客信息
     */
    @GetMapping(value = "/getBlogInfo")
    public AjaxResult getBlogInfo() {
        Map<String, Object> result = pxStatisticsService.getStatistics();
        SysDictData sysDictData = new SysDictData();
        sysDictData.setDictType("px_article_type");
        result.put("articleTypeNumber", sysDictDataService.selectDictDataList(sysDictData).size());
        SysConfig sysConfig = new SysConfig();
        sysConfig.setConfigType("blog");
        result.put("blogConfig", sysConfigService.selectConfigList(sysConfig));
        return AjaxResult.success(result);
    }

    /**
     * 根据参数键名查询参数值（客户端免登录）
     *
     * @param configKey 参数键名
     * @return 参数值
     */
    @GetMapping(value = "/config/configKey/{configKey}")
    public AjaxResult getConfigKey(@PathVariable String configKey) {
        // 显式指定重载版本：selectConfigByKey 返回 String，若直接传给
        // success(Object) 会被 success(String msg) 匹配，导致配置值落到 msg 字段而非 data。
        // 这里用 success(String msg, Object data)，保证参数值进入 data 字段。
        return AjaxResult.success("操作成功", sysConfigService.selectConfigByKey(configKey));
    }

    /**
     * 文件浏览+1
     */
    @GetMapping(value = "/file/browse/{id}")
    public AjaxResult browse(@PathVariable("id") Long id) {
        return AjaxResult.success(sysFileService.browse(id));
    }

    /**
     * 文件点赞+1
     */
    @GetMapping(value = "/file/like/{id}")
    public AjaxResult like(@PathVariable("id") Long id) {
        return AjaxResult.success(sysFileService.like(id));
    }

    /**
     * 根据字典类型查询字典数据信息
     */
    @GetMapping(value = "/dictType/{dictType}")
    public AjaxResult dictType(@PathVariable String dictType) {
        List<SysDictData> data = dictTypeService.selectDictDataByType(dictType);
        if (StringUtils.isNull(data)) {
            data = new ArrayList<>();
        }
        return AjaxResult.success(data);
    }

    /**
     * 获取博客通知公告列表
     */
    @GetMapping("/getNoticeList")
    public AjaxResult getClientNoticeList() {
        SysNotice sysNotice = new SysNotice();
        // 博客通知类型
        sysNotice.setNoticeType("bktz");
        List<SysNoticeVo> list = noticeService.selectNoticeList(sysNotice);
        return AjaxResult.success(list);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @GetMapping(value = "/getNotice/{noticeId}")
    public AjaxResult getInfo(HttpServletRequest request, @PathVariable Long noticeId) {
        return AjaxResult.success(noticeService.selectNoticeById(request, noticeId));
    }

    /**
     * 获取地区信息列表
     * @param pxRegion 地区信息
     * @return 地区信息列表
     */
    @GetMapping(value = "/getRegionList")
    public AjaxResult getRegionList(PxRegion pxRegion) {
        return AjaxResult.success(pxRegionService.getRegionList(pxRegion));
    }
}
