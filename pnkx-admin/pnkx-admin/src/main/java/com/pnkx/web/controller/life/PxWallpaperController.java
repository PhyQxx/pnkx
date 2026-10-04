package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.life.domain.po.PxWallpaper;
import com.pnkx.life.service.IPxWallpaperService;
import com.pnkx.web.service.PxWallpaperDownloadService;
import com.pnkx.web.service.PxWallpaperLikeService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
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
    private PxWallpaperLikeService wallpaperLikeService;

    @Resource
    private PxWallpaperDownloadService wallpaperDownloadService;

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
        return getDataTable(wallpaperLikeService.selectAllLikes(createBy));
    }

    /**
     * 管理端：查询所有下载记录（分页，支持按用户筛选）
     */
    @GetMapping("/records/downloads")
    public TableDataInfo allDownloads(@RequestParam(required = false) String createBy) {
        startPage();
        return getDataTable(wallpaperDownloadService.selectAllDownloads(createBy));
    }

    /**
     * 管理端：操作记录中出现过的用户（用于用户筛选下拉）
     *
     * @param type like（点赞）/download（下载）
     */
    @GetMapping("/records/users")
    public AjaxResult recordUsers(@RequestParam String type) {
        List<Map<String, Object>> users = "download".equals(type)
                ? wallpaperDownloadService.selectDownloadRecordUsers()
                : wallpaperLikeService.selectLikeRecordUsers();
        return AjaxResult.success(users);
    }

    /**
     * 管理端：操作记录统计（按日期趋势）
     */
    @GetMapping("/records/statsByDate")
    public AjaxResult statsByDate(@RequestParam(required = false) String beginTime,
                                  @RequestParam(required = false) String endTime) {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("like", wallpaperLikeService.selectLikeStatsByDate(beginTime, endTime));
        ajax.put("download", wallpaperDownloadService.selectDownloadStatsByDate(beginTime, endTime));
        return ajax;
    }

    /**
     * 管理端：操作记录统计（按文件夹分布）
     */
    @GetMapping("/records/statsByFolder")
    public AjaxResult statsByFolder() {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("like", wallpaperLikeService.selectLikeStatsByFolder());
        ajax.put("download", wallpaperDownloadService.selectDownloadStatsByFolder());
        return ajax;
    }

    /**
     * 管理端：下载记录按用户统计（用户汇总，支持按用户名/次数排序）
     */
    @GetMapping("/records/downloadStatsByUser")
    public AjaxResult downloadStatsByUser(@RequestParam(required = false) String beginTime,
                                          @RequestParam(required = false) String endTime) {
        List<Map<String, Object>> list = wallpaperDownloadService.selectDownloadStatsByUser(beginTime, endTime);
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
        return AjaxResult.success(wallpaperDownloadService.selectDownloadStatsByUserDate(beginTime, endTime));
    }
}
