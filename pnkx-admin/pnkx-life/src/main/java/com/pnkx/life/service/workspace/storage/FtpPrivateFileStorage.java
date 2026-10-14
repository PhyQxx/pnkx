package com.pnkx.life.service.workspace.storage;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.ftp.FtpTool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FTP 后端（生产环境）。私有文件存放于 FTP 上独立于公开静态目录（ftp.path）的私有根目录，
 * 读取仅经 /workspace/files/{id}/content 授权接口流式转发，FTP 目录本身不对外暴露 HTTP。
 */
@Component
@ConditionalOnProperty(name = "pnkx.private-files.storage", havingValue = "ftp")
public class FtpPrivateFileStorage implements PrivateFileStorage {
    private static final Logger log = LoggerFactory.getLogger(FtpPrivateFileStorage.class);
    @Resource private FtpTool ftpTool;
    @Value("${pnkx.private-files.ftp-root:/pnkx-private}") private String ftpRoot;
    @Value("${ftp.path}") private String publicPath;

    @Override public String type() { return "ftp"; }
    @Override @PostConstruct public void validateAtStartup() throws IOException {
        String root = normalizeRoot();
        String pub = normalize(publicPath);
        if (pub != null && (root.equals(pub) || root.startsWith(pub + "/")))
            throw new ServiceException("私有文件 FTP 目录（pnkx.private-files.ftp-root=" + root + "）不能位于公开上传目录（ftp.path=" + pub + "）之内");
        FTPClient client = connect();
        try {
            if (!client.changeWorkingDirectory(root)) {
                client.makeDirectory(root);
                if (!client.changeWorkingDirectory(root)) throw new ServiceException("无法在 FTP 上创建私有目录 " + root);
            }
            log.info("私有文件存储使用 FTP 后端，私有目录 {}（公开目录 {} 隔离校验通过）", root, pub);
        } finally { disconnect(client); }
    }
    private String normalizeRoot() {
        String r = normalize(ftpRoot);
        if (r == null || r.equals("/")) throw new ServiceException("pnkx.private-files.ftp-root 必须是 / 开头且非根的独立目录");
        return r;
    }
    private static String normalize(String p) {
        if (p == null || p.isBlank()) return null;
        String r = p.trim();
        if (!r.startsWith("/")) r = "/" + r;
        while (r.length() > 1 && r.endsWith("/")) r = r.substring(0, r.length() - 1);
        return r;
    }
    private FTPClient connect() throws IOException {
        FTPClient client = ftpTool.connectFtp();
        if (client == null) throw new ServiceException("FTP 存储不可用，请检查 FTP 配置");
        return client;
    }
    private void disconnect(FTPClient client) { ftpTool.closeFtpClient(client); }
    private String absolute(String objectKey) { return normalizeRoot() + "/" + objectKey; }

    @Override public void store(String objectKey, Path staged) throws IOException {
        FTPClient client = connect();
        boolean ok = false;
        try {
            client.enterLocalPassiveMode();
            if (!client.changeWorkingDirectory(normalizeRoot())) { client.makeDirectory(normalizeRoot()); }
            try (InputStream in = Files.newInputStream(staged)) {
                // objectKey 为全新 UUID，直接覆盖写，禁止 FtpTool 上传路径的自动改名逻辑
                ok = client.storeFile(absolute(objectKey), in);
            }
            if (!ok || !FTPReply.isPositiveCompletion(client.getReplyCode()))
                throw new ServiceException("FTP 写入私有文件失败（reply=" + client.getReplyCode() + "）");
        } finally {
            try { client.logout(); } catch (IOException ignored) { }
            disconnect(client);
        }
    }
    @Override public InputStream open(String objectKey) throws IOException {
        FTPClient client = connect();
        client.enterLocalPassiveMode();
        InputStream raw = client.retrieveFileStream(absolute(objectKey));
        if (raw == null || !FTPReply.isPositivePreliminary(client.getReplyCode())) {
            try { client.logout(); } catch (IOException ignored) { }
            disconnect(client);
            throw new ServiceException("文件暂时不可用");
        }
        // retrieveFileStream 读取完成后必须 completePendingCommand，连接生命周期随流关闭一起结束
        return new FilterInputStream(raw) {
            private boolean closed = false;
            @Override public void close() throws IOException {
                if (closed) return;
                closed = true;
                try { super.close(); } finally {
                    try { if (client.isConnected()) client.completePendingCommand(); } catch (IOException ignored) { }
                    try { client.logout(); } catch (IOException ignored) { }
                    disconnect(client);
                }
            }
        };
    }
    @Override public void deleteQuietly(String objectKey) {
        FTPClient client = null;
        try {
            client = ftpTool.connectFtp();
            if (client == null) { log.error("清理私有文件失败：FTP 连接不可用 {}", objectKey); return; }
            client.deleteFile(absolute(objectKey));
        } catch (Exception ex) {
            log.error("清理私有文件失败 {}", objectKey, ex);
        } finally { if (client != null) disconnect(client); }
    }
}
