package com.pnkx.life.service.note;

import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxNoteFolder;
import com.pnkx.life.mapper.PxNoteFolderMapper;
import com.pnkx.life.mapper.PxNoteMapper;
import com.pnkx.life.service.NoteAccessService;
import com.pnkx.life.service.NoteRevisionConflictException;
import com.pnkx.life.service.impl.PxNoteFolderServiceImpl;
import com.pnkx.life.service.impl.PxNoteServiceImpl;
import com.pnkx.life.service.impl.PxOfflineSyncServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.api.Assumptions;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/** 真实 Mapper SQL + 服务测试；H2 MySQL 模式不替代生产 MySQL/双设备验收。 */
class NoteIsolationIntegrationTest {
    private SqlSession session;
    private Connection mysqlAdmin;
    private String mysqlSchema;
    private DataSource dataSource;
    private PxNoteMapper notes;
    private PxNoteFolderMapper folders;
    private PxNoteServiceImpl noteService;
    private PxNoteFolderServiceImpl folderService;
    private PxOfflineSyncServiceImpl offlineService;

    @BeforeEach
    void setup() throws Exception {
        String mysqlUrl = System.getProperty("note.test.mysqlUrl");
        UnpooledDataSource ds;
        if (mysqlUrl != null) {
            if (!mysqlUrl.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/")) {
                throw new IllegalArgumentException("Only the isolated loopback MySQL fixture is supported");
            }
            mysqlSchema = "pnkx_note_test_" + UUID.randomUUID().toString().replace("-", "");
            mysqlAdmin = DriverManager.getConnection(mysqlUrl, "root", "");
            try (Statement stmt = mysqlAdmin.createStatement()) {
                stmt.execute("CREATE DATABASE " + mysqlSchema);
            }
            ds = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", mysqlUrl + mysqlSchema, "root", "");
        } else {
            ds = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID()
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        }
        dataSource = ds;
        Configuration config = new Configuration(new Environment("test", new SpringManagedTransactionFactory(), ds));
        config.getTypeAliasRegistry().registerAlias("PxNote", PxNote.class);
        config.getTypeAliasRegistry().registerAlias("PxNoteFolder", PxNoteFolder.class);
        for (String file : List.of("PxNoteMapper.xml", "PxNoteFolderMapper.xml")) {
            try (InputStream in = getClass().getResourceAsStream("/mapper/" + file)) {
                new XMLMapperBuilder(in, config, file, config.getSqlFragments()).parse();
            }
        }
        var factory = new SqlSessionFactoryBuilder().build(config);
        session = factory.openSession(true);
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("""
                    CREATE TABLE px_note (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255), content LONGTEXT,
                      rich_text LONGTEXT, folder BIGINT DEFAULT 0, `order` INT, del_flag INT DEFAULT 0,
                      version VARCHAR(20), client_uuid VARCHAR(36) UNIQUE, create_by VARCHAR(64),
                      create_time TIMESTAMP, update_by VARCHAR(64), update_time TIMESTAMP, remark VARCHAR(255))
                    """);
            statement.execute("""
                    CREATE TABLE px_note_folder (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), parent_id BIGINT DEFAULT 0,
                      password VARCHAR(255), `order` INT, del_flag INT DEFAULT 0, version VARCHAR(20),
                      create_by VARCHAR(64), create_time TIMESTAMP, update_by VARCHAR(64),
                      update_time TIMESTAMP, remark VARCHAR(255))
                    """);
            statement.execute("""
                    INSERT INTO px_note_folder(id,name,parent_id,create_by,create_time) VALUES
                      (10,'A folder',0,'101',TIMESTAMP '2020-01-01 00:00:00'),
                      (11,'A child',10,'101',TIMESTAMP '2020-01-01 00:00:00'),
                      (20,'B folder',0,'202',TIMESTAMP '2020-01-01 00:00:00'),
                      (21,'B malformed child',10,'202',TIMESTAMP '2020-01-01 00:00:00')
                    """);
            statement.execute("""
                    INSERT INTO px_note(id,title,content,folder,create_by,create_time,update_time,client_uuid) VALUES
                      (1,'A note','A content',10,'101',TIMESTAMP '2020-01-01 00:00:00',TIMESTAMP '2020-01-01 00:00:00','a-uuid'),
                      (2,'B note','B secret',20,'202',TIMESTAMP '2020-01-01 00:00:00',TIMESTAMP '2020-01-01 00:00:00','b-uuid'),
                      (3,'A child note','A content',11,'101',TIMESTAMP '2020-01-01 00:00:00',TIMESTAMP '2020-01-01 00:00:00',NULL),
                      (4,'B in A folder','B secret',10,'202',TIMESTAMP '2020-01-01 00:00:00',TIMESTAMP '2020-01-01 00:00:00',NULL),
                      (5,'B descendant note','B secret',21,'202',TIMESTAMP '2020-01-01 00:00:00',TIMESTAMP '2020-01-01 00:00:00',NULL)
                    """);
        }
        // Execute the actual deployment migration against pre-existing rows.
        var migration = java.nio.file.Path.of(System.getProperty("basedir"),
                "../pnkx-admin/src/main/resources/db/migration/V1.6.4__note_revision.sql");
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute(java.nio.file.Files.readString(migration));
        }
        var template = new SqlSessionTemplate(factory);
        notes = template.getMapper(PxNoteMapper.class);
        folders = template.getMapper(PxNoteFolderMapper.class);
        NoteAccessService access = new NoteAccessService();
        inject(access, "folderMapper", folders);
        noteService = new PxNoteServiceImpl();
        inject(noteService, "pxNoteMapper", notes);
        inject(noteService, "noteAccessService", access);
        folderService = new PxNoteFolderServiceImpl();
        inject(folderService, "pxNoteFolderMapper", folders);
        inject(folderService, "noteService", noteService);
        inject(folderService, "noteAccessService", access);
        offlineService = new PxOfflineSyncServiceImpl();
        inject(offlineService, "noteMapper", notes);
        inject(offlineService, "noteService", noteService);
        String historyMigration = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),
                "../pnkx-admin/src/main/resources/db/migration/V1.6.7__note_history.sql"));
        try (Statement stmt = session.getConnection().createStatement()) {
            for (String sql : historyMigration.split(";")) if (!sql.isBlank()) stmt.execute(sql);
        }
        String feedMigration=java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),"../pnkx-admin/src/main/resources/db/migration/V1.7.2__note_change_feed.sql"));
        try(Statement stmt=session.getConnection().createStatement()){for(String sql:feedMigration.split(";"))if(!sql.isBlank())stmt.execute(sql);}
        login(101);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        if (session != null) session.close();
        if (mysqlAdmin != null) {
            try (Connection admin = mysqlAdmin; Statement stmt = mysqlAdmin.createStatement()) {
                stmt.execute("DROP DATABASE " + mysqlSchema);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        }
    }

    @Test
    void changeFeedTracksDeletionRestoreAndRejectsForeignCursor() throws Exception {
        var feed=new com.pnkx.life.service.workspace.NoteChangeFeedService();inject(feed,"mapper",notes);
        var initial=feed.page("0");assertEquals("n2:101:2",initial.get("nextCursor"));
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        PxNote edit=noteService.selectPxNoteById(1L);edit.setContent("updated");
        tx.execute(status->noteService.updatePxNote(edit));tx.execute(status->noteService.deletePxNoteById(1L));
        var page=feed.page(initial.get("nextCursor").toString());
        var changes=(java.util.List<?>)page.get("items");assertEquals(2,changes.size());
        assertEquals(1,((PxNote)changes.getFirst()).getDelFlag());assertEquals("n2:101:4",page.get("nextCursor"));
        assertEquals(true,feed.page("n2:202:4").get("resetRequired"));
    }

    @Test
    void mysqlFeedSequenceFollowsCommitOrderInsteadOfAutoIncrementAllocation() throws Exception {
        Assumptions.assumeTrue(mysqlAdmin!=null);
        var feed=new com.pnkx.life.service.workspace.NoteChangeFeedService();inject(feed,"mapper",notes);
        var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(()->{login(101);new TransactionTemplate(new DataSourceTransactionManager(dataSource)).executeWithoutResult(status->{
                PxNote note=noteService.selectPxNoteById(1L);note.setContent("first");noteService.updatePxNote(note);entered.countDown();
                try{if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("timeout");}catch(InterruptedException ex){throw new RuntimeException(ex);}
            });return true;});
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            var second=executor.submit(()->{login(101);return new TransactionTemplate(new DataSourceTransactionManager(dataSource)).execute(status->{PxNote note=noteService.selectPxNoteById(3L);note.setContent("second");return noteService.updatePxNote(note);});});
            assertEquals("n2:101:2",feed.page("0").get("nextCursor"));
            release.countDown();first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS);
            var committed=feed.page("n2:101:2");assertEquals(2,((java.util.List<?>)committed.get("items")).size());assertEquals("n2:101:4",committed.get("nextCursor"));
        } finally {release.countDown();executor.shutdownNow();}
    }

    @Test
    void listsIgnoreForgedOwnerAndScopeAll() {
        PxNote query = new PxNote();
        query.setCreateBy("202");
        query.getParams().put("scopeAll", true);
        List<PxNoteFolder> result = noteService.selectPxNoteList(query);
        assertEquals(Set.of(1L, 3L), result.stream().map(PxNote::getId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void directReadsAndWritesRejectOtherUser() {
        assertThrows(ServiceException.class, () -> noteService.selectPxNoteById(2L));
        PxNote forged = new PxNote();
        forged.setId(2L);
        forged.setTitle("overwrite");
        assertThrows(ServiceException.class, () -> noteService.updatePxNote(forged));
        assertEquals("B note", notes.selectPxNoteById(2L, "202").getTitle());
    }

    @Test
    void staleAndMissingRevisionsCannotOverwriteContent() {
        PxNote first = noteService.selectPxNoteById(1L);
        PxNote stale = noteService.selectPxNoteById(1L);
        assertEquals(1L, first.getRevision());
        first.setContent("new content");
        assertEquals(2L, noteService.updatePxNote(first).getRevision());
        stale.setContent("stale content");
        var conflict = assertThrows(NoteRevisionConflictException.class, () -> noteService.updatePxNote(stale));
        assertEquals("NOTE_REVISION_CONFLICT", conflict.getErrorCode());
        assertEquals(2L, conflict.getCurrent().getRevision());
        assertEquals("new content", conflict.getCurrent().getContent());
        stale.setRevision(null);
        assertEquals("NOTE_REVISION_REQUIRED", assertThrows(NoteRevisionConflictException.class,
                () -> noteService.updatePxNote(stale)).getErrorCode());
        assertEquals("new content", noteService.selectPxNoteById(1L).getContent());
    }

    @Test
    void insertForcesInitialRevisionAndMovePreservesFullContent() {
        PxNote input = new PxNote();
        input.setTitle("new");
        input.setContent("complete body");
        input.setRevision(99L);
        PxNote created = noteService.insertPxNote(input);
        assertEquals(1L, created.getRevision());
        PxNote move = new PxNote();
        move.setId(created.getId());
        move.setRevision(1L);
        move.setFolder(10L);
        PxNote moved = noteService.updatePxNote(move);
        assertEquals("complete body", moved.getContent());
        assertEquals(2L, moved.getRevision());
        noteService.deletePxNoteById(created.getId());
        assertTrue(notes.selectIncremental("101", "2020-01-01 00:00:00", 0, 100).stream()
                .anyMatch(n -> n.getId().equals(created.getId()) && n.getRevision() == 3L));
    }

    @Test
    void concurrentWritersWithSameRevisionOnlyOneCommits() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var one = pool.submit(() -> concurrentNoteSave(barrier, "first"));
            var two = pool.submit(() -> concurrentNoteSave(barrier, "second"));
            assertNotEquals(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
        }
        assertEquals(2L, noteService.selectPxNoteById(1L).getRevision());
    }

    private boolean concurrentNoteSave(CyclicBarrier barrier, String content) throws Exception {
        login(101);
        try {
            return new TransactionTemplate(new DataSourceTransactionManager(dataSource)).execute(status -> {
                PxNote draft = noteService.selectPxNoteById(1L);
                draft.setContent(content);
                try { barrier.await(10, TimeUnit.SECONDS); }
                catch (Exception ex) { throw new RuntimeException(ex); }
                noteService.updatePxNote(draft);
                return true;
            });
        } catch (NoteRevisionConflictException conflict) {
            assertEquals(2L, conflict.getCurrent().getRevision());
            assertNotEquals(content, conflict.getCurrent().getContent());
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void updatesCannotTransferOwnershipOrSoftDelete() {
        PxNote edited = noteService.selectPxNoteById(1L);
        edited.setTitle("new title");
        edited.setCreateBy("202");
        edited.setCreateTime(new Date());
        edited.setDelFlag(1);
        noteService.updatePxNote(edited);
        PxNote saved = noteService.selectPxNoteById(1L);
        assertEquals("101", saved.getCreateBy());
        assertEquals(0, saved.getDelFlag());
        assertEquals("new title", saved.getTitle());
        assertTrue(saved.getCreateTime().before(new Date(1600000000000L)));
    }

    @Test
    void mapperItselfRejectsCrossUserUpdatesAndDeletes() {
        PxNote edit = new PxNote();
        edit.setId(2L);
        edit.setUpdateBy("101");
        edit.setUpdateTime(new Date());
        edit.setTitle("forged");
        assertEquals(0, notes.updatePxNote(edit));
        assertEquals(0, notes.deletePxNoteById(2L, "101"));
        assertNull(notes.selectByClientUuid("b-uuid", "101"));
    }

    @Test
    void mixedOwnerBatchDeleteDoesNotDeleteAnyItem() {
        assertThrows(ServiceException.class, () -> noteService.deletePxNoteByIds(new Long[]{1L, 2L}));
        assertNotNull(noteService.selectPxNoteById(1L));
        assertNotNull(notes.selectPxNoteById(2L, "202"));
    }

    @Test
    void deletionIsVisibleToIncrementalSyncAndReplayIsNoop() {
        assertEquals(1, noteService.deletePxNoteByIds(new Long[]{1L, 1L}));
        List<PxNote> changes = offlineService.selectNoteIncremental("101", "2021-01-01 00:00:00", 0, 50);
        assertEquals(1, changes.size());
        assertEquals(1L, changes.get(0).getId());
        assertEquals(1, changes.get(0).getDelFlag());
        Date deletedTime = changes.get(0).getUpdateTime();
        assertEquals(0, noteService.deletePxNoteById(1L));
        assertThrows(ServiceException.class, () -> noteService.selectPxNoteById(1L));
        assertEquals(deletedTime, offlineService.selectNoteIncremental("101", "2021-01-01 00:00:00", 0, 50).get(0).getUpdateTime());
    }

    @Test
    void incrementalAndAiSearchCannotChooseAnotherUser() {
        assertThrows(ServiceException.class, () -> offlineService.selectNoteIncremental("202", "2020-01-01", 0, 50));
        assertThrows(ServiceException.class, () -> noteService.searchNotes("202", "secret", null, 5));
        assertTrue(noteService.searchNotes("101", "secret", null, 5).isEmpty());
    }

    @Test
    void deletedNotesCannotBeUpdatedOrResurrected() {
        noteService.deletePxNoteById(1L);
        PxNote edit = new PxNote();
        edit.setId(1L);
        edit.setDelFlag(0);
        assertThrows(ServiceException.class, () -> noteService.updatePxNote(edit));
    }

    @Test
    void noteCannotBeCreatedOrMovedIntoOthersFolder() {
        PxNote input = new PxNote();
        input.setTitle("new");
        input.setFolder(20L);
        assertThrows(ServiceException.class, () -> noteService.insertPxNote(input));
        input.setId(1L);
        assertThrows(ServiceException.class, () -> noteService.updatePxNote(input));
    }

    @Test
    void folderListingIncludesOnlyOwnedNotesAndCountsOwnedDescendants() {
        PxNoteFolder query = new PxNoteFolder();
        query.setParentId(10L);
        query.setCreateBy("202");
        query.getParams().put("scopeAll", true);
        List<PxNoteFolder> rows = folderService.selectPxNoteFolderList(query);
        assertEquals(Set.of(1L, 11L), rows.stream().map(PxNoteFolder::getId).collect(java.util.stream.Collectors.toSet()));
        assertThrows(ServiceException.class, () -> folderService.selectPxNoteFolderById(20L));
    }

    @Test
    void recursiveFolderCountsAreScopedAtEveryLevelOnMysql() {
        // H2 的带参数递归 CTE 在派生聚合表中与 MySQL 行为不同；不能当作生产验收。
        Assumptions.assumeTrue(mysqlAdmin != null, "Run scripts/test-note-isolation.sh for the real MySQL CTE test");
        assertEquals(2, folderService.selectPxNoteFolderById(10L).getNoteCount());
        PxNoteFolder query = new PxNoteFolder();
        query.setParentId(0L);
        assertEquals(2, folderService.selectPxNoteFolderList(query).get(0).getNoteCount());
    }

    @Test
    void folderTreeNeverIncludesAnotherUsersNodes() {
        var tree = folderService.selectPxNoteFolderTreeList(new PxNoteFolder());
        assertEquals(1, tree.size());
        assertEquals(10L, tree.get(0).getId());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(11L, tree.get(0).getChildren().get(0).getId());
    }

    @Test
    void folderMovesRejectCyclesAndForeignParents() {
        PxNoteFolder move = new PxNoteFolder();
        move.setId(10L);
        move.setParentId(11L);
        assertThrows(ServiceException.class, () -> folderService.updatePxNoteFolder(move));
        move.setParentId(20L);
        assertThrows(ServiceException.class, () -> folderService.updatePxNoteFolder(move));
        move.setParentId(10L);
        assertThrows(ServiceException.class, () -> folderService.updatePxNoteFolder(move));
        assertEquals(0L, folderService.selectPxNoteFolderById(10L).getParentId());
    }

    @Test
    void validFolderMovePreservesOwnershipAndCreationTime() {
        PxNoteFolder folder = folderService.selectPxNoteFolderById(11L);
        Date createdAt = folder.getCreateTime();
        folder.setParentId(0L);
        folder.setCreateBy("202");
        folder.setCreateTime(new Date());
        folder.setDelFlag(1);
        PxNoteFolder saved = folderService.updatePxNoteFolder(folder);
        assertEquals("101", saved.getCreateBy());
        assertEquals(createdAt, saved.getCreateTime());
        assertEquals(0, saved.getDelFlag());
        assertEquals(0L, saved.getParentId());
    }

    @Test
    void concurrentFolderMovesCannotFormCycleOnMysql() throws Exception {
        Assumptions.assumeTrue(mysqlAdmin != null, "MySQL row-lock regression requires scripts/test-note-isolation.sh");
        try (Statement stmt = session.getConnection().createStatement()) {
            stmt.execute("INSERT INTO px_note_folder(id,name,parent_id,create_by) VALUES(30,'Another root',0,'101')");
        }
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> moveInTransaction(10L, 30L, barrier));
            var second = executor.submit(() -> moveInTransaction(30L, 10L, barrier));
            boolean firstApplied = first.get(15, TimeUnit.SECONDS);
            boolean secondApplied = second.get(15, TimeUnit.SECONDS);
            assertNotEquals(firstApplied, secondApplied, "Only one conflicting move may commit");
        }
        assertFalse(folderService.selectPxNoteFolderById(10L).getParentId().equals(30L)
                && folderService.selectPxNoteFolderById(30L).getParentId().equals(10L));
    }

    private boolean moveInTransaction(Long id, Long parent, CyclicBarrier barrier) throws Exception {
        login(101);
        try {
            barrier.await(5, TimeUnit.SECONDS);
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            transaction.setTimeout(10);
            transaction.execute(status -> {
                PxNoteFolder move = new PxNoteFolder();
                move.setId(id);
                move.setParentId(parent);
                folderService.updatePxNoteFolder(move);
                return null;
            });
            return true;
        } catch (ServiceException expectedConflict) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void folderBatchDeleteRejectsMixedOwnership() {
        assertThrows(ServiceException.class, () -> folderService.deletePxNoteFolderByIds(new Long[]{10L, 20L}));
        assertNotNull(folderService.selectPxNoteFolderById(10L));
        assertEquals(0, folders.deletePxNoteFolderById(20L, "101"));
    }

    @Test
    void offlineCreateUsesOwnershipAndFolderValidation() {
        assertThrows(ServiceException.class, () -> offlineService.insertNoteIdempotent(Map.of("folder", 20L), "new-uuid"));
        assertThrows(ServiceException.class, () -> offlineService.insertNoteIdempotent(Map.of("title", "forged"), "b-uuid"));
        assertEquals(1L, offlineService.insertNoteIdempotent(Map.of("title", "replay"), "a-uuid"));
        assertEquals("A note", noteService.selectPxNoteById(1L).getTitle());
    }

    @Test
    void replayOfDeletedOfflineCreationReturnsTombstoneWithoutRestoring() {
        noteService.deletePxNoteById(1L);
        PxNote replay = offlineService.insertNoteIdempotentResult(Map.of("title", "retry"), "a-uuid");
        assertEquals(1L, replay.getId());
        assertEquals(1, replay.getDelFlag());
        assertEquals(2L, replay.getRevision());
        assertThrows(ServiceException.class, () -> noteService.selectPxNoteById(1L));
    }

    @Test
    void offlineCreateSetsOwnerAndRepeatsOnlyOwnIdentifier() {
        Long id = offlineService.insertNoteIdempotent(Map.of("title", "offline", "createBy", "202"), "new-uuid");
        assertEquals("101", noteService.selectPxNoteById(id).getCreateBy());
        assertEquals(id, offlineService.insertNoteIdempotent(Map.of("title", "again"), "new-uuid"));
    }

    @Test
    void anonymousAccessFailsClosed() {
        SecurityContextHolder.clearContext();
        assertThrows(ServiceException.class, () -> noteService.selectPxNoteList(new PxNote()));
        assertThrows(ServiceException.class, () -> folderService.selectPxNoteFolderTreeList(new PxNoteFolder()));
        assertThrows(ServiceException.class, () -> noteService.deletePxNoteById(1L));
    }

    @Test
    void adminStillUsesPrivateNoteScope() {
        login(1);
        assertTrue(noteService.selectPxNoteList(new PxNote()).isEmpty());
        assertThrows(ServiceException.class, () -> noteService.selectPxNoteById(1L));
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void login(long id) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setUserName("test-" + id);
        LoginUser login = new LoginUser(user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }
}
