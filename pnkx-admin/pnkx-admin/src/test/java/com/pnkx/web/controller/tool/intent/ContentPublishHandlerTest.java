package com.pnkx.web.controller.tool.intent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentPublishHandlerTest {
    @Test
    void detectsAndRedactsCommonPrivateIdentifiers() {
        String source = "联系 13812345678，邮箱 alice@example.com，身份证 110101199001011234";
        assertFalse(ContentPublishHandler.privacyRisks(source).isEmpty());
        String redacted = ContentPublishHandler.redact(source);
        assertFalse(redacted.contains("13812345678"));
        assertFalse(redacted.contains("alice@example.com"));
        assertFalse(redacted.contains("110101199001011234"));
        assertTrue(redacted.contains("已隐藏"));
    }
}
