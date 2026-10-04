package com.pnkx.web.service;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件管理器服务（Obsidian Vault）
 * <p>
 * 从 SysFileManagerController 下沉的文件系统业务逻辑。
 * 路径锁定：/vol2/1000/我的文档/obsidian/
 *
 * @author phy
 */
@Service
public class SysFileManagerService {

    private static final String BASE_PATH = "/vol2/1000/我的文档/obsidian/";
    private static final Path BASE_DIR = Paths.get(BASE_PATH).normalize();
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList(".md", ".txt", ".png", ".jpg", ".jpeg", ".gif", ".pdf")
    );

    /**
     * 安全路径解析：将用户输入的相对路径解析到 BASE_DIR 内，
     * 经 normalize 消解 .. 与绝对路径后必须仍位于 BASE_DIR 之下，否则拒绝
     *
     * @return 解析后的绝对路径，非法时返回 null
     */
    static Path resolveSafePath(String relativePath) {
        // 用原生判空：StringUtils.isEmpty 对单个 NUL 字符也返回 true，会吞掉注入检查
        if (relativePath == null || relativePath.isEmpty()) {
            return BASE_DIR;
        }
        if (relativePath.contains("\0")) {
            return null;
        }
        Path resolved = BASE_DIR.resolve(relativePath).normalize();
        return resolved.startsWith(BASE_DIR) ? resolved : null;
    }

    /**
     * 校验文件名（新建文件/目录时使用，防止名称本身携带路径穿越）
     */
    static boolean isSafeName(String name) {
        return !StringUtils.isEmpty(name) && !name.contains("/") && !name.contains("\\")
                && !name.contains("..") && !name.contains("\0");
    }

    /**
     * 列出目录内容（目录在前、文件按名称排序）
     */
    public List<Map<String, Object>> listDir(String path) {
        Path dirPath = requireSafePath(path);
        File dir = dirPath.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            throw new ServiceException("目录不存在");
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        String relativeBase = StringUtils.isEmpty(path) ? "" : (path.endsWith("/") ? path : path + "/");
        List<Map<String, Object>> dirs = new ArrayList<>();
        List<Map<String, Object>> fileList = new ArrayList<>();

        for (File f : files) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", f.getName());
            item.put("path", relativeBase + f.getName());
            item.put("isDirectory", f.isDirectory());
            item.put("size", f.length());
            item.put("modifiedTime", f.lastModified());
            item.put("extension", f.isFile() ? getExtension(f.getName()) : "");

            if (f.isDirectory()) {
                dirs.add(item);
            } else {
                fileList.add(item);
            }
        }

        dirs.sort(Comparator.comparing(m -> (String) m.get("name")));
        fileList.sort(Comparator.comparing(m -> (String) m.get("name")));
        dirs.addAll(fileList);
        return dirs;
    }

    /**
     * 读取文件内容：图片返回 base64，文本返回 UTF-8 字符串
     */
    public Map<String, Object> read(String path) {
        Path safePath = requireSafePath(path);
        File file = safePath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new ServiceException("文件不存在");
        }
        if (!ALLOWED_EXTENSIONS.contains(getExtension(file.getName()).toLowerCase())) {
            throw new ServiceException("不支持的文件类型");
        }
        if (file.length() > MAX_FILE_SIZE) {
            throw new ServiceException("文件超过5MB限制");
        }

        String extension = getExtension(file.getName()).toLowerCase();
        String mimeType = getMimeType(extension);

        try {
            Map<String, Object> data = new HashMap<>();
            if (extension.equals(".png") || extension.equals(".jpg") || extension.equals(".jpeg")
                    || extension.equals(".gif")) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                data.put("content", Base64.getEncoder().encodeToString(bytes));
            } else {
                data.put("content", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            }
            data.put("mimeType", mimeType);
            data.put("isBase64", extension.equals(".png") || extension.equals(".jpg")
                    || extension.equals(".jpeg") || extension.equals(".gif"));
            data.put("size", file.length());
            return data;
        } catch (IOException e) {
            throw new ServiceException("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 覆写文件内容
     */
    public void write(String path, String content) {
        Path safePath = requireSafePath(path);
        File file = safePath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new ServiceException("文件不存在");
        }
        if (!ALLOWED_EXTENSIONS.contains(getExtension(file.getName()).toLowerCase())) {
            throw new ServiceException("不支持的文件类型");
        }
        if (file.length() > MAX_FILE_SIZE) {
            throw new ServiceException("文件超过5MB限制");
        }
        try {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ServiceException("保存失败: " + e.getMessage());
        }
    }

    /**
     * 创建目录
     */
    public void mkdir(String path, String name) {
        Path safePath = resolveSafePath(StringUtils.isEmpty(path) ? name : path + "/" + name);
        if (safePath == null || !isSafeName(name)) {
            throw new ServiceException("非法路径访问");
        }
        File dir = safePath.toFile();
        if (dir.exists()) {
            throw new ServiceException("目录已存在");
        }
        if (!dir.mkdirs()) {
            throw new ServiceException("创建目录失败");
        }
    }

    /**
     * 新建文件，返回相对路径
     */
    public String create(String path, String name, String content) {
        if (!isSafeName(name)) {
            throw new ServiceException("非法文件名称");
        }
        Path filePath = resolveSafePath(StringUtils.isEmpty(path) ? name : path + "/" + name);
        if (filePath == null) {
            throw new ServiceException("非法路径访问");
        }
        if (!ALLOWED_EXTENSIONS.contains(getExtension(name).toLowerCase())) {
            throw new ServiceException("不支持的文件类型");
        }
        if (filePath.toFile().exists()) {
            throw new ServiceException("文件已存在");
        }
        try {
            String defaultContent = StringUtils.isEmpty(content) ? "" : content;
            Files.write(filePath, defaultContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ServiceException("创建文件失败: " + e.getMessage());
        }
        return StringUtils.isEmpty(path) ? name : path + "/" + name;
    }

    /**
     * 删除文件/目录（禁止删除根目录本身）
     */
    public void delete(String path) {
        Path safePath = requireSafePath(path);
        File target = safePath.toFile();
        if (!target.exists()) {
            throw new ServiceException("文件或目录不存在");
        }
        if (safePath.equals(BASE_DIR)) {
            throw new ServiceException("不允许删除根目录");
        }
        try {
            if (target.isDirectory()) {
                deleteDirectory(target);
            } else {
                target.delete();
            }
        } catch (Exception e) {
            throw new ServiceException("删除失败: " + e.getMessage());
        }
    }

    /**
     * 移动/重命名
     */
    public void move(String oldPath, String newPath) {
        Path safeOldPath = resolveSafePath(oldPath);
        Path safeNewPath = resolveSafePath(newPath);
        if (safeOldPath == null || safeNewPath == null) {
            throw new ServiceException("非法路径访问");
        }
        File oldFile = safeOldPath.toFile();
        File newFile = safeNewPath.toFile();
        if (!oldFile.exists()) {
            throw new ServiceException("源文件不存在");
        }
        if (newFile.exists()) {
            throw new ServiceException("目标路径已存在");
        }
        try {
            Files.move(oldFile.toPath(), newFile.toPath());
        } catch (IOException e) {
            throw new ServiceException("移动/重命名失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文件：文件名匹配 + md/txt 内容匹配，最多返回 100 条
     */
    public List<Map<String, Object>> search(String q) {
        if (StringUtils.isEmpty(q)) {
            throw new ServiceException("关键词不能为空");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        Path rootPath = Paths.get(BASE_PATH);
        if (!Files.exists(rootPath)) {
            return results;
        }

        String query = q.toLowerCase();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString().toLowerCase();
                    if (fileName.contains(query)) {
                        addResult(file, false);
                    } else {
                        // 搜索 .md 文件内容（跳过超大文件）
                        String ext = getExtension(fileName);
                        if ((ext.equals(".md") || ext.equals(".txt")) && attrs.size() <= MAX_FILE_SIZE) {
                            try {
                                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                                if (content.toLowerCase().contains(query)) {
                                    addResult(file, true);
                                }
                            } catch (IOException ignored) {
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                private void addResult(Path file, boolean matchedContent) throws IOException {
                    String absolutePath = file.toAbsolutePath().toString();
                    String relativePath = absolutePath.substring(BASE_PATH.length());
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", file.getFileName().toString());
                    item.put("path", relativePath);
                    item.put("size", Files.size(file));
                    item.put("modifiedTime", Files.getLastModifiedTime(file).toMillis());
                    item.put("isDirectory", false);
                    item.put("matchedContent", matchedContent);
                    results.add(item);
                }
            });
        } catch (IOException e) {
            throw new ServiceException("搜索失败: " + e.getMessage());
        }

        return results.size() > 100 ? new ArrayList<>(results.subList(0, 100)) : results;
    }

    private Path requireSafePath(String relativePath) {
        Path safePath = resolveSafePath(relativePath);
        if (safePath == null) {
            throw new ServiceException("非法路径访问");
        }
        return safePath;
    }

    private void deleteDirectory(File dir) throws IOException {
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx) : "";
    }

    private String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case ".md":
            case ".txt":
                return "text/markdown; charset=utf-8";
            case ".png":
                return "image/png";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".gif":
                return "image/gif";
            case ".pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }
}
