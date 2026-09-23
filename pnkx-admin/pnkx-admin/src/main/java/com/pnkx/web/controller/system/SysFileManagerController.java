package com.pnkx.web.controller.system;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.StringUtils;

/**
 * 文件管理器 - Obsidian Vault
 * 路径锁定：/vol2/1000/我的文档/obsidian/
 *
 * @author phy
 */
@PreAuthorize("@ss.hasRole('admin')")
@RestController
@RequestMapping("/system/file-manager")
public class SysFileManagerController extends BaseController {

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
     * 列出目录内容
     * GET /system/file-manager/list?path=JXD/
     * GET /system/file-manager/list  （默认列出根目录）
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String path) {
        Path dirPath = resolveSafePath(path);
        if (dirPath == null) {
            return AjaxResult.error("非法路径访问");
        }

        File dir = dirPath.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return AjaxResult.error("目录不存在");
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return AjaxResult.success(Collections.emptyList());
        }

        String relativeBase = StringUtils.isEmpty(path) ? "" : (path.endsWith("/") ? path : path + "/");
        List<Map<String, Object>> result = new ArrayList<>();
        // 目录优先排序
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

        // 排序：目录在前，文件按名称
        dirs.sort(Comparator.comparing(m -> (String) m.get("name")));
        fileList.sort(Comparator.comparing(m -> (String) m.get("name")));
        dirs.addAll(fileList);
        return AjaxResult.success(dirs);
    }

    /**
     * 读取文件内容
     * GET /system/file-manager/read?path=JXD/笔记/test.md
     */
    @GetMapping("/read")
    public AjaxResult read(@RequestParam String path) {
        Path safePath = resolveSafePath(path);
        if (safePath == null) {
            return AjaxResult.error("非法路径访问");
        }

        File file = safePath.toFile();
        if (!file.exists() || !file.isFile()) {
            return AjaxResult.error("文件不存在");
        }
        if (!ALLOWED_EXTENSIONS.contains(getExtension(file.getName()).toLowerCase())) {
            return AjaxResult.error("不支持的文件类型");
        }
        if (file.length() > MAX_FILE_SIZE) {
            return AjaxResult.error("文件超过5MB限制");
        }

        String extension = getExtension(file.getName()).toLowerCase();
        String mimeType = getMimeType(extension);

        try {
            if (extension.equals(".png") || extension.equals(".jpg") || extension.equals(".jpeg")
                    || extension.equals(".gif")) {
                // 图片返回 base64
                byte[] bytes = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);
                Map<String, Object> data = new HashMap<>();
                data.put("content", base64);
                data.put("mimeType", mimeType);
                data.put("isBase64", true);
                data.put("size", file.length());
                return AjaxResult.success(data);
            } else {
                // 文本文件
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                Map<String, Object> data = new HashMap<>();
                data.put("content", content);
                data.put("mimeType", mimeType);
                data.put("isBase64", false);
                data.put("size", file.length());
                return AjaxResult.success(data);
            }
        } catch (IOException e) {
            return AjaxResult.error("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 保存文件
     * PUT /system/file-manager/write
     * body: {path, content}
     */
    @PutMapping("/write")
    public AjaxResult write(@RequestBody Map<String, String> body) {
        String path = body.get("path");
        String content = body.get("content");
        Path safePath = resolveSafePath(path);
        if (safePath == null) {
            return AjaxResult.error("非法路径访问");
        }

        File file = safePath.toFile();
        if (!file.exists() || !file.isFile()) {
            return AjaxResult.error("文件不存在");
        }
        if (!ALLOWED_EXTENSIONS.contains(getExtension(file.getName()).toLowerCase())) {
            return AjaxResult.error("不支持的文件类型");
        }
        if (file.length() > MAX_FILE_SIZE) {
            return AjaxResult.error("文件超过5MB限制");
        }

        try {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return AjaxResult.success("保存成功");
        } catch (IOException e) {
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 创建目录
     * POST /system/file-manager/mkdir
     * body: {path, name}
     */
    @PostMapping("/mkdir")
    public AjaxResult mkdir(@RequestBody Map<String, String> body) {
        String path = body.get("path");
        String name = body.get("name");
        Path safePath = resolveSafePath(StringUtils.isEmpty(path) ? name : path + "/" + name);
        if (safePath == null || !isSafeName(name)) {
            return AjaxResult.error("非法路径访问");
        }

        File dir = safePath.toFile();
        if (dir.exists()) {
            return AjaxResult.error("目录已存在");
        }
        if (!dir.mkdirs()) {
            return AjaxResult.error("创建目录失败");
        }
        return AjaxResult.success("创建成功");
    }

    /**
     * 新建文件
     * POST /system/file-manager/create
     * body: {path, name, content}
     */
    @PostMapping("/create")
    public AjaxResult create(@RequestBody Map<String, String> body) {
        String path = body.get("path");
        String name = body.get("name");
        String content = body.get("content");
        if (!isSafeName(name)) {
            return AjaxResult.error("非法文件名称");
        }
        Path filePath = resolveSafePath(StringUtils.isEmpty(path) ? name : path + "/" + name);
        if (filePath == null) {
            return AjaxResult.error("非法路径访问");
        }
        if (!ALLOWED_EXTENSIONS.contains(getExtension(name).toLowerCase())) {
            return AjaxResult.error("不支持的文件类型");
        }

        File file = filePath.toFile();
        if (file.exists()) {
            return AjaxResult.error("文件已存在");
        }

        try {
            // 默认内容
            String defaultContent = StringUtils.isEmpty(content) ? "" : content;
            Files.write(filePath, defaultContent.getBytes(StandardCharsets.UTF_8));
            String relativePath = StringUtils.isEmpty(path) ? name : path + "/" + name;
            return AjaxResult.success("创建成功", relativePath);
        } catch (IOException e) {
            return AjaxResult.error("创建文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件/目录
     * DELETE /system/file-manager?path=JXD/笔记/test.md
     */
    @DeleteMapping("/")
    public AjaxResult delete(@RequestParam String path) {
        Path safePath = resolveSafePath(path);
        if (safePath == null) {
            return AjaxResult.error("非法路径访问");
        }

        File target = safePath.toFile();
        if (!target.exists()) {
            return AjaxResult.error("文件或目录不存在");
        }
        // 禁止删除根目录本身
        if (safePath.equals(BASE_DIR)) {
            return AjaxResult.error("不允许删除根目录");
        }

        try {
            if (target.isDirectory()) {
                deleteDirectory(target);
            } else {
                target.delete();
            }
            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
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

    /**
     * 重命名/移动
     * PUT /system/file-manager/move
     * body: {oldPath, newPath}
     */
    @PutMapping("/move")
    public AjaxResult move(@RequestBody Map<String, String> body) {
        String oldPath = body.get("oldPath");
        String newPath = body.get("newPath");
        Path safeOldPath = resolveSafePath(oldPath);
        Path safeNewPath = resolveSafePath(newPath);
        if (safeOldPath == null || safeNewPath == null) {
            return AjaxResult.error("非法路径访问");
        }

        File oldFile = safeOldPath.toFile();
        File newFile = safeNewPath.toFile();

        if (!oldFile.exists()) {
            return AjaxResult.error("源文件不存在");
        }
        if (newFile.exists()) {
            return AjaxResult.error("目标路径已存在");
        }

        try {
            Files.move(oldFile.toPath(), newFile.toPath());
            return AjaxResult.success("移动/重命名成功");
        } catch (IOException e) {
            return AjaxResult.error("移动/重命名失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文件
     * GET /system/file-manager/search?q=关键词
     */
    @GetMapping("/search")
    public AjaxResult search(@RequestParam String q) {
        if (StringUtils.isEmpty(q)) {
            return AjaxResult.error("关键词不能为空");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        Path rootPath = Paths.get(BASE_PATH);
        if (!Files.exists(rootPath)) {
            return AjaxResult.success(results);
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
                            } catch (IOException ignored) {}
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
            return AjaxResult.error("搜索失败: " + e.getMessage());
        }

        // 限制返回数量
        if (results.size() > 100) {
            return AjaxResult.success(results.subList(0, 100));
        }
        return AjaxResult.success(results);
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
