package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.domain.po.PxBookkeepingRecord;
import com.pnkx.life.mapper.PxBookkeepingRecordMapper;
import com.pnkx.life.service.impl.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PrivateFinanceBoundaryTest {
  @Test
  void foreignRecordCannotBeReadEditedOrDeleted() {
    var mapper = mock(PxBookkeepingRecordMapper.class);
    var foreign = new PxBookkeepingRecord();
    foreign.setId(9L);
    foreign.setCreateBy("3");
    when(mapper.selectPxBookkeepingRecordById(9L)).thenReturn(foreign);
    var service = new PxBookkeepingRecordServiceImpl();
    ReflectionTestUtils.setField(service, "pxBookkeepingRecordMapper", mapper);
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUserId).thenReturn("2");
      assertThrows(ServiceException.class, () -> service.selectPxBookkeepingRecordById(9L));
      assertThrows(ServiceException.class, () -> service.updatePxBookkeepingRecord(foreign));
      assertThrows(ServiceException.class, () -> service.deletePxBookkeepingRecordById(9L));
      assertThrows(
          ServiceException.class, () -> service.deletePxBookkeepingRecordByIds(new Long[] {9L}));
      verify(mapper, never()).updatePxBookkeepingRecord(any());
      verify(mapper, never()).deletePxBookkeepingRecordById(anyLong());
    }
  }

  @Test
  void allAnalysisStillRestrictsToCurrentOwner() {
    var mapper = mock(PxBookkeepingRecordMapper.class);
    when(mapper.getNaturalLanguageList(any())).thenReturn(List.of());
    var service = new PxBookkeepingAiService();
    ReflectionTestUtils.setField(service, "pxBookkeepingRecordMapper", mapper);
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUserId).thenReturn("2");
      ReflectionTestUtils.invokeMethod(service, "buildAnalysisQuestion", true);
      verify(mapper).getNaturalLanguageList(argThat(q -> "2".equals(q.getCreateBy())));
    }
  }

  @Test
  void missingIdentityCannotFallBackToAllAccounts() {
    var mapper = mock(PxBookkeepingRecordMapper.class);
    var service = new PxBookkeepingAiService();
    ReflectionTestUtils.setField(service, "pxBookkeepingRecordMapper", mapper);
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUserId).thenThrow(new ServiceException("未登录"));
      assertThrows(
          ServiceException.class,
          () -> ReflectionTestUtils.invokeMethod(service, "buildAnalysisQuestion", false));
      verifyNoInteractions(mapper);
    }
  }
}
