package com.pnkx.web.controller.blog.admin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.ai.AiClient;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.service.search.SearchService;
import org.springframework.web.bind.annotation.RequestParam;
import com.pnkx.blog.domain.po.*;
import com.pnkx.blog.domain.vo.PxArticleVo;
import com.pnkx.life.service.*;import com.pnkx.blog.service.*;import com.pnkx.system.domain.SysNotice;
import com.pnkx.system.domain.SysNoticeRead;
import com.pnkx.system.service.ISysConfigService;
import com.pnkx.system.service.ISysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Stream;

/**
 * 管理端controller
 *
 * @author phy
 * @date 2021-10-30
 */
@RestController
@RequestMapping("/admin")
public class PxAdminController extends BaseController {

    @Resource
    private IPxArticleService pxArticleService;
    @Resource
    private IPxToDoService pxToDoService;
    @Resource
    private IPxBookkeepingRecordService pxBookkeepingRecordService;
    @Resource
    private IPxDiaryService pxDiaryService;
    @Resource
    private IPxNoteFolderService pxNoteFolderService;
    @Resource
    private IPxMessageService pxMessageService;
    @Resource
    private IPxFriendLinkService pxFriendLinkService;
    @Resource
    private ISysNoticeService noticeService;
    @Resource
    private AiLifeReminderDataService reminderDataService;
    @Resource
    private ISysConfigService configService;
    @Resource
    private IPxNoteService noteService;
    @Resource
    private IPxCommemorationDayService commemorationDayService;
    @Resource
    private IPxSubscriptionService subscriptionService;
    @Resource
    private IPxRecipeService recipeService;
    @Resource
    private IPxShoppingListService shoppingListService;
    @Resource
    private IPxBookService bookService;
    @Resource
    private AiClient aiClient;
    @Resource
    private SearchService searchService;

    /**
     * 跨模块统一搜索。返回稳定的轻量结果结构，PC 与移动端共用。
     */
    @GetMapping("/globalSearch")
    public AjaxResult globalSearch(String q, String type, String startDate, String endDate, String tag) {
        if (q == null || q.isBlank()) return AjaxResult.success(Collections.emptyList());
        return AjaxResult.success(searchService.search(q, type, startDate, endDate, tag, 1, 100).rows());
    }

    @GetMapping("/globalSearch/page")
    public AjaxResult searchPage(String q, String type, String startDate, String endDate, String tag,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "20") int pageSize) {
        return AjaxResult.success(searchService.search(q, type, startDate, endDate, tag, pageNum, pageSize));
    }

    /** 将当前权限范围内的搜索结果交给 AI 做摘要或问答。 */
    @PostMapping("/globalSearch/summary")
    public AjaxResult summarizeSearch(@RequestBody Map<String, Object> body) {
        String q = Objects.toString(body.get("q"), "");
        String type = Objects.toString(body.get("type"), "all");
        String startDate = Objects.toString(body.get("startDate"), "");
        String endDate = Objects.toString(body.get("endDate"), "");
        String tag = Objects.toString(body.get("tag"), "");
        String question = Objects.toString(body.get("question"), "请概括这些结果，并给出下一步建议");
        AjaxResult search = globalSearch(q, type, startDate, endDate, tag);
        Object data = search.get("data");
        String context = JSON.toJSONString(data);
        if (context.length() > 12000) context = context.substring(0, 12000);
        JSONObject answer = aiClient.chat(
                "你是个人生活信息整理助手。只能依据给定搜索结果回答；不要推测缺失信息，不要泄露原始ID。",
                "用户问题：" + question + "\n搜索结果：" + context);
        return AjaxResult.success(answer == null ? "暂时无法生成摘要" : answer.getString("content"));
    }
    /**
     * 全文检索
     */
    @GetMapping("/fullRetrieval")
    public AjaxResult retrieval(String searchCode) {
        if (searchCode == null || searchCode.isBlank()) return AjaxResult.success(Collections.emptyList());
        Map<String, List<JSONObject>> grouped = new LinkedHashMap<>();
        Map<String, String> labels = Map.ofEntries(Map.entry("article", "博客文章"), Map.entry("todo", "待办事项"),
                Map.entry("diary", "日记"), Map.entry("note", "笔记"), Map.entry("bookkeeping", "生活账本"),
                Map.entry("commemoration", "纪念日"), Map.entry("subscription", "订阅"), Map.entry("recipe", "菜谱"),
                Map.entry("shopping", "购物清单"), Map.entry("file", "文件"), Map.entry("book", "书籍"), Map.entry("webArticle", "剪藏"));
        for (var row : searchService.search(searchCode, "all", null, null, null, 1, 100).rows()) {
            JSONObject item = new JSONObject();
            item.put("id", row.getId()); item.put("title", row.getTitle());
            item.put("content", row.getSubtitle() == null ? row.getTitle() : row.getSubtitle());
            item.put("type", row.getType()); item.put("route", row.getRoute()); item.put("appRoute", row.getAppRoute());
            grouped.computeIfAbsent(labels.get(row.getType()), key -> new ArrayList<>()).add(item);
        }
        return AjaxResult.success(grouped.entrySet().stream().map(entry ->
                Map.of("label", entry.getKey(), "options", entry.getValue())).toList());
    }

    /**
     * 获取所有代办
     */
    @GetMapping("/getAllToDo")
    public AjaxResult getAllToDo() {
        // 所有的待办
        JSONObject result = new JSONObject();

        // 待办事项TODO
        PxToDo pxToDo = new PxToDo();
        // 创建人
        pxToDo.setCreateBy(SecurityUtils.getUserId());
        // 未完成
        pxToDo.setStatus(false);
        // 结束时间
        pxToDo.setPlanEndTime(DateUtils.getTime());
        List<PxToDo> pxToDos = pxToDoService.selectPxToDoList(pxToDo);
        result.put("todo", pxToDos);

        // 提醒聚合（纪念日 / 情侣卡券 / 经期记录）统一走提醒中心服务，
        // 新增提醒类型只需扩展 AiLifeReminderDataService.buildAllReminders 一处
        JSONObject reminders = reminderDataService.buildAllReminders(SecurityUtils.getUserId());
        result.putAll(reminders);

        // 留言审核
        PxLeaveMessage pxLeaveMessage = new PxLeaveMessage();
        // 状态
        pxLeaveMessage.setState("0");
        List<PxLeaveMessage> leaveMessages = pxMessageService.selectPxLeaveMessageExamine(pxLeaveMessage);
        result.put("message", leaveMessages);

        // 友链审核
        PxFriendLink pxFriendLink = new PxFriendLink();
        // 状态
        pxFriendLink.setStatus("0");
        List<PxFriendLink> friendLinks = pxFriendLinkService.selectPxFriendLinkList(pxFriendLink);
        result.put("link", friendLinks);

        // 通知公告
        SysNoticeRead sysNoticeRead = new SysNoticeRead();
        sysNoticeRead.setCreateBy(SecurityUtils.getUserId());
        List<SysNotice> unreadNoticeList = noticeService.getUnreadNoticeList(sysNoticeRead);
        result.put("notice", unreadNoticeList);

        // 姨妈助手设置
        JSONObject menstruationAssistantSetting = new JSONObject();
        menstruationAssistantSetting.put("cycle", configService.selectConfigByKey("ymzq"));
        menstruationAssistantSetting.put("duration", configService.selectConfigByKey("ymsc"));
        menstruationAssistantSetting.put("state", configService.selectConfigByKey("ymdqzt"));
        result.put("menstruationAssistantSetting", menstruationAssistantSetting);
        return AjaxResult.success(result);
    }
}
