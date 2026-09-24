package com.pnkx.service.impl;

import com.pnkx.domain.po.PxAiOperationLog;
import com.pnkx.mapper.PxAiOperationLogMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PxAiOperationLogServiceImplTest {
    @Test
    void confirmableCrossModuleIntentStartsAsDraftWrite() throws Exception {
        AtomicReference<PxAiOperationLog> updated = new AtomicReference<>();
        PxAiOperationLogMapper mapper = (PxAiOperationLogMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{PxAiOperationLogMapper.class},
                (proxy, method, args) -> {
                    if ("updatePxAiOperationLogByRequestId".equals(method.getName())) {
                        updated.set((PxAiOperationLog) args[0]);
                        return 1;
                    }
                    return method.getReturnType().isPrimitive() ? 0 : null;
                });
        PxAiOperationLogServiceImpl service = new PxAiOperationLogServiceImpl();
        Field field = PxAiOperationLogServiceImpl.class.getDeclaredField("pxAiOperationLogMapper");
        field.setAccessible(true);
        field.set(service, mapper);

        service.finishDetection("request-1", "content_publish", new BigDecimal("0.98"), "{}", 12L);

        assertEquals(1, updated.get().getIsWrite());
        assertEquals("draft", updated.get().getWriteStatus());
    }
}
