package com.pnkx.web.service;

import com.pnkx.life.domain.po.PxWallpaper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 壁纸传输服务
 * <p>
 * 从 PxClientWallpaperController 下沉的下载基础设施逻辑：
 * HTTP 图片代理（含中文 URL 逐段编码）、单张流式下载、ZIP 流式打包、
 * 下载文件名/条目名构造。Controller 保留状态码与响应头的编排。
 *
 * @author phy
 */
@Slf4j
@Service
public class PxWallpaperTransferService {

    /**
     * 通过 HTTP 代理读取图片并写入响应（单张下载）。
     * 远端非 2xx 时向响应写入 502，正常时透传 Content-Type/Content-Length。
     */
    public void streamFromHttp(String imageUrl, HttpServletResponse response) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = openHttp(imageUrl);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warn("HTTP 代理下载壁纸失败，远端状态码: {}，地址: {}", code, imageUrl);
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }
            String contentType = conn.getContentType();
            response.setContentType(contentType != null && !contentType.isEmpty()
                    ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            long len = conn.getContentLengthLong();
            if (len > 0) {
                response.setContentLengthLong(len);
            }
            try (InputStream in = conn.getInputStream()) {
                pipe(in, response.getOutputStream());
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 将图片字节复制到目标流（打包用）。远端非 2xx 时静默跳过（返回，不写任何字节）
     */
    public void copyHttpToStream(String imageUrl, OutputStream target) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = openHttp(imageUrl);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warn("HTTP 取图片失败，状态码: {}，地址: {}", code, imageUrl);
                return;
            }
            try (InputStream in = conn.getInputStream()) {
                pipe(in, target);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 把壁纸列表逐张写入 ZIP 输出流（流式，不落盘）。
     * 单张拉取失败不影响整包，仅记录日志。条目同名自动追加序号。
     */
    public void writeZip(List<PxWallpaper> wallpapers, ZipOutputStream zip) throws Exception {
        Set<String> usedNames = new java.util.HashSet<>();
        for (PxWallpaper wp : wallpapers) {
            if (wp.getUrl() == null || wp.getUrl().trim().isEmpty()) {
                continue;
            }
            String entryName = buildEntryName(wp, usedNames);
            zip.putNextEntry(new ZipEntry(entryName));
            try {
                copyHttpToStream(wp.getUrl().trim(), zip);
            } catch (Exception e) {
                // 单张失败不影响整包，仅记录日志
                log.warn("打包壁纸 {} 失败: {}", wp.getId(), e.getMessage());
            }
            zip.closeEntry();
            zip.flush();
        }
    }

    /**
     * 构造下载文件名：壁纸名称 + 扩展名，兜底用 id
     */
    public String buildDownloadName(PxWallpaper wallpaper, String imageUrl) {
        String filename = (wallpaper.getName() != null && !wallpaper.getName().trim().isEmpty())
                ? wallpaper.getName().trim() : String.valueOf(wallpaper.getId());
        String ext = extractExtension(imageUrl);
        return ext != null ? filename + "." + ext : filename;
    }

    /**
     * 打开 HTTP 连接，自动处理 URL 中未编码的中文字符。
     * 图床地址可能含原始中文（如 /ftp/我的图片/...），需逐段编码后再请求，
     * 否则 HttpURLConnection 会因非法字符报错。
     */
    private HttpURLConnection openHttp(String imageUrl) throws Exception {
        String encodedUrl = encodeUrlPath(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) URI.create(encodedUrl).toURL().openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 pnkx-wallpaper-proxy");
        conn.setInstanceFollowRedirects(true);
        conn.connect();
        return conn;
    }

    /**
     * 对 URL 中 path 部分的非 ASCII 字符做百分号编码，query/fragment 保持原样。
     * 已编码的 %XX 保持不变。
     */
    private String encodeUrlPath(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return url;
        }
        // host 段：在 :// 之后到第一个 / ? # 之间
        int pathStart = url.indexOf('/', schemeEnd + 3);
        int queryStart = url.indexOf('?');
        int fragStart = url.indexOf('#');
        int pathEnd = url.length();
        if (queryStart >= 0 && queryStart < pathEnd) {
            pathEnd = queryStart;
        }
        if (fragStart >= 0 && fragStart < pathEnd) {
            pathEnd = fragStart;
        }
        if (pathStart < 0) {
            // 无路径，无需编码
            return url;
        }
        String before = url.substring(0, pathStart);
        String path = url.substring(pathStart, pathEnd);
        String after = url.substring(pathEnd);

        // 逐段编码（保留 / 不被编码），已编码的 %XX 原样保留
        // path 形如 "/ftp/我的图片/x.png"，按 / 切分逐段编码后用 / 拼回，
        // 保留开头的 /。直接 split 会因首个 / 产生前导空串导致双斜杠，
        // 故跳过空段，首段前补回 /。
        StringBuilder sb = new StringBuilder();
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            sb.append("/").append(encodeSegment(segment));
        }
        return before + sb + after;
    }

    /**
     * 编码单个 path 片段：保留 %XX 已编码片段与 ASCII 安全面字符，其余百分号编码
     */
    private String encodeSegment(String segment) {
        if (segment.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        int len = segment.length();
        while (i < len) {
            char c = segment.charAt(i);
            // 保留已有的百分号编码 %XX
            if (c == '%' && i + 2 < len) {
                char h1 = segment.charAt(i + 1);
                char h2 = segment.charAt(i + 2);
                if (isHex(h1) && isHex(h2)) {
                    out.append('%').append(h1).append(h2);
                    i += 3;
                    continue;
                }
            }
            if (c <= 127) {
                // ASCII 安全面字符原样保留（含中文常见分隔符等）
                out.append(c);
            } else {
                // 非 ASCII（如中文）按 UTF-8 百分号编码
                try {
                    byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                    for (byte b : bytes) {
                        out.append(String.format("%%%02X", b & 0xff));
                    }
                } catch (Exception e) {
                    out.append(c);
                }
            }
            i++;
        }
        return out.toString();
    }

    private boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * 高效拷贝流
     */
    private void pipe(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        out.flush();
    }

    /**
     * 构造 zip 内条目名（壁纸名称 + 扩展名），同名校验自动追加序号
     */
    private String buildEntryName(PxWallpaper wp, Set<String> usedNames) {
        String base = (wp.getName() != null && !wp.getName().trim().isEmpty())
                ? sanitizeFileName(wp.getName().trim()) : ("wallpaper_" + wp.getId());
        String ext = extractExtension(wp.getUrl());
        String name = ext != null ? base + "." + ext : base;
        // 去重
        if (!usedNames.add(name)) {
            int seq = 1;
            String candidate;
            do {
                candidate = ext != null ? base + "_" + seq + "." + ext : base + "_" + seq;
                seq++;
            } while (!usedNames.add(candidate));
            name = candidate;
        }
        return name;
    }

    /**
     * 去掉文件名里的非法字符
     */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 从 URL 中提取文件扩展名（小写，不含点），无可识别扩展名返回 null
     */
    private String extractExtension(String url) {
        if (url == null) {
            return null;
        }
        int query = url.indexOf('?');
        String path = query >= 0 ? url.substring(0, query) : url;
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        String ext = name.substring(dot + 1).toLowerCase();
        if (ext.matches("[a-z0-9]{2,5}")) {
            return ext;
        }
        return null;
    }
}
