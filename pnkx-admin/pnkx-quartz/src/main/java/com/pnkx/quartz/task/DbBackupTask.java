package com.pnkx.quartz.task;

import com.pnkx.common.config.PnkxConfig;
import com.pnkx.common.ftp.FtpTool;
import com.pnkx.common.utils.DateUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import jakarta.annotation.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * 数据库自动备份任务
 * <p>
 * 纯 JDBC 逻辑备份（不依赖 mysqldump，容器/裸机通用）：
 * 遍历当前库全部表导出建表语句 + INSERT 数据 → gzip → 上传 FTP，
 * 保留最近 30 天（按文件名日期判断）。
 * <p>
 * 调度：sys_job 配置 invoke_target = dbBackupTask.execute()（V1.4.6 已注册每日 03:00）。
 * 个人博客数据量级（<50MB）下全量导出耗时可接受；数据量显著增长后建议换 mysqldump 物理方案。
 *
 * @author phy
 */
@Component("dbBackupTask")
public class DbBackupTask {

    private static final Logger log = LoggerFactory.getLogger(DbBackupTask.class);

    /**
     * 备份在 FTP 上的目录
     */
    private static final String FTP_BACKUP_DIR = "pnkx-backup";

    /**
     * 备份保留天数
     */
    private static final int RETENTION_DAYS = 30;

    /**
     * INSERT 批量大小
     */
    private static final int BATCH_SIZE = 500;

    @Resource
    private DataSource dataSource;

    @Resource
    private FtpTool ftpTool;

    /**
     * 执行备份（由 quartz 调度，也可在管理端手动触发）
     */
    public void execute() {
        String date = DateUtils.dateTimeNow("yyyy-MM-dd");
        String fileName = String.format("pnkx-backup-%s.sql.gz", date);
        File localFile = null;
        FTPClient ftpClient = null;
        try {
            File tempDir = new File(PnkxConfig.getUploadPath(), "backup");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            localFile = new File(tempDir, fileName);
            dumpDatabase(localFile);
            log.info("数据库逻辑备份完成：{}（{}）", fileName, humanSize(localFile.length()));

            ftpClient = ftpTool.connectFtp();
            if (ftpClient == null) {
                log.error("备份上传失败：FTP 连接不可用，本地备份保留于 {}", localFile.getAbsolutePath());
                return;
            }
            uploadToFtp(ftpClient, localFile, fileName);
            cleanExpiredBackups(ftpClient);
            log.info("备份 {} 已上传 FTP 目录 {}/", fileName, FTP_BACKUP_DIR);
        } catch (Exception e) {
            log.error("数据库备份任务异常", e);
        } finally {
            ftpTool.closeFtpClient(ftpClient);
            // 上传成功后删除本地临时文件（失败时保留，便于人工兜底）
            if (localFile != null && localFile.exists()) {
                try {
                    Files.deleteIfExists(localFile.toPath());
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 遍历当前库全部表，导出 DDL + 数据到 gzip 文件
     */
    private void dumpDatabase(File outFile) throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' ORDER BY table_name")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            try (GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(outFile));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8), 1 << 16)) {
                writer.write("-- pnkx database backup\n");
                writer.write("-- created_at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                writer.write("SET NAMES utf8mb4;\n");
                writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
                long totalRows = 0;
                for (String table : tables) {
                    dumpTableStructure(stmt, writer, table);
                    totalRows += dumpTableData(stmt, writer, table);
                }
                writer.write("\nSET FOREIGN_KEY_CHECKS = 1;\n");
                log.info("备份统计：{} 张表，{} 行数据", tables.size(), totalRows);
            }
        }
    }

    private void dumpTableStructure(Statement stmt, BufferedWriter writer, String table) throws Exception {
        writer.write("-- ----------------------------\n");
        writer.write("-- Table structure for `" + table + "`\n");
        writer.write("-- ----------------------------\n");
        writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");
        try (ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            if (rs.next()) {
                writer.write(rs.getString(2) + ";\n\n");
            }
        }
    }

    /**
     * 流式导出表数据（INSERT 批量拼接），返回行数
     */
    private long dumpTableData(Statement stmt, BufferedWriter writer, String table) throws Exception {
        stmt.setFetchSize(Integer.MIN_VALUE);
        long rowCount = 0;
        StringBuilder batch = new StringBuilder();
        int inBatch = 0;
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                rowCount++;
                StringBuilder values = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        values.append(',');
                    }
                    values.append(sqlValue(rs, i));
                }
                batch.append("INSERT INTO `").append(table).append("` VALUES (").append(values).append(");\n");
                if (++inBatch >= BATCH_SIZE) {
                    writer.write(batch.toString());
                    batch.setLength(0);
                    inBatch = 0;
                }
            }
        }
        if (batch.length() > 0) {
            writer.write(batch.toString());
        }
        writer.write("\n");
        return rowCount;
    }

    /**
     * JDBC 值 → SQL 字面量（含 null/二进制/时间类型的转义）
     */
    private String sqlValue(ResultSet rs, int columnIndex) throws Exception {
        Object value = rs.getObject(columnIndex);
        if (value == null) {
            return "NULL";
        }
        if (value instanceof byte[] bytes) {
            if (bytes.length == 0) {
                return "''";
            }
            StringBuilder hex = new StringBuilder("0x");
            for (byte b : bytes) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        }
        if (value instanceof java.sql.Timestamp || value instanceof java.sql.Date || value instanceof java.sql.Time) {
            return "'" + value.toString().replace("'", "''") + "'";
        }
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return "'" + value.toString().replace("\\", "\\\\").replace("'", "''")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\0", "\\0") + "'";
    }

    /**
     * 上传备份文件到 FTP 指定目录（目录不存在则创建）
     */
    private void uploadToFtp(FTPClient ftpClient, File localFile, String fileName) throws Exception {
        ftpClient.makeDirectory(FTP_BACKUP_DIR);
        ftpClient.changeWorkingDirectory(FTP_BACKUP_DIR);
        try (FileInputStream in = new FileInputStream(localFile)) {
            if (!ftpClient.storeFile(fileName, in)) {
                throw new IllegalStateException("FTP 存储备份文件失败: " + fileName);
            }
        }
    }

    /**
     * 清理超过保留天数的备份（按文件名中的日期判断）
     */
    private void cleanExpiredBackups(FTPClient ftpClient) {
        try {
            ftpClient.changeWorkingDirectory(FTP_BACKUP_DIR);
            FTPFile[] files = ftpClient.listFiles();
            LocalDate cutoff = LocalDate.now().minusDays(RETENTION_DAYS);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            int removed = 0;
            for (FTPFile file : files) {
                String name = file.getName();
                if (!name.startsWith("pnkx-backup-") || !name.endsWith(".sql.gz")) {
                    continue;
                }
                try {
                    String dateStr = name.substring("pnkx-backup-".length(), name.length() - ".sql.gz".length());
                    if (fmt.parse(dateStr).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(cutoff)) {
                        if (ftpClient.deleteFile(name)) {
                            removed++;
                        }
                    }
                } catch (Exception parseOrDeleteFail) {
                    // 文件名不带日期或删除失败：跳过该文件
                }
            }
            if (removed > 0) {
                log.info("已清理 {} 个过期备份（>{} 天）", removed, RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.warn("清理过期备份失败（不影响备份本体）", e);
        }
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        }
        return String.format("%.1fMB", bytes / 1024.0 / 1024.0);
    }
}
