package com.pnkx.web.controller.tool;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.PnkxApplication;
import com.pnkx.domain.po.PxAiOperationLog;
import com.pnkx.service.IPxAiOperationLogService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PnkxApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class AiOperationLogServiceTest {

    @Autowired
    private IPxAiOperationLogService aiOperationLogService;

    @Test
    public void testAuditFlow() {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String question = "今天花了100元吃火锅";
        Long modelId = 1L;

        // 1. Start
        PxAiOperationLog log = aiOperationLogService.start(requestId, question, modelId, true);
        assertNotNull(log.getId());
        assertEquals(requestId, log.getRequestId());
        assertEquals(question, log.getQuestion());
        assertEquals("none", log.getWriteStatus());

        // 2. Finish Detection
        String intent = "bookkeeping";
        BigDecimal confidence = new BigDecimal("0.95");
        JSONObject parsedJson = new JSONObject().fluentPut("intent", intent).fluentPut("amount", 100);
        aiOperationLogService.finishDetection(requestId, intent, confidence, parsedJson.toJSONString(), 1500L);

        PxAiOperationLog detectedLog = aiOperationLogService.selectPxAiOperationLogById(log.getId());
        assertEquals(intent, detectedLog.getIntent());
        assertEquals(0, confidence.compareTo(detectedLog.getConfidence()));
        assertEquals("draft", detectedLog.getWriteStatus());
        assertEquals(Integer.valueOf(1), detectedLog.getIsWrite());

        // 3. Finish Write
        JSONObject draft = new JSONObject().fluentPut("money", 100).fluentPut("remark", "吃火锅");
        aiOperationLogService.finishWrite(requestId, true, "confirmed", draft.toJSONString(), null);

        PxAiOperationLog finalLog = aiOperationLogService.selectPxAiOperationLogById(log.getId());
        assertEquals("confirmed", finalLog.getWriteStatus());
        assertTrue(finalLog.getParsedJson().contains("吃火锅"));
        assertNull(finalLog.getErrorMsg());
    }

    @Test
    public void testCancelFlow() {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        aiOperationLogService.start(requestId, "测试取消", 1L, true);
        aiOperationLogService.finishDetection(requestId, "todo", new BigDecimal("0.8"), "{}", 100L);
        aiOperationLogService.finishWrite(requestId, true, "cancelled", "{}", null);

        PxAiOperationLog log = aiOperationLogService.selectPxAiOperationLogList(new PxAiOperationLog() {{
            setRequestId(requestId);
        }}).get(0);
        assertEquals("cancelled", log.getWriteStatus());
    }

    @Test
    public void testReadIntentDoesNotBecomeDraftWrite() {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        PxAiOperationLog started = aiOperationLogService.start(requestId, "查看生活周报", 1L, true);

        aiOperationLogService.finishDetection(requestId, "life_report", new BigDecimal("0.91"), "{}", 100L);

        PxAiOperationLog log = aiOperationLogService.selectPxAiOperationLogById(started.getId());
        assertEquals(Integer.valueOf(0), log.getIsWrite());
        assertEquals("none", log.getWriteStatus());
    }
}
