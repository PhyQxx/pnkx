package com.pnkx.web.controller.blog;
import com.pnkx.blog.mapper.PxAnalyticsMapper;
import com.pnkx.blog.service.AnalyticsRetention;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
class AnalyticsRetentionTest {
    @Test void disabledNeverTouchesDatabase() {
        var mapper=mock(PxAnalyticsMapper.class);
        assertEquals(0,new AnalyticsRetention(mapper,false).cleanup());verifyNoInteractions(mapper);
    }
    @Test void stopsOnPartialBatch() {
        var mapper=mock(PxAnalyticsMapper.class);when(mapper.deleteExpired(any())).thenReturn(1000,25);
        assertEquals(1025,new AnalyticsRetention(mapper,true).cleanup());verify(mapper,times(2)).deleteExpired(any());
    }
    @Test void cappedAtTenBatches() {
        var mapper=mock(PxAnalyticsMapper.class);when(mapper.deleteExpired(any())).thenReturn(1000);
        assertEquals(10000,new AnalyticsRetention(mapper,true).cleanup());verify(mapper,times(10)).deleteExpired(any());
    }
    @Test void cutoffUsesShanghaiCalendar() {
        var clock=Clock.fixed(Instant.parse("2026-11-23T16:01:00Z"),ZoneOffset.UTC);
        assertEquals(LocalDate.of(2026,11,24).minusDays(90).atStartOfDay(),AnalyticsRetention.cutoff(clock));
    }
    @Test void previewNeverDeletes() {
        var mapper=mock(PxAnalyticsMapper.class);when(mapper.countExpired(any())).thenReturn(12L);
        assertEquals(12L,new AnalyticsRetention(mapper,true).preview().get("expiredCount"));verify(mapper,never()).deleteExpired(any());
    }
}
