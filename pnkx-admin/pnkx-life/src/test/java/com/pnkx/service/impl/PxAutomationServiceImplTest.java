package com.pnkx.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PxAutomationServiceImplTest {
    @Test
    void retryReusesStableBusinessWriteKey() {
        String first = PxAutomationServiceImpl.stableWriteKey(12L, "event-42", null);
        String retry = PxAutomationServiceImpl.stableWriteKey(12L, "ignored", "exec:12:event-42");
        assertEquals("auto:12:event-42", first);
        assertEquals(first, retry);
    }
}
