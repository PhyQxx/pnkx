package com.pnkx.life.service.workspace.storage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * 私有文件存储后端。objectKey 恒为 36 位 UUID 字符串，各后端自行决定物理位置；
 * 私有内容必须通过本接口的授权读取路径访问，不得暴露为公开静态资源 URL。
 */
public interface PrivateFileStorage {
    /** 后端标识：local / ftp */
    String type();
    /** 启动期校验配置（目录隔离、可达性），失败 fail-fast */
    void validateAtStartup() throws Exception;
    /** 将暂存文件落入存储（实现可移动或复制，完成后暂存文件可删除） */
    void store(String objectKey, Path staged) throws IOException;
    /** 打开对象读取流；调用方负责关闭 */
    InputStream open(String objectKey) throws IOException;
    /** 尽力删除（事务回滚清理），失败仅记录日志 */
    void deleteQuietly(String objectKey);
}
