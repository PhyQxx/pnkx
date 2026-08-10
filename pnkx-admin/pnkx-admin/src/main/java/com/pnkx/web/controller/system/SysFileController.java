package com.pnkx.web.controller.system;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.config.PnkxConfig;
import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.file.FileTypeUtils;
import com.pnkx.common.utils.file.MimeTypeUtils;
import com.pnkx.common.utils.uuid.IdUtils;
import com.pnkx.system.domain.SysFile;
import com.pnkx.system.service.ISysFileService;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 通用请求处理
 *
 * @author phy
 */
@RequestMapping("/system/file")
@RestController
public class SysFileController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(SysFileController.class);

    @Resource
    private ISysFileService sysFileService;


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
        // 文件
        if (chunk == null || chunk.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }
        // 文件名
        if (StringUtils.isEmpty(filename)) {
            filename = chunk.getOriginalFilename();
        }
        if (StringUtils.isEmpty(filename)) {
            return AjaxResult.error("文件名不能为空");
        }
        log.info("上传文件：{}", filename);
        // 当前块的次序，第一个块是 1，注意不是从 0 开始的
        if (StringUtils.isEmpty(chunkNumber)) {
            chunkNumber = "1";
        }
        // 文件被分成块的总数
        if (StringUtils.isEmpty(totalChunks)) {
            totalChunks = "1";
        }
        // 文件唯一标识
        if (StringUtils.isEmpty(identifier)) {
            identifier = IdUtils.fastUUID();
        }

        // 分片文件存放位置
        String undeterminedArea = PnkxConfig.getUploadPath() + File.separator + "undetermined" + File.separator + identifier;

        // 用于存储文件分片的文件夹
        File folder = new File(undeterminedArea);
        if (!folder.exists() && !folder.isDirectory()) {
            folder.mkdirs();
        }

        // 文件分片的路径
        String filePath = undeterminedArea + File.separator + chunkNumber + filename.substring(filename.lastIndexOf("."));
        try {
            File saveFile = new File(filePath);
            // 写入文件分片
            chunk.transferTo(saveFile);
            double uploaded = (double) Integer.parseInt(chunkNumber) / Integer.parseInt(totalChunks);
            NumberFormat nt = NumberFormat.getPercentInstance();
            nt.setMinimumFractionDigits(2);
            String mergeChunkName = "已上传" + nt.format(uploaded);
            // 获取分片文件
            File[] list = folder.listFiles();
            assert list != null;
            if (list.length == Integer.parseInt(totalChunks)) {
                // 合并文件分片
                mergeChunkName = mergeChunk(undeterminedArea, uploadPath, isThumbnail, identifier + filename.substring(filename.lastIndexOf(".")), fileType, filename);
                return AjaxResult.success(mergeChunkName, true);
            }
            return AjaxResult.success(mergeChunkName, false);
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

    /**
     * 合并文件分片
     *
     * @param path         文件分片所在的文件夹
     * @param uploadPath   上传指定路径
     * @param isThumbnail  是否生成缩略图
     * @param fileName     文件名
     * @param fileType     文件类型
     * @param originalName 文件名包括后缀
     * @return
     * @throws IOException
     */
    private String mergeChunk(String path, String uploadPath, Boolean isThumbnail, String fileName, String fileType, String originalName) throws IOException {
        // 文件分片所在的文件夹
        File chunkFileFolder = new File(path);
        // 合并后的文件的路径
        String newFilePath = PnkxConfig.getUploadPath();
        File file = new File(newFilePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        File mergeFile = new File(newFilePath + File.separator + fileName);
        // 得到文件分片所在的文件夹下的所有文件
        File[] chunks = chunkFileFolder.listFiles();
        assert chunks != null;
        // 排序
        File[] files = Arrays.stream(chunks)
                // 按照id值排序
                .sorted(Comparator.comparing(o -> Integer.valueOf(o.getName().substring(0, o.getName().lastIndexOf(".")))))
                .toArray(File[]::new);
        try {
            // 合并文件
            RandomAccessFile randomAccessFileWriter = new RandomAccessFile(mergeFile, "rw");
            byte[] bytes = new byte[1024];
            for (File chunk : files) {
                RandomAccessFile randomAccessFileReader = new RandomAccessFile(chunk, "r");
                int len;
                while ((len = randomAccessFileReader.read(bytes)) != -1) {
                    randomAccessFileWriter.write(bytes, 0, len);
                }
                randomAccessFileReader.close();
            }
            randomAccessFileWriter.close();
        } catch (Exception e) {
            log.error("合并文件异常", e);
            throw new IOException("合并文件异常", e);
        }
        // 合并后删除分片文件
        deleteFile(chunkFileFolder);
        String mergeFilePath = sysFileService.uploadFile(mergeFile, uploadPath, fileName);
        SysFile sysFile = new SysFile();
        sysFile.setPath(mergeFilePath);
        sysFile.setUrl(WebsiteAddressConstants.FTP_SITE_ADDRESS + mergeFilePath);
        sysFile.setName(originalName);
        sysFile.setPort("博客管理端分片上传");
        sysFile.setType(fileType);
        if (Boolean.TRUE.equals(Arrays.asList(MimeTypeUtils.IMAGE_EXTENSION).contains(FileTypeUtils.getFileType(mergeFile)) && isThumbnail) && mergeFile.length() > 1024*500) {
            String thumbnail = newFilePath + File.separator + "thumbnail" + File.separator + fileName;
            File thumbnailPath = new File(newFilePath + File.separator + "thumbnail");
            if (!thumbnailPath.exists()) {
                thumbnailPath.mkdirs();
            }
            Thumbnails.of(mergeFile)
                    .size(128, 128)
                    .toFile(thumbnail);
            File thumbnailFile = new File(thumbnail);
            String thumbnailUrl = sysFileService.uploadFile(thumbnailFile, uploadPath, "thumbnail-" + fileName);
            sysFile.setThumbnail(WebsiteAddressConstants.FTP_SITE_ADDRESS + thumbnailUrl);
            deleteFile(thumbnailFile);
        }
        // 上传到FTP后删除本地文件
        deleteFile(mergeFile);
        sysFileService.insertSysFile(sysFile);
        return sysFile.getUrl();
    }

    /**
     * 删除文件
     *
     * @param file
     * @return
     */
    public static Boolean deleteFile(File file) {
        // 判断文件不为null或文件目录存在
        if (file == null || !file.exists()) {
            log.error("文件删除失败,请检查文件是否存在以及文件路径是否正确");
            return false;
        }
        if (file.isFile()) {
            // 文件删除
            file.delete();
            // 打印文件名
            log.info("删除文件名：" + file.getName());
        } else if (file.isDirectory()) {
            //获取目录下子文件
            File[] files = file.listFiles();
            //遍历该目录下的文件对象
            for (File f : files) {
                // 判断子目录是否存在子目录,如果是文件则删除
                if (f.isDirectory()) {
                    // 递归删除目录下的文件
                    deleteFile(f);
                } else {
                    // 文件删除
                    f.delete();
                    // 打印文件名
                    log.info("删除文件名：" + f.getName());
                }
            }
            // 文件夹删除
            file.delete();
            log.info("删除目录名：" + file.getName());
        }
        return true;
    }
}
