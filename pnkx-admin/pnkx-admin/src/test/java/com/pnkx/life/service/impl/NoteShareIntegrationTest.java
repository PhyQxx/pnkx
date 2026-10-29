package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.mapper.PxNoteMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="PNKX_TEST_MYSQL_URL", matches="jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test_note_share.*")
class NoteShareIntegrationTest {
    JdbcTemplate db;
    NoteShareService shares;
    @BeforeEach void setup() throws Exception {
        db = new JdbcTemplate(new DriverManagerDataSource(System.getenv("PNKX_TEST_MYSQL_URL"),"root","pnkx_local_test_only"));
        for (String t : List.of("px_note_share","px_note","px_note_folder","sys_user")) db.execute("drop table if exists " + t);
        db.execute("create table sys_user(user_id bigint primary key,user_name varchar(64),status char(1),del_flag char(1))");
        db.execute("create table px_note(id bigint primary key,title varchar(255),content text,rich_text text,folder bigint,create_by varchar(64),del_flag int default 0,update_time datetime)");
        db.execute("create table px_note_folder(id bigint primary key,name varchar(255),parent_id bigint,create_by varchar(64),del_flag int default 0)");
        db.execute(Files.readString(Path.of("src/main/resources/db/migration/V1.8.4__note_sharing.sql")));
        db.update("insert into sys_user values (1,'owner','0','0'),(2,'reader','0','0'),(3,'other','0','0')");
        db.update("insert into px_note_folder values(10,'Shared',0,'1',0),(11,'Child',10,'1',0),(12,'Private',0,'1',0)");
        db.update("insert into px_note values(20,'Nested','body','<p>body</p>',11,'1',0,now()),(21,'Private','secret','',12,'1',0,now()),(22,'Foreign','secret','',11,'3',0,now())");
        shares = new NoteShareService(db);
    }
    Map<String,Object> create(String username,String permission) {
        return shares.create("folder",10L,"1",username,permission,7).get(0);
    }
    @Test void publicLinkLifecycleAndSubtreeBoundary() {
        var grant = create(null,"read"); String token = grant.get("token").toString();
        assertEquals(64,token.length());
        var root = shares.browse(null,token,null,null,null);
        assertEquals(10L,root.get("id"));
        var child = shares.browse(null,token,null,"folder",11L);
        assertEquals(1,((List<?>)child.get("children")).size());
        assertEquals("body",shares.browse(null,token,null,"note",20L).get("content"));
        assertThrows(ServiceException.class,()->shares.browse(null,token,null,"note",21L));
        assertThrows(ServiceException.class,()->shares.browse(null,token,null,"note",22L));
        shares.revoke(((Number)grant.get("id")).longValue(),"1");
        assertThrows(ServiceException.class,()->shares.browse(null,token,null,null,null));
    }
    @Test void recipientGrantUpgradeDowngradeAndRevocation() {
        var read = create("reader","read");
        assertEquals(1,shares.received("2").size()); assertTrue(shares.received("3").isEmpty());
        shares.requireAccess("note",20L,"2",false);
        assertThrows(ServiceException.class,()->shares.requireAccess("note",20L,"2",true));
        assertThrows(ServiceException.class,()->shares.browse(((Number)read.get("id")).longValue(),null,"3",null,null));
        create("reader","edit"); shares.requireAccess("note",20L,"2",true);
        assertEquals(1,shares.received("2").size());
        var downgraded = create("reader","read");
        assertThrows(ServiceException.class,()->shares.requireAccess("note",20L,"2",true));
        shares.revoke(((Number)downgraded.get("id")).longValue(),"1");
        assertTrue(shares.received("2").isEmpty());
        assertThrows(ServiceException.class,()->shares.requireAccess("note",20L,"2",false));
    }
    @Test void expirationDeletionAndMoveAreEnforced() {
        var grant = create("reader","edit");
        db.update("update px_note_folder set parent_id=12 where id=11");
        assertThrows(ServiceException.class,()->shares.requireAccess("note",20L,"2",false));
        db.update("update px_note_folder set parent_id=10 where id=11");
        shares.requireAccess("note",20L,"2",true);
        db.update("update px_note_share set expires_at=date_sub(now(),interval 1 second) where id=?",grant.get("id"));
        assertThrows(ServiceException.class,()->shares.requireAccess("note",20L,"2",false));
        var link = create(null,"read");
        db.update("update px_note_folder set del_flag=1 where id=10");
        assertThrows(ServiceException.class,()->shares.browse(null,link.get("token").toString(),null,"note",20L));
    }
    @Test void editApiStripsOwnershipMoveAndDeleteFields() {
        create("reader","edit");
        var mapper = mock(PxNoteMapper.class);
        var old = new PxNote(); old.setId(20L); old.setCreateBy("1"); old.setFolder(11L);
        when(mapper.selectPxNoteById(20L)).thenReturn(old);
        var service = new PxNoteServiceImpl();
        ReflectionTestUtils.setField(service,"sharing",shares);
        ReflectionTestUtils.setField(service,"pxNoteMapper",mapper);
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn("2");
            var edit = new PxNote(); edit.setId(20L); edit.setTitle("Edited"); edit.setFolder(12L); edit.setCreateBy("2"); edit.setDelFlag(1);
            service.updatePxNote(edit);
            var captured = org.mockito.ArgumentCaptor.forClass(PxNote.class);
            verify(mapper).updatePxNote(captured.capture());
            assertEquals("Edited",captured.getValue().getTitle());
            assertNull(captured.getValue().getFolder()); assertNull(captured.getValue().getCreateBy()); assertNull(captured.getValue().getDelFlag());
            assertThrows(ServiceException.class,()->service.deletePxNoteById(20L));
            create("reader","read");
            assertThrows(ServiceException.class,()->service.updatePxNote(edit));
        }
    }
    @Test void invalidUsersAndForeignManagementFail() {
        assertThrows(ServiceException.class,()->create("missing","read"));
        assertThrows(ServiceException.class,()->create("owner","read"));
        var grant = create(null,"read");
        assertThrows(ServiceException.class,()->shares.revoke(((Number)grant.get("id")).longValue(),"2"));
        assertThrows(ServiceException.class,()->shares.create("note",22L,"1",null,"read",7));
    }
}
