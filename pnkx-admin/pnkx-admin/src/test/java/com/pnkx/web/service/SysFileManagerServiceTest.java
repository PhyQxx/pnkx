package com.pnkx.web.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件管理器安全校验单元测试
 * <p>
 * 覆盖常见路径穿越（Path Traversal）攻击向量，确保所有用户输入路径
 * 经 resolveSafePath 解析后无法逃逸出 BASE_DIR。
 *
 * @author phy
 */
class SysFileManagerServiceTest {

    private static final Path BASE_DIR = Paths.get("/vol2/1000/我的文档/obsidian/").normalize();

    @Test
    void 合法相对路径应解析到基目录内() {
        Path result = SysFileManagerService.resolveSafePath("JXD/笔记/test.md");
        assertNotNull(result);
        assertTrue(result.startsWith(BASE_DIR));
        assertEquals(BASE_DIR.resolve("JXD/笔记/test.md").normalize(), result);
    }

    @Test
    void 空路径应返回基目录本身() {
        assertEquals(BASE_DIR, SysFileManagerService.resolveSafePath(null));
        assertEquals(BASE_DIR, SysFileManagerService.resolveSafePath(""));
    }

    @Test
    void 目录穿越应被拒绝() {
        assertNull(SysFileManagerService.resolveSafePath("../etc/passwd"));
        assertNull(SysFileManagerService.resolveSafePath("a/../../b"));
        assertNull(SysFileManagerService.resolveSafePath("a/b/../../../c"));
        assertNull(SysFileManagerService.resolveSafePath(".."));
        assertNull(SysFileManagerService.resolveSafePath("./.."));
    }

    @Test
    void 绝对路径应被拒绝() {
        assertNull(SysFileManagerService.resolveSafePath("/etc/passwd"));
        assertNull(SysFileManagerService.resolveSafePath("/vol2/其他目录/secret"));
    }

    @Test
    void null字节注入应被拒绝() {
        assertNull(SysFileManagerService.resolveSafePath("a\0/../.."));
        assertNull(SysFileManagerService.resolveSafePath("\0"));
    }

    @Test
    void 内部合法的点多级目录应放行() {
        // 目录名本身含点是合法的（如 .obsidian 配置目录），只要不上跳
        assertNotNull(SysFileManagerService.resolveSafePath(".obsidian/app.json"));
        assertNotNull(SysFileManagerService.resolveSafePath("a/./b/c.md"));
    }

    @Test
    void isSafeName应拒绝携带路径特征的文件名() {
        assertFalse(SysFileManagerService.isSafeName(null));
        assertFalse(SysFileManagerService.isSafeName(""));
        assertFalse(SysFileManagerService.isSafeName("a/b.md"));
        assertFalse(SysFileManagerService.isSafeName("a\\b.md"));
        assertFalse(SysFileManagerService.isSafeName(".."));
        assertFalse(SysFileManagerService.isSafeName("a..b"));
        assertFalse(SysFileManagerService.isSafeName("a\0b"));
    }

    @Test
    void isSafeName应放行正常文件名() {
        assertTrue(SysFileManagerService.isSafeName("日记.md"));
        assertTrue(SysFileManagerService.isSafeName("note-2026.txt"));
        assertTrue(SysFileManagerService.isSafeName("图片.png"));
    }
}
