package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NoteShareServiceTest {
    JdbcTemplate db;
    NoteShareService service;
    Map<String,Map<String,Object>> resources;
    Map<String,Object> grant;

    @BeforeEach void setup() {
        db = mock(JdbcTemplate.class);
        service = new NoteShareService(db);
        resources = new HashMap<>();
        put("folder",10,0,"1"); put("folder",11,10,"1"); put("note",20,11,"1"); put("note",21,0,"1");
        grant = new HashMap<>(Map.of("id",1L,"resource_type","folder","resource_id",10L,"owner_id","1","permission","read"));
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            if (sql.startsWith("select id,create_by owner,")) {
                String type = sql.contains("from px_note_folder ") ? "folder" : "note";
                var row = resources.get(type + inv.getArgument(1));
                return row == null ? List.of() : List.of(row);
            }
            return List.of();
        });
    }
    void put(String type, long id, long parent, String owner) {
        resources.put(type+id,new HashMap<>(Map.of("id",id,"parent",parent,"owner",owner,"title","Title")));
    }
    @Test void inheritedFolderAccessIncludesNestedNotes() { assertTrue(service.contains(grant,"note",20L)); }
    @Test void siblingIsNotShared() { assertFalse(service.contains(grant,"note",21L)); }
    @Test void movingOutsideRevokesInheritedAccess() {
        put("folder",11,0,"1"); assertFalse(service.contains(grant,"note",20L));
    }
    @Test void deletedAncestorRevokesInheritedAccess() {
        resources.remove("folder11"); assertFalse(service.contains(grant,"note",20L));
    }
    @Test void foreignOwnerInsideSharedFolderIsExcluded() {
        put("note",20,11,"2"); assertFalse(service.contains(grant,"note",20L));
    }
    @Test void cyclesFailClosed() {
        put("folder",11,12,"1"); put("folder",12,11,"1"); assertFalse(service.contains(grant,"note",20L));
    }
    @Test void singleNoteGrantDoesNotShareItsFolder() {
        grant.put("resource_type","note"); grant.put("resource_id",20L);
        assertTrue(service.contains(grant,"note",20L)); assertFalse(service.contains(grant,"folder",11L));
    }
    @Test void readGrantDoesNotAllowWrites() {
        when(db.queryForList(contains("where recipient_id=?"), eq("2"))).thenAnswer(inv ->
                inv.getArgument(0,String.class).contains("permission='edit'") ? List.of() : List.of(grant));
        assertDoesNotThrow(() -> service.requireAccess("note",20L,"2",false));
        assertThrows(ServiceException.class, () -> service.requireAccess("note",20L,"2",true));
    }
    @Test void editGrantAllowsWritesButNotShareManagement() {
        grant.put("permission","edit");
        when(db.queryForList(contains("where recipient_id=?"), eq("2"))).thenReturn(List.of(grant));
        assertDoesNotThrow(() -> service.requireAccess("note",20L,"2",true));
        assertThrows(ServiceException.class, () -> service.managed("note",20L,"2"));
    }
    @Test void revokedExpiredOrUnknownLinkFailsClosed() {
        assertThrows(ServiceException.class, () -> service.browse(null,"gone",null,null,null));
        verify(db).queryForList(contains("expires_at>now()"),eq("gone"));
    }
    @Test void publicLinkCannotBrowseOutsideGrant() {
        when(db.queryForList(contains("where token=?"),eq("token"))).thenReturn(List.of(grant));
        assertThrows(ServiceException.class, () -> service.browse(null,"token",null,"note",21L));
    }
    @Test void publicLinkNeverReturnsWritePermissionOrOwner() {
        grant.put("permission","edit");
        when(db.queryForList(contains("where token=?"),eq("token"))).thenReturn(List.of(grant));
        when(db.queryForMap(anyString(),eq(20L))).thenReturn(Map.of("content","hello","richText","<p>hello</p>"));
        var result = service.browse(null,"token",null,"note",20L);
        assertEquals(false,result.get("editable")); assertFalse(result.containsKey("owner")); assertFalse(result.containsKey("parent"));
    }
    @Test void createRejectsInvalidPermissionAndExpiry() {
        assertThrows(ServiceException.class, () -> service.create("note",20L,"1",null,"edit",7));
        assertThrows(ServiceException.class, () -> service.create("note",20L,"1",null,"read",-1));
        assertThrows(ServiceException.class, () -> service.create("note",20L,"2",null,"read",7));
        assertThrows(ServiceException.class, () -> service.create("unknown",20L,"1",null,"read",7));
    }
    @Test void anonymousCannotUsePrivateRead() {
        assertThrows(ServiceException.class, () -> service.requireAccess("note",20L,null,false));
    }
}
