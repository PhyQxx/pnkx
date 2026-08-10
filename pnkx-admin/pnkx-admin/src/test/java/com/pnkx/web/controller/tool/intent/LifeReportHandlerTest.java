package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.service.AiLifeReportDataService;
import io.agentscope.core.model.ChatResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class LifeReportHandlerTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private AiLifeReportDataService lifeReportDataService;

    @InjectMocks
    private LifeReportHandler lifeReportHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testHandleLifeReport() throws IOException {
        JSONObject intentData = new JSONObject();
        intentData.put("period", "week");
        intentData.put("reportType", "summary");

        when(lifeReportDataService.buildReportData(anyString(), anyString(), anyString())).thenReturn(new JSONObject());
        when(aiClient.chatStream(anyString(), anyString())).thenReturn(Flux.empty());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean handled = lifeReportHandler.handle("帮我总结下这一周的生活", intentData, out);

        assertTrue(handled);
        String result = out.toString();
        assertTrue(result.contains("[DONE]"));
    }
}
