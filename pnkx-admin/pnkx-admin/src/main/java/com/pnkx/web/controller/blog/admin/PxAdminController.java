package com.pnkx.web.controller.blog.admin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.ai.AiClient;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.*;
import com.pnkx.domain.vo.PxArticleVo;
import com.pnkx.service.*;
import com.pnkx.system.domain.SysNotice;
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
import com.pnkx.mapper.PxNoteMapper;

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
    private PxNoteMapper noteMapper;
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

    /**
     * 跨模块统一搜索。返回稳定的轻量结果结构，PC 与移动端共用。
     */
    @GetMapping("/globalSearch")
    public AjaxResult globalSearch(String q, String type, String startDate, String endDate, String tag) {
        String keyword = q == null ? "" : q.trim();
        if (keyword.isEmpty()) return AjaxResult.success(Collections.emptyList());
        String userId = SecurityUtils.getUserId();
        List<JSONObject> results = new ArrayList<>();

        if (matchesType(type, "article")) {
            PxArticleVo query = new PxArticleVo();
            query.setSearchValue(keyword);
            query.setCreateBy(userId);
            pxArticleService.selectPxArticleNotContent(query).stream().limit(10).forEach(item ->
                    addSearchResult(results, "article", item.getId(), item.getTitle(), "博客文章",
                            "/blog/article", "/pages_blog/article/detail?id=" + item.getId(), item, startDate, endDate, tag));
        }
        if (matchesType(type, "todo")) {
            PxToDo query = new PxToDo();
            query.setSearchValue(keyword);
            pxToDoService.selectPxToDoList(query).stream().limit(10).forEach(item ->
                    addSearchResult(results, "todo", item.getId(), item.getContent(), item.getLabel(),
                            "/mytool/todo", "/pages_life/todo/edit?id=" + item.getId(), item, startDate, endDate, tag));
        }
        if (matchesType(type, "diary")) {
            pxDiaryService.retrieval(keyword).stream().limit(10).forEach(item ->
                    addSearchResult(results, "diary", item.getId(), item.getTitle(), "日记",
                            "/mytool/diary", "/pages_life/diary/edit?id=" + item.getId(), item, startDate, endDate, tag));
        }
        if (matchesType(type, "note")) {
            noteMapper.searchAiNotes(userId, keyword, null, 10).forEach(item ->
                    addSearchResult(results, "note", item.getId(), item.getTitle(), "笔记",
                            "/note", "/pages_life/note/detail?id=" + item.getId(), item, startDate, endDate, tag));
        }
        if (matchesType(type, "bookkeeping")) {
            PxBookkeepingRecord query = new PxBookkeepingRecord();
            query.setSearchValue(keyword);
            pxBookkeepingRecordService.selectPxBookkeepingRecordAll(query).stream().limit(10).forEach(item ->
                    addSearchResult(results, "bookkeeping", item.getId(), "¥" + item.getMoney(), item.getRemark(),
                            "/mytool/bookkeeping/record", "/pages_life/bookkeeping/record/index", item, startDate, endDate, tag));
        }
        if (matchesType(type, "commemoration")) {
            PxCommemorationDay query = new PxCommemorationDay();
            query.setName(keyword);
            commemorationDayService.selectPxCommemorationDayList(query).stream().limit(10).forEach(item ->
                    addSearchResult(results, "commemoration", item.getId(), item.getName(), "纪念日",
                            "/commemorationDay", "/pages_life/commemorationDay/add?id=" + item.getId(), item, startDate, endDate, tag));
        }
        if (matchesType(type, "subscription")) {
            PxSubscription query = new PxSubscription();
            query.setName(keyword);
            subscriptionService.selectPxSubscriptionList(query).stream().limit(10).forEach(item ->
                    addSearchResult(results, "subscription", item.getId(), item.getName(), "订阅 ¥" + item.getAmount(),
                            "/mytool/subscription", "/pages_life/subscription/index?highlight=" + item.getId(), item, startDate, endDate, tag));
        }
        if (matchesType(type, "recipe")) {
            PxRecipe query = new PxRecipe();
            query.setTitle(keyword);
            recipeService.selectPxRecipeList(query).stream().limit(10).forEach(item ->
                    addSearchResult(results, "recipe", item.getId(), item.getTitle(), "菜谱",
                            "/mytool/recipe", "/pages_life/recipe/detail?id=" + item.getId(), item, startDate, endDate, tag));
        }
        if (matchesType(type, "shopping")) {
            PxShoppingList query = new PxShoppingList();
            query.setName(keyword);
            shoppingListService.selectPxShoppingListList(query).stream().limit(10).forEach(item ->
                    addSearchResult(results, "shopping", item.getId(), item.getName(), "购物清单",
                            "/mytool/shoppingList", "/pages_life/shoppingList/detail?id=" + item.getId(), item, startDate, endDate, tag));
        }
        if (matchesType(type, "book")) {
            PxBook query = new PxBook();
            query.setTitle(keyword);
            query.setCreateBy(userId);
            bookService.selectBookList(query).stream().limit(10).forEach(item ->
                    addSearchResult(results, "book", item.getId(), item.getTitle(), item.getAuthor(),
                            "/myapp/book", "/pages_life/book/detail?id=" + item.getId(), item, startDate, endDate, tag));
        }
        return AjaxResult.success(results);
    }

    private boolean matchesType(String requested, String actual) {
        return requested == null || requested.isBlank() || "all".equals(requested) || actual.equals(requested);
    }

    private void addSearchResult(List<JSONObject> results, String type, Object id, String title,
                                 String subtitle, String route, String appRoute, Object source,
                                 String startDate, String endDate, String tag) {
        JSONObject sourceJson = (JSONObject) JSONObject.toJSON(source);
        String createdAt = firstNotBlank(sourceJson.getString("createTime"), sourceJson.getString("date"),
                sourceJson.getString("payTime"), sourceJson.getString("planEndTime"));
        String tags = firstNotBlank(sourceJson.getString("label"), sourceJson.getString("tags"),
                sourceJson.getString("typeName"));
        if (!matchesDate(createdAt, startDate, endDate) || !matchesTag(tags, tag)) return;
        JSONObject item = new JSONObject();
        item.put("type", type);
        item.put("id", id);
        item.put("title", title);
        item.put("subtitle", subtitle);
        item.put("route", route);
        item.put("appRoute", appRoute);
        item.put("createdAt", createdAt);
        item.put("tags", tags == null ? Collections.emptyList() : Arrays.asList(tags.split(",")));
        results.add(item);
    }

    private boolean matchesDate(String value, String startDate, String endDate) {
        if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) return true;
        if (value == null || value.length() < 10) return false;
        String date = value.substring(0, 10);
        return (startDate == null || startDate.isBlank() || date.compareTo(startDate) >= 0)
                && (endDate == null || endDate.isBlank() || date.compareTo(endDate) <= 0);
    }

    private boolean matchesTag(String tags, String requested) {
        return requested == null || requested.isBlank()
                || (tags != null && tags.toLowerCase(Locale.ROOT).contains(requested.trim().toLowerCase(Locale.ROOT)));
    }

    private String firstNotBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
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
        logger.info("全文检索-检索条件为：{}", searchCode);
        List<Map<String, Object>> result = new ArrayList<>();
        // 文章列表
        PxArticleVo pxArticle = new PxArticleVo();
        pxArticle.setSearchValue(searchCode);
        pxArticle.setCreateBy(SecurityUtils.getUserId());
        List<PxArticleVo> pxArticles = pxArticleService.selectPxArticleNotContent(pxArticle);
        Map<String, Object> article = new HashMap<>();
        article.put("label", "博客文章");
        article.put("options", pxArticles);
        result.add(article);
        // 待办事项
        PxToDo pxToDo = new PxToDo();
        pxToDo.setSearchValue(searchCode);
        List<PxToDo> pxToDos = pxToDoService.selectPxToDoList(pxToDo);
        Map<String, Object> todo = new HashMap<>();
        todo.put("label", "待办事项");
        todo.put("options", pxToDos);
        result.add(todo);
        // 生活账本
        PxBookkeepingRecord pxBookkeepingRecord = new PxBookkeepingRecord();
        pxBookkeepingRecord.setSearchValue(searchCode);
        List<?> pxBookkeepingRecords = pxBookkeepingRecordService.selectPxBookkeepingRecordAll(pxBookkeepingRecord);
        Map<String, Object> bookkeeping = new HashMap<>();
        bookkeeping.put("label", "生活账本");
        bookkeeping.put("options", pxBookkeepingRecords);
        result.add(bookkeeping);
        // 日记
        List<PxDiary> diaryList = pxDiaryService.retrieval(searchCode);
        Map<String, Object> diary = new HashMap<>();
        diary.put("label", "日记");
        diary.put("options", diaryList);
        result.add(diary);
        // 笔记
        PxNoteFolder pxNoteFolder = new PxNoteFolder();
        pxNoteFolder.setName(searchCode);
        List<PxNoteFolder> noteList = pxNoteFolderService.selectPxNoteFolderList(pxNoteFolder);
        Map<String, Object> note = new HashMap<>();
        note.put("label", "笔记");
        note.put("options", noteList);
        result.add(note);
        PxCommemorationDay dayQuery = new PxCommemorationDay();
        dayQuery.setName(searchCode);
        result.add(searchGroup("纪念日", commemorationDayService.selectPxCommemorationDayList(dayQuery)));
        PxSubscription subscriptionQuery = new PxSubscription();
        subscriptionQuery.setName(searchCode);
        result.add(searchGroup("订阅", subscriptionService.selectPxSubscriptionList(subscriptionQuery)));
        PxRecipe recipeQuery = new PxRecipe();
        recipeQuery.setTitle(searchCode);
        result.add(searchGroup("菜谱", recipeService.selectPxRecipeList(recipeQuery)));
        PxShoppingList shoppingQuery = new PxShoppingList();
        shoppingQuery.setName(searchCode);
        result.add(searchGroup("购物清单", shoppingListService.selectPxShoppingListList(shoppingQuery)));
        PxBook bookQuery = new PxBook();
        bookQuery.setTitle(searchCode);
        bookQuery.setCreateBy(SecurityUtils.getUserId());
        result.add(searchGroup("书籍", bookService.selectBookList(bookQuery)));
        return AjaxResult.success(result);
    }

    private Map<String, Object> searchGroup(String label, List<?> options) {
        Map<String, Object> group = new HashMap<>();
        group.put("label", label);
        group.put("options", options);
        return group;
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
