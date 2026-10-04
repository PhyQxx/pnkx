package com.pnkx.web.controller.system;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.system.domain.SysFile;
import com.pnkx.system.service.ISysFileService;
import com.pnkx.web.service.SysFileChunkUploadService;
import com.pnkx.web.service.SysFileChunkUploadService.ChunkUploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 通用请求处理
 * 分片上传业务逻辑见 SysFileChunkUploadService
 *
 * @author phy
 */
@RequestMapping("/system/file")
@RestController
public class SysFileController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(SysFileController.class);

    @Resource
    private ISysFileService sysFileService;

    @Resource
    private SysFileChunkUploadService chunkUploadService;

    /**
     * 大文件分片上传
     */
    @PostMapping(value = "/uploadLarge")
    public AjaxResult uploadLarge(
            @RequestParam("file") MultipartFile chunk,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "chunkNumber", required = false) String chunkNumber,
            @RequestParam(value = "totalChunks", required = false) String totalChunks,
            @RequestParam(value = "identifier", required = false) String identifier,
            @RequestParam(value = "uploadPath", required = false) String uploadPath,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "isThumbnail", defaultValue = "true") Boolean isThumbnail) {
        try {
            ChunkUploadResult result = chunkUploadService.saveChunk(
                    chunk, filename, chunkNumber, totalChunks, identifier, uploadPath, fileType, isThumbnail);
            return AjaxResult.success(result.message, result.merged);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("保存文件分片异常", e);
            return AjaxResult.error("保存文件分片异常");
        }
    }

    /**
     * 查询文件记录列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysFile sysFile) {
        startPage();
        List<SysFile> list = sysFileService.selectSysFileList(sysFile);
        return getDataTable(list);
    }

    /**
     * 导出文件记录列表
     */
    @Log(title = "文件记录", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(SysFile sysFile) {
        List<SysFile> list = sysFileService.selectSysFileList(sysFile);
        ExcelUtil<SysFile> util = new ExcelUtil<SysFile>(SysFile.class);
        return util.exportExcel(list, "file");
    }

    /**
     * 获取文件记录详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(sysFileService.selectSysFileById(id));
    }

    /**
     * 新增文件记录
     */
    @Log(title = "文件记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysFile sysFile) {
        return toAjax(sysFileService.insertSysFile(sysFile));
    }

    /**
     * 修改文件记录
     */
    @Log(title = "文件记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysFile sysFile) {
        return toAjax(sysFileService.updateSysFile(sysFile));
    }

    /**
     * 删除文件记录
     */
    @Log(title = "文件记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(sysFileService.deleteSysFileByIds(ids));
    }
}
