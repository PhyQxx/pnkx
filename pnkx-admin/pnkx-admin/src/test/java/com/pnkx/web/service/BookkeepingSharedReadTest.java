package com.pnkx.web.service;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.PxBookkeepingRecord;
import com.pnkx.life.mapper.PxBookkeepingRecordMapper;
import com.pnkx.life.service.impl.PxBookkeepingRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookkeepingSharedReadTest {
    @Test void detailRespectsFinancialSharingScope() {
        var mapper = mock(PxBookkeepingRecordMapper.class);
        var permissions = mock(DataPermissionService.class);
        var service = new PxBookkeepingRecordServiceImpl();
        ReflectionTestUtils.setField(service, "pxBookkeepingRecordMapper", mapper);
        ReflectionTestUtils.setField(service, "dataPermissionService", permissions);
        var partnerRecord = new PxBookkeepingRecord();
        partnerRecord.setCreateBy("2");
        when(mapper.selectPxBookkeepingRecordById(10L)).thenReturn(partnerRecord);
        when(permissions.getVisibleUserIds("budget")).thenReturn(List.of(1L, 2L));
        assertSame(partnerRecord, service.selectPxBookkeepingRecordById(10L));
        when(permissions.getVisibleUserIds("budget")).thenReturn(List.of(1L));
        assertThrows(ServiceException.class, () -> service.selectPxBookkeepingRecordById(10L));
        assertThrows(ServiceException.class, () -> service.selectPxBookkeepingRecordById(99L));
    }
}
