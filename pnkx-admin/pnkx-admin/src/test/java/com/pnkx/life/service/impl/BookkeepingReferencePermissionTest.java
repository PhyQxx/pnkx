package com.pnkx.life.service.impl;

import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import com.pnkx.life.service.IPxCommemorationDayService;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookkeepingReferencePermissionTest {
    PxBookkeepingRecordServiceImpl service;
    PxBookkeepingAccountMapper accounts;
    PxBookkeepingClassificationMapper categories;
    PxBookkeepingRecordMapper records;
    DataPermissionService permissions;

    @BeforeEach
    void setup() {
        SysUser user = new SysUser();
        user.setUserId(2L);
        LoginUser login = new LoginUser();
        login.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, List.of()));
        service = new PxBookkeepingRecordServiceImpl();
        accounts = mock(PxBookkeepingAccountMapper.class);
        categories = mock(PxBookkeepingClassificationMapper.class);
        records = mock(PxBookkeepingRecordMapper.class);
        permissions = mock(DataPermissionService.class);
        ReflectionTestUtils.setField(service, "ownedAccountMapper", accounts);
        ReflectionTestUtils.setField(service, "classificationMapper", categories);
        ReflectionTestUtils.setField(service, "pxBookkeepingRecordMapper", records);
        ReflectionTestUtils.setField(service, "dataPermissionService", permissions);
        ReflectionTestUtils.setField(service, "budgetService", mock(PxBookkeepingBudgetServiceImpl.class));
        ReflectionTestUtils.setField(service, "commemorationDayService", mock(IPxCommemorationDayService.class));
        when(permissions.getVisibleUserIds("")).thenReturn(List.of(2L));
        when(records.insertPxBookkeepingRecord(any())).thenReturn(1);
        account(19L, "1");
        PxBookkeepingClassification category = new PxBookkeepingClassification();
        category.setCreateBy("1");
        when(categories.selectPxBookkeepingClassificationById(27L)).thenReturn(category);
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    void account(long id, String owner) {
        PxBookkeepingAccount account = new PxBookkeepingAccount();
        account.setId(id);
        account.setCreateBy(owner);
        when(accounts.selectPxBookkeepingAccountById(id)).thenReturn(account);
    }

    PxBookkeepingRecord record() {
        PxBookkeepingRecord r = new PxBookkeepingRecord();
        r.setAccount(19L);
        r.setType(27L);
        return r;
    }

    @Test
    void sharedReferencesCanBeUsedAndRecordRemainsPrivate() {
        when(permissions.getVisibleUserIds("")).thenReturn(List.of(2L, 1L));
        PxBookkeepingRecord r = record();
        assertEquals(1, service.insertPxBookkeepingRecord(r));
        assertEquals("2", r.getCreateBy());
    }

    @Test
    void adminCanUseLegacyReferencesWithEmptyOwner() {
        when(permissions.getVisibleUserIds("")).thenReturn(null);
        account(19L, "");
        assertEquals(1, service.insertPxBookkeepingRecord(record()));
    }

    @Test
    void inaccessibleAccountIsRejectedBeforeWriting() {
        assertThrows(ServiceException.class, () -> service.insertPxBookkeepingRecord(record()));
        verifyNoInteractions(records);
    }

    @Test
    void inaccessibleCategoryIsRejectedBeforeWriting() {
        account(19L, "2");
        assertEquals("分类不存在或无权使用", assertThrows(ServiceException.class,
                () -> service.insertPxBookkeepingRecord(record())).getMessage());
        verifyNoInteractions(records);
    }

    @Test
    void missingAccountIsRejectedEvenForAdmin() {
        when(permissions.getVisibleUserIds("")).thenReturn(null);
        when(accounts.selectPxBookkeepingAccountById(19L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.insertPxBookkeepingRecord(record()));
        verifyNoInteractions(records);
    }

    @Test
    void transferChecksTargetVisibilityAndRejectsSameAccount() {
        account(19L, "2");
        account(20L, "1");
        PxBookkeepingRecord r = record();
        r.setType(0L);
        r.setOtherAccount(20L);
        assertThrows(ServiceException.class, () -> service.insertPxBookkeepingRecord(r));
        verifyNoInteractions(records);
        when(permissions.getVisibleUserIds("")).thenReturn(List.of(2L, 1L));
        assertEquals(1, service.insertPxBookkeepingRecord(r));
        r.setOtherAccount(19L);
        assertThrows(ServiceException.class, () -> service.insertPxBookkeepingRecord(r));
        verify(records, times(1)).insertPxBookkeepingRecord(any());
    }

    @Test
    void sharedReferencesWorkForBatchAndUpdate() {
        when(permissions.getVisibleUserIds("")).thenReturn(List.of(2L, 1L));
        assertEquals(1, service.insertBatchRecord(List.of(record())));
        PxBookkeepingRecord before = record();
        before.setCreateBy("2");
        when(records.selectPxBookkeepingRecordById(100L)).thenReturn(before);
        when(records.updatePxBookkeepingRecord(any())).thenReturn(1);
        PxBookkeepingRecord update = new PxBookkeepingRecord();
        update.setId(100L);
        assertEquals(1, service.updatePxBookkeepingRecord(update));
        before.setCreateBy("1");
        assertThrows(ServiceException.class, () -> service.updatePxBookkeepingRecord(update));
        verify(records, times(1)).updatePxBookkeepingRecord(any());
    }
}
