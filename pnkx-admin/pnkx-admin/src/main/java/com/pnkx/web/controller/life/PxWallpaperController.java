package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxLikeRecord;
import com.pnkx.domain.po.PxWallpaper;
import com.pnkx.domain.po.PxWallpaperDownloadRecord;
import com.pnkx.mapper.PxLikeRecordMapper;
import com.pnkx.mapper.PxWallpaperDownloadRecordMapper;
import com.pnkx.service.IPxWallpaperService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author PHY
 * @classname PxWallpaperController
 * @description 壁纸Controller（需登录，用于后台维护）
 */
@RestController
@RequestMapping("/wallpaper")
public class PxWallpaperController extends BaseController {

    @Resource
    private IPxWallpaperService pxWallpaperService;

    @Resource
    private PxLikeRecordMapper pxLikeRecordMapper;

    @Resource
    private PxWallpaperDownloadRecordMapper pxWallpaperDownloadRecordMapper;

    private static final String WALLPAPER_LIKE_TYPE = "3";

    /**
     * 查询壁纸列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxWallpaper pxWallpaper) {
        startPage();
        List<PxWallpaper> list = pxWallpaperService.selectPxWallpaperList(pxWallpaper);
        return getDataTable(list);
    }

    /**
     * 导出壁纸列表
     */
    @Log(title = "壁纸", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxWallpaper pxWallpaper) {
        List<PxWallpaper> list = pxWallpaperService.selectPxWallpaperList(pxWallpaper);
        ExcelUtil<PxWallpaper> util = new ExcelUtil<>(PxWallpaper.class);
        return util.exportExcel(list, "wallpaper");
    }

    /**
     * 获取壁纸详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxWallpaperService.selectPxWallpaperById(id));
    }

    /**
     * 新增壁纸
     */
    @Log(title = "壁纸", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxWallpaper pxWallpaper) {
        return AjaxResult.success(pxWallpaperService.insertPxWallpaper(pxWallpaper));
    }

    /**
     * 修改壁纸
     */
    @Log(title = "壁纸", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxWallpaper pxWallpaper) {
        return AjaxResult.success(pxWallpaperService.updatePxWallpaper(pxWallpaper));
    }

    /**
     * 删除壁纸
     */
    @Log(title = "壁纸", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxWallpaperService.deletePxWallpaperByIds(ids));
    }

    /**
     * 管理端：查询所有点赞记录（分页，JOIN 壁纸表取名称缩略图，支持按用户筛选）
     */
    @GetMapping("/records/likes")
    public TableDataInfo allLikes(@RequestParam(required = false) String createBy) {
        startPage();
        PxLikeRecord param = new PxLikeRecord();
        param.setType(WALLPAPER_LIKE_TYPE);
        param.setCreateBy(createBy);
        List<PxLikeRecord> list = pxLikeRecordMapper.selectRecordListWithWallpaper(param);
        return getDataTable(list);
    }

    /**
     * 管理端：查询所有下载记录（分页，支持按用户筛选）
     */
    @GetMapping("/records/downloads")
    public TableDataInfo allDownloads(@RequestParam(required = false) String createBy) {
        startPage();
        PxWallpaperDownloadRecord param = new PxWallpaperDownloadRecord();
        param.setCreateBy(createBy);
        List<PxWallpaperDownloadRecord> list = pxWallpaperDownloadRecordMapper.selectMyDownloadList(param);
        return getDataTable(list);
    }

    /**
     * 管理端：操作记录中出现过的用户（用于用户筛选下拉）
     *
     * @param type like（点赞）/download（下载）
     */
    @GetMapping("/records/users")
    public AjaxResult recordUsers(@RequestParam String type) {
        List<Map<String, Object>> users;
        if ("download".equals(type)) {
            users = pxWallpaperDownloadRecordMapper.selectDownloadRecordUsers();
        } else {
            PxLikeRecord param = new PxLikeRecord();
            param.setType(WALLPAPER_LIKE_TYPE);
            users = pxLikeRecordMapper.selectRecordUsers(param);
        }
        return AjaxResult.success(users);
    }

    /**
     * 管理端：操作记录统计（按日期趋势）
     */
    @GetMapping("/records/statsByDate")
    public AjaxResult statsByDate(@RequestParam(required = false) String beginTime,
                                  @RequestParam(required = false) String endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("likeType", WALLPAPER_LIKE_TYPE);
        params.put("beginTime", beginTime);
        params.put("endTime", endTime);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("like", pxLikeRecordMapper.selectRecordStatsByDate(params));
        ajax.put("download", pxWallpaperDownloadRecordMapper.selectDownloadStatsByDate(params));
        return ajax;
    }

    /**
     * 管理端：操作记录统计（按文件夹分布）
     */
    @GetMapping("/records/statsByFolder")
    public AjaxResult statsByFolder() {
        Map<String, Object> params = new HashMap<>();
        params.put("likeType", WALLPAPER_LIKE_TYPE);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("like", pxLikeRecordMapper.selectRecordStatsByFolder(params));
        ajax.put("download", pxWallpaperDownloadRecordMapper.selectDownloadStatsByFolder(params));
        return ajax;
    }

    /**
     * 管理端：下载记录按用户统计（用户汇总，支持按用户名/次数排序）
     */
    @GetMapping("/records/downloadStatsByUser")
    public AjaxResult downloadStatsByUser(@RequestParam(required = false) String beginTime,
                                          @RequestParam(required = false) String endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("beginTime", beginTime);
        params.put("endTime", endTime);
        List<Map<String, Object>> list = pxWallpaperDownloadRecordMapper.selectDownloadStatsByUser(params);
        // 计算总下载次数
        long total = list.stream().mapToLong(m -> ((Number) m.get("count")).longValue()).sum();
        AjaxResult ajax = AjaxResult.success(list);
        ajax.put("total", total);
        return ajax;
    }

    /**
     * 管理端：下载记录按用户+日期统计明细
     */
    @GetMapping("/records/downloadStatsByUserDate")
    public AjaxResult downloadStatsByUserDate(@RequestParam(required = false) String beginTime,
                                              @RequestParam(required = false) String endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("beginTime", beginTime);
        params.put("endTime", endTime);
        return AjaxResult.success(pxWallpaperDownloadRecordMapper.selectDownloadStatsByUserDate(params));
    }
}
