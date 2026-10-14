package com.pnkx.life.service.workspace.storage;
import com.pnkx.common.config.PnkxConfig;
import com.pnkx.common.exception.ServiceException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 本地磁盘后端（默认，开发环境零依赖）。 */
@Component
@ConditionalOnProperty(name = "pnkx.private-files.storage", havingValue = "local", matchIfMissing = true)
public class LocalPrivateFileStorage implements PrivateFileStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalPrivateFileStorage.class);
    @Value("${pnkx.private-files.root:${user.home}/.pnkx-private-files}") private String root;
    @Override public String type() { return "local"; }
    @Override @PostConstruct public void validateAtStartup() throws IOException {
        Path dir = path();
        log.info("私有文件存储使用本地目录 {}", dir);
    }
    private Path path() throws IOException {
        Path dir = Path.of(root).toAbsolutePath().normalize();
        String profile = PnkxConfig.getProfile();
        if (profile != null && dir.startsWith(Path.of(profile).toAbsolutePath().normalize()))
            throw new ServiceException("私有文件目录不能位于公开上传目录内");
        Files.createDirectories(dir);
        if (Files.isSymbolicLink(dir) || !dir.toRealPath().equals(dir))
            throw new ServiceException("私有文件目录不能包含符号链接");
        return dir;
    }
    @Override public void store(String objectKey, Path staged) throws IOException {
        Path target = path().resolve(Objects.requireNonNull(objectKey));
        try {
            Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.copy(staged, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    @Override public InputStream open(String objectKey) throws IOException {
        Path file = path().resolve(Objects.requireNonNull(objectKey));
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new ServiceException("文件暂时不可用");
        return Files.newInputStream(file);
    }
    @Override public void deleteQuietly(String objectKey) {
        try { Files.deleteIfExists(Path.of(root).toAbsolutePath().normalize().resolve(objectKey)); }
        catch (Exception ex) { log.error("清理私有文件失败 {}", objectKey, ex); }
    }
}
