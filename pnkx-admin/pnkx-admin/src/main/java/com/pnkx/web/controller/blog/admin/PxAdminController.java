package com.pnkx.web.controller.blog.admin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.controller.BaseController;
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
        return AjaxResult.success(result);
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
