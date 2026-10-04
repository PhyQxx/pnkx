package com.pnkx.web.service;

import com.pnkx.common.config.PnkxConfig;
import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.file.FileTypeUtils;
import com.pnkx.common.utils.file.MimeTypeUtils;
import com.pnkx.common.utils.uuid.IdUtils;
import com.pnkx.system.domain.SysFile;
import com.pnkx.system.service.ISysFileService;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * 大文件分片上传服务
 * <p>
 * 从 SysFileController 下沉的分片落盘、校验白名单、分片合并、缩略图生成与 FTP 上传逻辑。
 * 校验失败抛 ServiceException（携带原接口文案），由 Controller 转为错误响应。
 *
 * @author phy
 */
@Service
public class SysFileChunkUploadService {

    private static final Logger log = LoggerFactory.getLogger(SysFileChunkUploadService.class);

    /**
     * 分片上传 identifier 白名单：字母数字下划线连字符，防路径穿越
     */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    /**
     * 分片序号/总数白名单：纯数字
     */
    private static final Pattern SAFE_CHUNK_NUMBER = Pattern.compile("^\\d{1,9}$");

    /**
     * 文件扩展名白名单：字母数字
     */
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^[A-Za-z0-9]{1,10}$");

    @Resource
    private ISysFileService sysFileService;

    /**
     * 分片上传结果：merged=true 表示全部分片集齐并完成合并上传（message 为最终 URL）
     */
    public static class ChunkUploadResult {
        public final boolean merged;
        public final String message;

        public ChunkUploadResult(boolean merged, String message) {
            this.merged = merged;
            this.message = message;
        }
    }

    /**
     * 保存一个分片；全部分片集齐时自动合并、生成缩略图（可选）并上传 FTP
     */
    public ChunkUploadResult saveChunk(MultipartFile chunk, String filename, String chunkNumber,
                                       String totalChunks, String identifier, String uploadPath,
                                       String fileType, Boolean isThumbnail) throws IOException {
        if (chunk == null || chunk.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        if (StringUtils.isEmpty(filename)) {
            filename = chunk.getOriginalFilename();
        }
        if (StringUtils.isEmpty(filename)) {
            throw new ServiceException("文件名不能为空");
        }
        log.info("上传文件：{}", filename);
        // 当前块的次序，第一个块是 1，注意不是从 0 开始的
        if (StringUtils.isEmpty(chunkNumber)) {
            chunkNumber = "1";
        }
        if (!SAFE_CHUNK_NUMBER.matcher(chunkNumber).matches() || "0".equals(chunkNumber)) {
            throw new ServiceException("非法的分片序号");
        }
        // 文件被分成块的总数
        if (StringUtils.isEmpty(totalChunks)) {
            totalChunks = "1";
        }
        if (!SAFE_CHUNK_NUMBER.matcher(totalChunks).matches()) {
            throw new ServiceException("非法的分片总数");
        }
        if (Integer.parseInt(chunkNumber) > Integer.parseInt(totalChunks)) {
            throw new ServiceException("分片序号超出总数");
        }
        // 文件唯一标识（用户可控，白名单校验防路径穿越）
        if (StringUtils.isEmpty(identifier)) {
            identifier = IdUtils.fastUUID();
        }
        if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new ServiceException("非法的文件标识");
        }
        // 扩展名参与分片/合并文件命名，白名单校验
        String extension = getSafeExtension(filename);
        uploadPath = sanitizeUploadPath(uploadPath);

        // 分片文件存放位置
        String undeterminedArea = PnkxConfig.getUploadPath() + File.separator + "undetermined" + File.separator + identifier;

        // 用于存储文件分片的文件夹
        File folder = new File(undeterminedArea);
        if (!folder.exists() && !folder.isDirectory()) {
            folder.mkdirs();
        }

        // 文件分片的路径
        String filePath = undeterminedArea + File.separator + chunkNumber + extension;
        File saveFile = new File(filePath);
        // 写入文件分片
        chunk.transferTo(saveFile);
        double uploaded = (double) Integer.parseInt(chunkNumber) / Integer.parseInt(totalChunks);
        NumberFormat nt = NumberFormat.getPercentInstance();
        nt.setMinimumFractionDigits(2);
        String mergeChunkName = "已上传" + nt.format(uploaded);
        // 获取分片文件
        File[] list = folder.listFiles();
        if (list == null) {
            throw new IOException("读取分片目录失败");
        }
        if (list.length == Integer.parseInt(totalChunks)) {
            // 合并文件分片
            mergeChunkName = mergeChunk(undeterminedArea, uploadPath, isThumbnail, identifier + extension, fileType, filename);
            return new ChunkUploadResult(true, mergeChunkName);
        }
        return new ChunkUploadResult(false, mergeChunkName);
    }

    /**
     * 提取安全的文件扩展名（仅字母数字，否则回退 .part），防止扩展名携带路径字符
     */
    private String getSafeExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return ".part";
        }
        String ext = filename.substring(idx + 1);
        return SAFE_EXTENSION.matcher(ext).matches() ? "." + ext : ".part";
    }

    /**
     * 净化上传子路径：仅允许相对路径（禁止绝对路径、反斜杠、冒号与 .. 上跳）
     */
    private String sanitizeUploadPath(String uploadPath) {
        if (StringUtils.isEmpty(uploadPath)) {
            return uploadPath;
        }
        String clean = uploadPath.replace('\\', '/');
        if (clean.contains(":") || clean.startsWith("/")) {
            return "";
        }
        String[] parts = clean.split("/", -1);
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (StringUtils.isEmpty(part) || ".".equals(part) || "..".equals(part)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(part);
        }
        return sb.toString();
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
     * @return 合并后文件的最终访问 URL
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
        if (chunks == null) {
            throw new IOException("读取分片目录失败");
        }
        // 排序
        File[] files = Arrays.stream(chunks)
                // 按照id值排序
                .sorted(Comparator.comparing(o -> Integer.valueOf(o.getName().substring(0, o.getName().lastIndexOf(".")))))
                .toArray(File[]::new);
        try {
            // 合并文件（try-with-resources 确保异常时释放句柄）
            try (RandomAccessFile randomAccessFileWriter = new RandomAccessFile(mergeFile, "rw")) {
                byte[] bytes = new byte[8192];
                for (File chunk : files) {
                    try (RandomAccessFile randomAccessFileReader = new RandomAccessFile(chunk, "r")) {
                        int len;
                        while ((len = randomAccessFileReader.read(bytes)) != -1) {
                            randomAccessFileWriter.write(bytes, 0, len);
                        }
                    }
                }
            }
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
        if (Boolean.TRUE.equals(Arrays.asList(MimeTypeUtils.IMAGE_EXTENSION).contains(FileTypeUtils.getFileType(mergeFile)) && isThumbnail) && mergeFile.length() > 1024 * 500) {
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
     * 删除文件（供 CommonController 等复用的公共工具）
     *
     * @param file 文件或目录
     * @return 是否删除
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
            if (files != null) {
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
            }
            // 文件夹删除
            file.delete();
            log.info("删除目录名：" + file.getName());
        }
        return true;
    }
}
