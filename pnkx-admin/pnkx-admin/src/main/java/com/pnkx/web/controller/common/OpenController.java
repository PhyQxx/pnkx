package com.pnkx.web.controller.common;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.enums.OperatorType;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.domain.po.*;
import com.pnkx.domain.vo.PxCardRecordVo;
import com.pnkx.service.*;
import com.pnkx.system.domain.SysAppVersion;
import com.pnkx.system.service.ISysAppVersionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 开放式接口Controller
 *
 * @author phy
 * @date 2021-10-30
 */
@RestController
@RequestMapping("/open")
public class OpenController extends BaseController {
    @Resource
    private IPxArticleService pxArticleService;
    @Resource
    private IPxBookKeepingAccountService pxBookkeepingAccountService;
    @Resource
    private IPxBookkeepingClassificationService pxBookkeepingClassificationService;
    @Resource
    private IPxBookkeepingRecordService pxBookkeepingRecordService;
    @Resource
    private IPxLoversCardService pxLoversCardService;
    @Resource
    private IPxCommemorationDayService pxCommemorationDayService;
    @Resource
    private IPxDiaryService pxDiaryService;
    @Resource
    private IPxFriendLinkService pxFriendLinkService;
    @Resource
    private IPxMessageService pxMessageService;
    @Resource
    private IPxMenstruationRecordService pxMenstruationRecordService;
    @Resource
    private IPxNoteService pxNoteService;
    @Resource
    private IPxToDoService pxToDoService;
    @Resource
    private IPxVisitsService pxVisitsService;
    @Resource
    ISysAppVersionService sysAppVersionService;

    /**
     * 获取ip
     */
    @Operation(summary = "获取IP")
    @Log(title = "获取IP", operatorType = OperatorType.OTHER)
    @GetMapping("/getIp")
    public AjaxResult getIp(HttpServletRequest request) {
        return AjaxResult.success("获取IP成功！", IpUtils.getIpAddr(request));
    }

    /**
     * 检查App更新（基于sys_app_version表）
     *
     * @param platform    平台 (android/ios)
     * @param versionCode 当前版本编号
     * @return 更新信息，无需更新时返回null
     */
    @Operation(summary = "检查App更新")
    @Log(title = "检查App更新", operatorType = OperatorType.OTHER)
    @GetMapping("/checkUpdate")
    public AjaxResult checkUpdate(@RequestParam(defaultValue = "android") String platform,
                                  @RequestParam Integer versionCode) {
        SysAppVersion latest = sysAppVersionService.checkUpdate(platform);
        if (latest == null || latest.getVersionCode() <= versionCode) {
            return AjaxResult.success();
        }
        return AjaxResult.success(latest);
    }

    @Operation(summary = "获取指定业务数据")
    @Log(title = "获取指定业务数据", operatorType = OperatorType.MANAGE)
    @GetMapping("/getSpecifyBusinessData/{businessType}")
    public AjaxResult getSpecifyBusinessData(@PathVariable("businessType")String businessType) {
        if (businessType == null) {
            return AjaxResult.error("业务类型不能为空！");
        }
        // 业务数据
        List list = new ArrayList<>();
        switch (businessType) {
            case "article":
                // 文章
                list = pxArticleService.selectPxArticleList(new PxArticle());
                break;
            case "friend_link":
                // 友链
                list = pxFriendLinkService.selectPxFriendLinkList(new PxFriendLink());
                break;
            case "leave_message":
                // 留言
                list = pxMessageService.selectPxLeaveMessageList(new PxLeaveMessage());
                break;
            case "visits":
                // 访客
                list = pxVisitsService.selectPxVisitsList(new PxVisits());
                break;
            case "bookkeeping_account":
                // 账本用户
                list = pxBookkeepingAccountService.selectPxBookkeepingAccountList(new PxBookkeepingAccount());
                break;
            case "bookkeeping_classification":
                // 账本分类
                list = pxBookkeepingClassificationService.selectPxBookkeepingClassificationList(new PxBookkeepingClassification());
                break;
            case "bookkeeping_record":
                // 账本记录
                list = pxBookkeepingRecordService.selectPxBookkeepingRecordAll(new PxBookkeepingRecord());
                break;
            case "love_card":
                // 情侣卡券
                list = pxLoversCardService.selectPxLoversCardList(new PxLoversCard());
                break;
            case "card_record":
                // 情侣卡券使用记录
                list = pxLoversCardService.selectPxLoversCardRecordList(new PxCardRecordVo());
                break;
            case "commemoration_day":
                // 纪念日
                list = pxCommemorationDayService.selectPxCommemorationDayList(new PxCommemorationDay());
                break;
            case "diary":
                // 日记
                list = pxDiaryService.selectPxDiaryList(new PxDiary());
                break;
            case "menstruation_record":
                // 姨妈助手记录
                list = pxMenstruationRecordService.selectMenstruationRecordList(new PxMenstruationRecord());
                break;
            case "note":
                // 笔记
                list = pxNoteService.selectPxNoteList(new PxNote());
                break;
            case "to_do":
                // 待办事项
                list = pxToDoService.selectPxToDoList(new PxToDo());
                break;
        }
        return AjaxResult.success("获取指定业务数据成功！", list);
    }
}
