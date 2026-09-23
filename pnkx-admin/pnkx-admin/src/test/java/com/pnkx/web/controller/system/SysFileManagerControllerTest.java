package com.pnkx.web.controller.system;

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
class SysFileManagerControllerTest {

    private static final Path BASE_DIR = Paths.get("/vol2/1000/我的文档/obsidian/").normalize();

    @Test
    void 合法相对路径应解析到基目录内() {
        Path result = SysFileManagerController.resolveSafePath("JXD/笔记/test.md");
        assertNotNull(result);
        assertTrue(result.startsWith(BASE_DIR));
        assertEquals(BASE_DIR.resolve("JXD/笔记/test.md").normalize(), result);
    }

    @Test
    void 空路径应返回基目录本身() {
        assertEquals(BASE_DIR, SysFileManagerController.resolveSafePath(null));
        assertEquals(BASE_DIR, SysFileManagerController.resolveSafePath(""));
    }

    @Test
    void 目录穿越应被拒绝() {
        assertNull(SysFileManagerController.resolveSafePath("../etc/passwd"));
        assertNull(SysFileManagerController.resolveSafePath("a/../../b"));
        assertNull(SysFileManagerController.resolveSafePath("a/b/../../../c"));
        assertNull(SysFileManagerController.resolveSafePath(".."));
        assertNull(SysFileManagerController.resolveSafePath("./.."));
    }

    @Test
    void 绝对路径应被拒绝() {
        assertNull(SysFileManagerController.resolveSafePath("/etc/passwd"));
        assertNull(SysFileManagerController.resolveSafePath("/vol2/其他目录/secret"));
    }

    @Test
    void null字节注入应被拒绝() {
        assertNull(SysFileManagerController.resolveSafePath("a\0/../.."));
        assertNull(SysFileManagerController.resolveSafePath("\0"));
    }

    @Test
    void 内部合法的点多级目录应放行() {
        // 目录名本身含点是合法的（如 .obsidian 配置目录），只要不上跳
        assertNotNull(SysFileManagerController.resolveSafePath(".obsidian/app.json"));
        assertNotNull(SysFileManagerController.resolveSafePath("a/./b/c.md"));
    }

    @Test
    void isSafeName应拒绝携带路径特征的文件名() {
        assertFalse(SysFileManagerController.isSafeName(null));
        assertFalse(SysFileManagerController.isSafeName(""));
        assertFalse(SysFileManagerController.isSafeName("a/b.md"));
        assertFalse(SysFileManagerController.isSafeName("a\\b.md"));
        assertFalse(SysFileManagerController.isSafeName(".."));
        assertFalse(SysFileManagerController.isSafeName("a..b"));
        assertFalse(SysFileManagerController.isSafeName("a\0b"));
    }

    @Test
    void isSafeName应放行正常文件名() {
        assertTrue(SysFileManagerController.isSafeName("日记.md"));
        assertTrue(SysFileManagerController.isSafeName("note-2026.txt"));
        assertTrue(SysFileManagerController.isSafeName("图片.png"));
    }
}
