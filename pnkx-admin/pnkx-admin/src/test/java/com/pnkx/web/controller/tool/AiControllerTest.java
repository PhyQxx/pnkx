package com.pnkx.web.controller.tool;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AiControllerTest {

    @Test
    public void resolveModelIdAcceptsNumericAndStringValues() {
        Map<String, Object> body = new HashMap<>();

        body.put("modelId", 3);
        assertEquals(Long.valueOf(3L), AiController.resolveModelId(body));

        body.put("modelId", "8");
        assertEquals(Long.valueOf(8L), AiController.resolveModelId(body));

        body.put("modelId", " ");
        assertNull(AiController.resolveModelId(body));

        body.remove("modelId");
        assertNull(AiController.resolveModelId(body));
    }
}
