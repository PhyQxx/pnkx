package com.pnkx.web.controller.system;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.web.service.SysFileManagerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 文件管理器 - Obsidian Vault
 * 业务逻辑（路径安全/读写/搜索）见 SysFileManagerService
 *
 * @author phy
 */
@PreAuthorize("@ss.hasRole('admin')")
@RestController
@RequestMapping("/system/file-manager")
public class SysFileManagerController extends BaseController {

    @Resource
    private SysFileManagerService fileManagerService;

    /**
     * 列出目录内容
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String path) {
        try {
            return AjaxResult.success(fileManagerService.listDir(path));
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 读取文件内容
     */
    @GetMapping("/read")
    public AjaxResult read(@RequestParam String path) {
        try {
            return AjaxResult.success(fileManagerService.read(path));
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 保存文件
     */
    @PutMapping("/write")
    public AjaxResult write(@RequestBody Map<String, String> body) {
        try {
            fileManagerService.write(body.get("path"), body.get("content"));
            return AjaxResult.success("保存成功");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 创建目录
     */
    @PostMapping("/mkdir")
    public AjaxResult mkdir(@RequestBody Map<String, String> body) {
        try {
            fileManagerService.mkdir(body.get("path"), body.get("name"));
            return AjaxResult.success("创建成功");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 新建文件
     */
    @PostMapping("/create")
    public AjaxResult create(@RequestBody Map<String, String> body) {
        try {
            return AjaxResult.success("创建成功", fileManagerService.create(body.get("path"), body.get("name"), body.get("content")));
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 删除文件/目录
     */
    @DeleteMapping("/")
    public AjaxResult delete(@RequestParam String path) {
        try {
            fileManagerService.delete(path);
            return AjaxResult.success("删除成功");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 重命名/移动
     */
    @PutMapping("/move")
    public AjaxResult move(@RequestBody Map<String, String> body) {
        try {
            fileManagerService.move(body.get("oldPath"), body.get("newPath"));
            return AjaxResult.success("移动/重命名成功");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 搜索文件
     */
    @GetMapping("/search")
    public AjaxResult search(@RequestParam String q) {
        try {
            List<Map<String, Object>> results = fileManagerService.search(q);
            return AjaxResult.success(results);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
