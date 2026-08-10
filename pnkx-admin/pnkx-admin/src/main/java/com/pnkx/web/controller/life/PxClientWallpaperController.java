package com.pnkx.web.controller.life;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxLikeRecord;
import com.pnkx.domain.po.PxWallpaper;
import com.pnkx.domain.po.PxWallpaperDownloadRecord;
import com.pnkx.domain.po.PxWallpaperFolder;
import com.pnkx.domain.po.PxWallpaperShareRewardRecord;
import com.pnkx.mapper.PxLikeRecordMapper;
import com.pnkx.mapper.PxWallpaperDownloadRecordMapper;
import com.pnkx.mapper.PxWallpaperShareRewardRecordMapper;
import com.pnkx.service.IPxWallpaperFolderService;
import com.pnkx.service.IPxWallpaperService;
import com.pnkx.system.service.ISysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author PHY
 * @classname PxClientWallpaperController
 * @description 壁纸Controller（客户端免登录访问）
 */
@RestController
@RequestMapping("/client/wallpaper")
public class PxClientWallpaperController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PxClientWallpaperController.class);

    /**
     * 单次打包下载的壁纸数量上限，防止超大压缩包拖垮内存
     */
    private static final int ZIP_MAX_COUNT = 200;

    @Resource
    private IPxWallpaperService pxWallpaperService;

    @Resource
    private IPxWallpaperFolderService pxWallpaperFolderService;

    @Resource
    private PxLikeRecordMapper pxLikeRecordMapper;

    @Resource
    private PxWallpaperDownloadRecordMapper pxWallpaperDownloadRecordMapper;

    @Resource
    private PxWallpaperShareRewardRecordMapper pxWallpaperShareRewardRecordMapper;

    @Resource
    private ISysConfigService sysConfigService;

    /**
     * 每用户每日下载上限配置键
     */
    private static final String DAILY_LIMIT_CONFIG_KEY = "sys.wallpaper.download.daily.limit";
    private static final int DEFAULT_DAILY_LIMIT = 50;

    /**
     * 达到每日下载上限时的提醒文案配置键
     */
    private static final String DOWNLOAD_REMIND_CONFIG_KEY = "sys.wallpaper.download.remind.text";
    private static final String DEFAULT_DOWNLOAD_REMIND_TEXT = "今日下载次数已达上限，明天再来吧～";

    /**
     * 分享小程序每次奖励的下载次数配置键
     */
    private static final String SHARE_REWARD_CONFIG_KEY = "sys.wallpaper.download.share.reward";
    private static final int DEFAULT_SHARE_REWARD = 10;

    /**
     * 每日最多可分享获奖次数配置键（防刷，0 表示不限）
     */
    private static final String SHARE_DAILY_TIMES_CONFIG_KEY = "sys.wallpaper.download.share.daily.times";
    private static final int DEFAULT_SHARE_DAILY_TIMES = 3;

    /**
     * 壁纸点赞记录类型（与文章"0"、评论"1"区分）
     */
    private static final String WALLPAPER_LIKE_TYPE = "3";

    /**
     * 查询壁纸列表
     * 移动端不展示停用文件夹及其下属全部壁纸：排除所有停用文件夹子树下的壁纸
     */
    @GetMapping("/list")
    public TableDataInfo list(PxWallpaper pxWallpaper) {
        List<Long> disabledFolderIds = pxWallpaperFolderService.selectDisabledFolderSubtreeIds();
        if (!disabledFolderIds.isEmpty()) {
            pxWallpaper.setExcludeFolderIds(disabledFolderIds);
        }
        startPage();
        List<PxWallpaper> list = pxWallpaperService.selectPxWallpaperList(pxWallpaper);
        return getDataTable(list);
    }

    /**
     * 获取壁纸详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxWallpaperService.selectPxWallpaperById(id));
    }

    /**
     * 壁纸点赞（切换态：已赞则取消，未赞则点赞），需登录。
     * 复用 px_like_record（type=3）按用户精确记录，同时同步冗余 like_count 兼容旧客户端。
     *
     * @param id 壁纸ID
     */
    @GetMapping("/like/{id}")
    public AjaxResult like(@PathVariable("id") Long id) {
        String userId = SecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(401, "请先登录");
        }
        PxLikeRecord param = new PxLikeRecord();
        param.setItemId(id);
        param.setType(WALLPAPER_LIKE_TYPE);
        param.setCreateBy(userId);
        PxLikeRecord existed = pxLikeRecordMapper.selectLikeByUser(param);
        if (existed != null) {
            // 已赞 → 取消
            pxLikeRecordMapper.deleteLikeByUser(param);
            pxWallpaperService.updateLikeCount(id, -1);
            AjaxResult ajax = AjaxResult.success("已取消点赞");
            ajax.put("liked", false);
            return ajax;
        }
        // 未赞 → 点赞
        PxWallpaper wp = pxWallpaperService.selectPxWallpaperById(id);
        param.setCreateTime(DateUtils.getNowDate());
        if (wp != null) {
            param.setItemName(wp.getName());
            param.setItemThumbnail(wp.getThumbnail());
        }
        pxLikeRecordMapper.insertPxLikeRecord(param);
        pxWallpaperService.updateLikeCount(id, 1);
        AjaxResult ajax = AjaxResult.success("点赞成功");
        ajax.put("liked", true);
        return ajax;
    }

    /**
     * 检查当前用户是否已「收藏/点赞」指定壁纸（兼容旧版前端 collectCheck 调用）。
     * 本系统收藏与点赞统一使用 px_like_record（type=3），二者等价。
     * 未登录时直接返回 liked=false，不报错，避免旧客户端登录前报错。
     *
     * @param id 壁纸ID
     */
    @GetMapping("/collectCheck/{id}")
    public AjaxResult collectCheck(@PathVariable("id") Long id) {
        String userId = SecurityUtils.getUserId();
        boolean liked = false;
        if (userId != null) {
            PxLikeRecord param = new PxLikeRecord();
            param.setItemId(id);
            param.setType(WALLPAPER_LIKE_TYPE);
            param.setCreateBy(userId);
            liked = pxLikeRecordMapper.selectLikeByUser(param) != null;
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("liked", liked);
        // 同时返回 collected，兼容旧版前端按「收藏」语义读取该字段
        ajax.put("collected", liked);
        return ajax;
    }

    /**
     * 我的点赞记录（需登录，分页）
     */
    @GetMapping("/myLikes")
    public TableDataInfo myLikes() {
        String userId = SecurityUtils.getUserId();
        PxLikeRecord param = new PxLikeRecord();
        param.setCreateBy(userId);
        param.setType(WALLPAPER_LIKE_TYPE);
        startPage();
        List<PxLikeRecord> list = pxLikeRecordMapper.selectMyRecordList(param);
        return getDataTable(list);
    }

    /**
     * 我的下载记录（需登录，分页）
     */
    @GetMapping("/myDownloads")
    public TableDataInfo myDownloads() {
        String userId = SecurityUtils.getUserId();
        PxWallpaperDownloadRecord param = new PxWallpaperDownloadRecord();
        param.setCreateBy(userId);
        startPage();
        List<PxWallpaperDownloadRecord> list = pxWallpaperDownloadRecordMapper.selectMyDownloadList(param);
        return getDataTable(list);
    }

    /**
     * 查询当前用户今日下载状态（剩余次数、限额、分享奖励、提醒文案）。
     * <p>
     * 剩余次数 = 每日上限 + 今日分享奖励总和 − 今日真实下载次数。
     */
    @GetMapping("/downloadStatus")
    public AjaxResult downloadStatus() {
        int shareReward = getShareReward();
        String userId = SecurityUtils.getUserId();
        AjaxResult ajax = AjaxResult.success();
        fillDownloadStatus(ajax, userId, shareReward);
        ajax.put("remindText", getDownloadRemindText());
        return ajax;
    }

    /**
     * 分享小程序奖励下载次数（需登录）。
     * <p>
     * 用户在「我的」页点击「分享小程序」按钮（open-type=share）触发分享后，
     * 由前端在分享成功回调中调用本接口发放奖励。每次奖励次数由参数
     * sys.wallpaper.download.share.reward 控制，每日最多可获奖次数由
     * sys.wallpaper.download.share.daily.times 控制（防刷，0 表示不限）。
     *
     * @return 最新下载状态（含 limit/todayCount/todayReward/remaining/
     * reward/shareTimesUsed/shareTimesLimit）
     */
    @PostMapping("/shareReward")
    public AjaxResult shareReward() {
        String userId = SecurityUtils.getUserId();
        if (StringUtils.isEmpty(userId)) {
            return AjaxResult.error(401, "请先登录");
        }
        int reward = getShareReward();
        // 防刷：每日最多可分享获奖次数（0 表示不限）
        int shareTimesLimit = getShareDailyTimes();
        if (shareTimesLimit > 0) {
            int used = getTodayShareTimes(userId);
            if (used >= shareTimesLimit) {
                AjaxResult ajax = AjaxResult.error("今日分享奖励次数已用完，明天再来吧～");
                // 仍返回最新配额，便于前端刷新卡片
                fillDownloadStatus(ajax, userId, reward);
                return ajax;
            }
        }
        // 写入一条分享奖励记录
        try {
            PxWallpaperShareRewardRecord record = new PxWallpaperShareRewardRecord();
            record.setRewardCount(reward);
            record.setCreateBy(userId);
            record.setCreateTime(DateUtils.getNowDate());
            pxWallpaperShareRewardRecordMapper.insertShareReward(record);
        } catch (Exception e) {
            log.error("记录分享奖励失败, userId={}", userId, e);
            return AjaxResult.error("领取奖励失败，请稍后重试");
        }
        AjaxResult ajax = AjaxResult.success("分享成功，" + reward + " 次下载已到账");
        ajax.put("rewarded", reward);
        fillDownloadStatus(ajax, userId, reward);
        return ajax;
    }

    /**
     * 填充今日下载状态（供 shareReward 等接口统一返回最新配额）
     */
    private void fillDownloadStatus(AjaxResult ajax, String userId, int reward) {
        int limit = getDailyLimit();
        int todayCount = StringUtils.isEmpty(userId) ? 0
                : pxWallpaperDownloadRecordMapper.countTodayDownload(userId);
        int todayReward = getTodayRewardTotal(userId);
        int remaining = Math.max(0, limit + todayReward - todayCount);
        ajax.put("limit", limit);
        ajax.put("todayCount", todayCount);
        ajax.put("todayReward", todayReward);
        ajax.put("remaining", remaining);
        ajax.put("reward", reward);
        ajax.put("shareTimesUsed", getTodayShareTimes(userId));
        ajax.put("shareTimesLimit", getShareDailyTimes());
    }

    /**
     * 检查登录用户今日下载是否已达上限。达上限则写入 429 + 提醒文案并返回 false。
     * 未登录用户直接放行（游客不参与限额）。
     *
     * @param response 响应
     * @return true 可继续下载；false 已达上限，已回写错误响应
     */
    private boolean checkDownloadLimit(HttpServletResponse response) {
        String userId = SecurityUtils.getUserId();
        if (StringUtils.isEmpty(userId)) {
            // 游客不限额
            return true;
        }
        int limit = getDailyLimit();
        int todayCount = pxWallpaperDownloadRecordMapper.countTodayDownload(userId);
        int todayReward = getTodayRewardTotal(userId);
        if (todayCount >= limit + todayReward) {
            // 已达上限：拒绝并回写提醒文案，不输出任何下载字节
            response.setStatus(429);
            response.setContentType("application/json; charset=utf-8");
            try {
                response.getWriter().write(AjaxResult.error(429, getDownloadRemindText()).toString());
            } catch (Exception e) {
                log.warn("写入下载限额拒绝响应失败", e);
            }
            return false;
        }
        return true;
    }

    /**
     * 获取下载达上限提醒文案，未配置或为空时取默认文案
     */
    private String getDownloadRemindText() {
        String text = sysConfigService.selectConfigByKey(DOWNLOAD_REMIND_CONFIG_KEY);
        return StringUtils.isNotEmpty(text) ? text : DEFAULT_DOWNLOAD_REMIND_TEXT;
    }

    /**
     * 代理下载壁纸原图
     * <p>
     * 移动端 H5 直接下载跨域图片会受 CORS 限制，统一由后端代理转发。
     * 壁纸地址均为完整 http(s) 链接（FTP 图床也是 https 对外暴露），
     * 故一律走 HTTP 代理，无需 FTP 协议。
     *
     * @param id       壁纸 id
     * @param response 响应
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable("id") Long id, HttpServletResponse response) {
        PxWallpaper wallpaper = pxWallpaperService.selectPxWallpaperById(id);
        if (wallpaper == null || wallpaper.getUrl() == null || wallpaper.getUrl().trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // 登录用户每日下载上限拦截：达上限则拒绝，不输出任何下载字节
        if (!checkDownloadLimit(response)) {
            return;
        }
        String imageUrl = wallpaper.getUrl().trim();
        String downloadName = buildDownloadName(wallpaper, imageUrl);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"");

        try {
            streamFromHttp(imageUrl, response);
            // 下载成功后记录，并在响应头返回剩余次数
            recordDownload(wallpaper, "single");
            setRemainingHeader(response, 1);
        } catch (Exception e) {
            log.error("代理下载壁纸异常，地址: {}", imageUrl, e);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }

    /**
     * 批量打包下载壁纸（ZIP）
     * <p>
     * 支持两种模式（任选其一）：
     * <ul>
     *   <li>ids=1,2,3 —— 打包指定壁纸</li>
     *   <li>folderId=10 —— 打包该文件夹（含全部子文件夹）下的壁纸</li>
     * </ul>
     * 文件名重名时自动追加序号，ZIP 实时流式写入响应，不落盘。
     *
     * @param ids      壁纸 id 集合（逗号分隔），与 folderId 二选一
     * @param folderId 文件夹 id，与 ids 二选一
     * @param response 响应
     */
    @GetMapping("/download/zip")
    public void downloadZip(@RequestParam(value = "ids", required = false) String ids,
                            @RequestParam(value = "folderId", required = false) Long folderId,
                            HttpServletResponse response) {
        List<PxWallpaper> wallpapers;
        String zipName = "wallpapers";
        if (folderId != null) {
            wallpapers = pxWallpaperService.selectPxWallpaperByFolderSubtree(folderId);
            PxWallpaperFolder folder = pxWallpaperFolderService.selectPxWallpaperFolderById(folderId);
            if (folder != null && folder.getName() != null && !folder.getName().trim().isEmpty()) {
                zipName = folder.getName().trim();
            }
        } else if (ids != null && !ids.trim().isEmpty()) {
            Long[] idArr = parseIdList(ids);
            wallpapers = idArr.length == 0 ? Collections.emptyList()
                    : pxWallpaperService.selectPxWallpaperByIds(idArr);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (wallpapers.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (wallpapers.size() > ZIP_MAX_COUNT) {
            log.warn("打包下载壁纸数量 {} 超过上限 {}，已截断", wallpapers.size(), ZIP_MAX_COUNT);
            wallpapers = wallpapers.subList(0, ZIP_MAX_COUNT);
        }

        // 登录用户每日下载上限拦截：达上限则拒绝，不输出任何下载字节
        // 注：本次批量张数不参与判断——已超额的本次仍允许下载，仅阻止已达上限后的新请求
        if (!checkDownloadLimit(response)) {
            return;
        }

        response.setContentType("application/zip");
        // 文件名用 URL 编码以兼容中文
        String encodedName;
        try {
            encodedName = URLEncoder.encode(zipName, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            encodedName = "wallpapers";
        }
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedName + ".zip\"; filename*=UTF-8''" + encodedName + ".zip");

        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(response.getOutputStream());
            Set<String> usedNames = new HashSet<>();
            for (PxWallpaper wp : wallpapers) {
                if (wp.getUrl() == null || wp.getUrl().trim().isEmpty()) {
                    continue;
                }
                String entryName = buildEntryName(wp, usedNames);
                zip.putNextEntry(new ZipEntry(entryName));
                try {
                    copyHttpToStream(wp.getUrl().trim(), zip);
                } catch (Exception e) {
                    // 单张失败不影响整包，仅记录日志
                    log.warn("打包壁纸 {} 失败: {}", wp.getId(), e.getMessage());
                }
                zip.closeEntry();
                zip.flush();
            }
        } catch (Exception e) {
            log.error("打包下载壁纸异常", e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception ignored) {
                }
            }
        }
        // 批量记录下载，并在响应头返回剩余次数
        int actualCount = recordDownloadBatch(wallpapers, "zip");
        setRemainingHeader(response, actualCount);
    }

    /**
     * 下载完成后，在响应头写入剩余下载次数（供前端判断是否弹提醒）
     *
     * @param response    响应
     * @param thisCount   本次下载张数
     */
    private void setRemainingHeader(HttpServletResponse response, int thisCount) {
        try {
            String userId = SecurityUtils.getUserId();
            if (StringUtils.isEmpty(userId)) {
                return;
            }
            int limit = getDailyLimit();
            int todayCount = pxWallpaperDownloadRecordMapper.countTodayDownload(userId);
            int todayReward = getTodayRewardTotal(userId);
            int remaining = Math.max(0, limit + todayReward - todayCount);
            response.setHeader("X-Download-Limit", String.valueOf(limit));
            response.setHeader("X-Download-Remaining", String.valueOf(remaining));
        } catch (Exception e) {
            log.warn("写入下载剩余次数响应头失败", e);
        }
    }

    /**
     * 从系统参数获取每日下载上限，未配置或格式错误时取默认值 50
     */
    private int getDailyLimit() {
        String config = sysConfigService.selectConfigByKey(DAILY_LIMIT_CONFIG_KEY);
        if (StringUtils.isNotEmpty(config)) {
            try {
                return Integer.parseInt(config);
            } catch (NumberFormatException e) {
                log.warn("每日下载上限配置 {} 格式错误: {}", DAILY_LIMIT_CONFIG_KEY, config);
            }
        }
        return DEFAULT_DAILY_LIMIT;
    }

    /**
     * 从系统参数获取每次分享奖励的下载次数，未配置或格式错误时取默认值 10
     */
    private int getShareReward() {
        String config = sysConfigService.selectConfigByKey(SHARE_REWARD_CONFIG_KEY);
        if (StringUtils.isNotEmpty(config)) {
            try {
                return Integer.parseInt(config);
            } catch (NumberFormatException e) {
                log.warn("分享奖励次数配置 {} 格式错误: {}", SHARE_REWARD_CONFIG_KEY, config);
            }
        }
        return DEFAULT_SHARE_REWARD;
    }

    /**
     * 从系统参数获取每日最多可分享获奖次数（防刷，0 表示不限），
     * 未配置或格式错误时取默认值 3
     */
    private int getShareDailyTimes() {
        String config = sysConfigService.selectConfigByKey(SHARE_DAILY_TIMES_CONFIG_KEY);
        if (StringUtils.isNotEmpty(config)) {
            try {
                return Integer.parseInt(config);
            } catch (NumberFormatException e) {
                log.warn("每日分享次数上限配置 {} 格式错误: {}", SHARE_DAILY_TIMES_CONFIG_KEY, config);
            }
        }
        return DEFAULT_SHARE_DAILY_TIMES;
    }

    /**
     * 查询某用户今日已通过分享获得的下载次数总和，未登录或异常返回 0
     */
    private int getTodayRewardTotal(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return 0;
        }
        try {
            Map<String, Object> map = pxWallpaperShareRewardRecordMapper.getTodayShareReward(userId);
            if (map == null) {
                return 0;
            }
            Object total = map.get("total");
            if (total == null) {
                return 0;
            }
            return ((Number) total).intValue();
        } catch (Exception e) {
            log.warn("查询今日分享奖励总和失败, userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 查询某用户今日已分享获奖的次数，未登录或异常返回 0
     */
    private int getTodayShareTimes(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return 0;
        }
        try {
            Map<String, Object> map = pxWallpaperShareRewardRecordMapper.getTodayShareReward(userId);
            if (map == null) {
                return 0;
            }
            Object times = map.get("times");
            if (times == null) {
                return 0;
            }
            return ((Number) times).intValue();
        } catch (Exception e) {
            log.warn("查询今日分享次数失败, userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 记录单条下载（未登录则跳过，不阻塞下载流程）
     */
    private void recordDownload(PxWallpaper wallpaper, String downloadType) {
        try {
            String userId = SecurityUtils.getUserId();
            // 下载接口匿名可访问，未登录时 getUserId() 返回空串，跳过记录避免写入空 create_by
            if (StringUtils.isEmpty(userId) || wallpaper == null) {
                return;
            }
            PxWallpaperDownloadRecord record = new PxWallpaperDownloadRecord();
            record.setItemId(wallpaper.getId());
            record.setItemName(wallpaper.getName());
            record.setItemThumbnail(wallpaper.getThumbnail());
            record.setDownloadType(downloadType);
            record.setCreateBy(userId);
            record.setCreateTime(DateUtils.getNowDate());
            pxWallpaperDownloadRecordMapper.batchInsertDownloadRecord(Collections.singletonList(record));
        } catch (Exception e) {
            log.warn("记录下载历史失败，wallpaperId={}", wallpaper == null ? null : wallpaper.getId(), e);
        }
    }

    /**
     * 批量记录下载（未登录则跳过，不阻塞下载流程）。返回实际记录的张数。
     */
    private int recordDownloadBatch(List<PxWallpaper> wallpapers, String downloadType) {
        try {
            String userId = SecurityUtils.getUserId();
            // 下载接口匿名可访问，未登录时 getUserId() 返回空串，跳过记录避免写入空 create_by
            if (StringUtils.isEmpty(userId) || wallpapers == null || wallpapers.isEmpty()) {
                return 0;
            }
            java.util.Date now = DateUtils.getNowDate();
            List<PxWallpaperDownloadRecord> records = new java.util.ArrayList<>();
            for (PxWallpaper wp : wallpapers) {
                if (wp == null || wp.getUrl() == null || wp.getUrl().trim().isEmpty()) {
                    continue;
                }
                PxWallpaperDownloadRecord record = new PxWallpaperDownloadRecord();
                record.setItemId(wp.getId());
                record.setItemName(wp.getName());
                record.setItemThumbnail(wp.getThumbnail());
                record.setDownloadType(downloadType);
                record.setCreateBy(userId);
                record.setCreateTime(now);
                records.add(record);
            }
            if (!records.isEmpty()) {
                pxWallpaperDownloadRecordMapper.batchInsertDownloadRecord(records);
            }
            return records.size();
        } catch (Exception e) {
            log.warn("批量记录下载历史失败", e);
            return 0;
        }
    }

    /**
     * 通过 HTTP 代理读取图片并写入响应（单张下载）
     */
    private void streamFromHttp(String imageUrl, HttpServletResponse response) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = openHttp(imageUrl);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warn("HTTP 代理下载壁纸失败，远端状态码: {}，地址: {}", code, imageUrl);
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }
            String contentType = conn.getContentType();
            response.setContentType(contentType != null && !contentType.isEmpty()
                    ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            long len = conn.getContentLengthLong();
            if (len > 0) {
                response.setContentLengthLong(len);
            }
            try (InputStream in = conn.getInputStream()) {
                pipe(in, response.getOutputStream());
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 将图片字节复制到目标流（打包用）
     */
    private void copyHttpToStream(String imageUrl, OutputStream target) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = openHttp(imageUrl);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warn("HTTP 取图片失败，状态码: {}，地址: {}", code, imageUrl);
                return;
            }
            try (InputStream in = conn.getInputStream()) {
                pipe(in, target);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 打开 HTTP 连接，自动处理 URL 中未编码的中文字符。
     * 图床地址可能含原始中文（如 /ftp/我的图片/...），需逐段编码后再请求，
     * 否则 HttpURLConnection 会因非法字符报错。
     */
    private HttpURLConnection openHttp(String imageUrl) throws Exception {
        String encodedUrl = encodeUrlPath(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) URI.create(encodedUrl).toURL().openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 pnkx-wallpaper-proxy");
        conn.setInstanceFollowRedirects(true);
        conn.connect();
        return conn;
    }

    /**
     * 对 URL 中 path 部分的非 ASCII 字符做百分号编码，query/fragment 保持原样。
     * 已编码的 %XX 保持不变。
     */
    private String encodeUrlPath(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return url;
        }
        // host 段：在 :// 之后到第一个 / ? # 之间
        int pathStart = url.indexOf('/', schemeEnd + 3);
        int queryStart = url.indexOf('?');
        int fragStart = url.indexOf('#');
        int pathEnd = url.length();
        if (queryStart >= 0 && queryStart < pathEnd) {
            pathEnd = queryStart;
        }
        if (fragStart >= 0 && fragStart < pathEnd) {
            pathEnd = fragStart;
        }
        if (pathStart < 0) {
            // 无路径，无需编码
            return url;
        }
        String before = url.substring(0, pathStart);
        String path = url.substring(pathStart, pathEnd);
        String after = url.substring(pathEnd);

        // 逐段编码（保留 / 不被编码），已编码的 %XX 原样保留
        // path 形如 "/ftp/我的图片/x.png"，按 / 切分逐段编码后用 / 拼回，
        // 保留开头的 /。直接 split 会因首个 / 产生前导空串导致双斜杠，
        // 故跳过空段，首段前补回 /。
        StringBuilder sb = new StringBuilder();
        String[] segments = path.split("/", -1);
        for (int idx = 0; idx < segments.length; idx++) {
            String segment = segments[idx];
            if (segment.isEmpty()) {
                continue;
            }
            sb.append("/").append(encodeSegment(segment));
        }
        return before + sb + after;
    }

    /**
     * 编码单个 path 片段：保留 %XX 已编码片段与 ASCII 安全面字符，其余百分号编码
     */
    private String encodeSegment(String segment) {
        if (segment.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        int len = segment.length();
        while (i < len) {
            char c = segment.charAt(i);
            // 保留已有的百分号编码 %XX
            if (c == '%' && i + 2 < len) {
                char h1 = segment.charAt(i + 1);
                char h2 = segment.charAt(i + 2);
                if (isHex(h1) && isHex(h2)) {
                    out.append('%').append(h1).append(h2);
                    i += 3;
                    continue;
                }
            }
            if (c <= 127) {
                // ASCII 安全面字符原样保留（含中文常见分隔符等）
                out.append(c);
            } else {
                // 非 ASCII（如中文）按 UTF-8 百分号编码
                try {
                    byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                    for (byte b : bytes) {
                        out.append(String.format("%%%02X", b & 0xff));
                    }
                } catch (Exception e) {
                    out.append(c);
                }
            }
            i++;
        }
        return out.toString();
    }

    private boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * 高效拷贝流
     */
    private void pipe(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        out.flush();
    }

    /**
     * 解析逗号分隔的 id 列表
     */
    private Long[] parseIdList(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Long.valueOf(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toArray(Long[]::new);
    }

    /**
     * 构造 zip 内条目名（壁纸名称 + 扩展名），同名校验自动追加序号
     */
    private String buildEntryName(PxWallpaper wp, Set<String> usedNames) {
        String base = (wp.getName() != null && !wp.getName().trim().isEmpty())
                ? sanitizeFileName(wp.getName().trim()) : ("wallpaper_" + wp.getId());
        String ext = extractExtension(wp.getUrl());
        String name = ext != null ? base + "." + ext : base;
        // 去重
        if (!usedNames.add(name)) {
            int seq = 1;
            String candidate;
            do {
                candidate = ext != null ? base + "_" + seq + "." + ext : base + "_" + seq;
                seq++;
            } while (!usedNames.add(candidate));
            name = candidate;
        }
        return name;
    }

    /**
     * 去掉文件名里的非法字符
     */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 构造下载文件名：壁纸名称 + 扩展名，兜底用 id
     */
    private String buildDownloadName(PxWallpaper wallpaper, String imageUrl) {
        String filename = (wallpaper.getName() != null && !wallpaper.getName().trim().isEmpty())
                ? wallpaper.getName().trim() : String.valueOf(wallpaper.getId());
        String ext = extractExtension(imageUrl);
        return ext != null ? filename + "." + ext : filename;
    }

    /**
     * 从 URL 中提取文件扩展名（小写，不含点），无可识别扩展名返回 null
     */
    private String extractExtension(String url) {
        if (url == null) {
            return null;
        }
        int query = url.indexOf('?');
        String path = query >= 0 ? url.substring(0, query) : url;
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        String ext = name.substring(dot + 1).toLowerCase();
        if (ext.matches("[a-z0-9]{2,5}")) {
            return ext;
        }
        return null;
    }
}
