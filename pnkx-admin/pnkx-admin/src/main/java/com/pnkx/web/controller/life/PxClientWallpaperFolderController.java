package com.pnkx.web.controller.life;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.domain.po.PxWallpaperFolder;
import com.pnkx.service.IPxWallpaperFolderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxClientWallpaperFolderController
 * @description 壁纸文件夹Controller（客户端免登录访问）
 */
@RestController
@RequestMapping("/client/wallpaper/folder")
public class PxClientWallpaperFolderController extends BaseController {

    @Resource
    private IPxWallpaperFolderService pxWallpaperFolderService;

    /**
     * 查询壁纸文件夹列表
     * 移动端只展示已启用（enabled = 1）的文件夹
     */
    @GetMapping("/list")
    public TableDataInfo list(PxWallpaperFolder pxWallpaperFolder) {
        // 客户端强制只返回启用文件夹，停用的文件夹移动端不可见
        pxWallpaperFolder.setEnabled(1);
        startPage();
        List<PxWallpaperFolder> list = pxWallpaperFolderService.selectPxWallpaperFolderList(pxWallpaperFolder);
        return getDataTable(list);
    }

    /**
     * 获取壁纸文件夹详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxWallpaperFolderService.selectPxWallpaperFolderById(id));
    }
}
