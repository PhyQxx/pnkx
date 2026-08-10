package com.pnkx.web.controller.common;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.utils.file.FileTypeUtils;
import com.pnkx.common.utils.file.MimeTypeUtils;
import com.pnkx.system.domain.SysFile;
import com.pnkx.system.service.ISysFileService;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.pnkx.common.config.PnkxConfig;
import com.pnkx.common.constant.Constants;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.file.FileUploadUtils;
import com.pnkx.common.utils.file.FileUtils;
import com.pnkx.framework.config.ServerConfig;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.pnkx.web.controller.system.SysFileController.deleteFile;

/**
 * 通用请求处理
 *
 * @author phy
 */
@RestController
public class CommonController {
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Resource
    private ISysFileService sysFileService;

    /**
     * 通用下载请求
     *
     * @param fileName 文件名称
     * @param delete   是否删除
     */
    @GetMapping("common/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request) {
        try {
            if (!FileUtils.checkAllowDownload(fileName)) {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = PnkxConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete) {
                FileUtils.deleteFile(filePath);
            }
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求
     */
    @PostMapping("/common/upload")
    public AjaxResult uploadFile(MultipartFile file, String fileType, String uploadPath) {
        try {
            // 上传并返回新文件名称
            String path = sysFileService.uploadMultipartFile(file, uploadPath);
            SysFile sysFile = new SysFile();
            sysFile.setPath(path);
            sysFile.setType(fileType);
            sysFile.setUrl(WebsiteAddressConstants.FTP_SITE_ADDRESS + path);
            sysFile.setName(file.getOriginalFilename());
            sysFile.setPort("博客管理端");
            // 图片压缩(大小超过1M)
            if (Arrays.asList(MimeTypeUtils.IMAGE_EXTENSION).contains(FileTypeUtils.getFileType(Objects.requireNonNull(file.getOriginalFilename()))) && file.getSize() > 1024*1024) {
                // 上传文件路径
                String filePath = PnkxConfig.getUploadPath();
                String thumbnail = filePath + File.separator + "thumbnail" + File.separator + file.getOriginalFilename();
                File thumbnailPath = new File(filePath + File.separator + "thumbnail");
                if (!thumbnailPath.exists()) {
                    thumbnailPath.mkdirs();
                }
                File fileTemp = FileUtils.convert(file);
                Thumbnails.of(fileTemp)
                        .size(128, 128)
                        .toFile(thumbnail);
                File thumbnailFile = new File(thumbnail);
                String thumbnailUrl = sysFileService.uploadFile(thumbnailFile, null, "thumbnail-" + file.getOriginalFilename());
                sysFile.setThumbnail(WebsiteAddressConstants.FTP_SITE_ADDRESS + thumbnailUrl);
                deleteFile(thumbnailFile);
            }
            sysFileService.insertSysFile(sysFile);
            AjaxResult ajax = AjaxResult.success();
            ajax.put("fileId", sysFile.getId());
            ajax.put("fileName", sysFile.getName());
            ajax.put("url", sysFile.getUrl());
            ajax.put("thumbnail", sysFile.getThumbnail());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @GetMapping("/common/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try {
            if (!FileUtils.checkAllowDownload(resource)) {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = PnkxConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }
}
