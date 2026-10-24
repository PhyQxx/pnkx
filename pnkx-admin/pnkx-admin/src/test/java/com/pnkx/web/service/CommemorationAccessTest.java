package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.PxCommemorationDay;
import com.pnkx.life.mapper.PxCommemorationDayMapper;
import com.pnkx.life.service.impl.PxCommemorationDayServiceImpl;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class CommemorationAccessTest {
  PxCommemorationDayMapper mapper;
  DataPermissionService permissions;
  PxCommemorationDayServiceImpl service;

  @BeforeEach
  void setup() {
    mapper = mock(PxCommemorationDayMapper.class);
    permissions = mock(DataPermissionService.class);
    service = new PxCommemorationDayServiceImpl();
    ReflectionTestUtils.setField(service, "pxCommemorationDayMapper", mapper);
    ReflectionTestUtils.setField(service, "dataPermissionService", permissions);
  }

  @Test
  void detailRejectsForeignAndSoftDeletedRecords() {
    var row = new PxCommemorationDay();
    row.setCreateBy("2");
    when(mapper.selectPxCommemorationDayById(1L)).thenReturn(row);
    when(permissions.getVisibleUserIds("commemoration")).thenReturn(List.of(1L));
    assertThrows(Exception.class, () -> service.selectPxCommemorationDayById(1L));
    when(permissions.getVisibleUserIds("commemoration")).thenReturn(List.of(1L, 2L));
    assertSame(row, service.selectPxCommemorationDayById(1L));
    row.setDelFlag(true);
    assertThrows(Exception.class, () -> service.selectPxCommemorationDayById(1L));
  }

  @Test
  void insertIgnoresSpoofedOwnerAndUpdatePreservesOriginalOwnership() {
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUserId).thenReturn("1");
      var inserted = new PxCommemorationDay();
      inserted.setCreateBy("9");
      service.insertPxCommemorationDay(inserted);
      assertEquals("1", inserted.getCreateBy());
      var before = new PxCommemorationDay();
      before.setCreateBy("2");
      when(mapper.selectPxCommemorationDayById(3L)).thenReturn(before);
      when(permissions.canWrite("2", "commemoration")).thenReturn(true);
      var edit = new PxCommemorationDay();
      edit.setId(3L);
      edit.setCreateBy("9");
      service.updatePxCommemorationDay(edit);
      assertEquals("2", edit.getCreateBy());
      assertEquals("1", edit.getUpdateBy());
    }
  }
}
