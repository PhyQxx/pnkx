package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.service.IPxNoteService;
import com.pnkx.mapper.PxNoteMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class NoteHandlerTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private IPxNoteService noteService;

    @Mock
    private PxNoteMapper noteMapper;

    @Mock
    private AiPendingActionService pendingActionService;

    @InjectMocks
    private NoteHandler noteHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testHandleCreate() throws IOException {
        JSONObject intentData = new JSONObject();
        intentData.put("action", "create");
        intentData.put("requestId", "test-req");
        
        String json = "{\"title\": \"测试笔记\", \"content\": \"这是测试内容\"}";
        when(aiClient.chat(anyString(), anyString())).thenReturn(new JSONObject().fluentPut("content", json));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean handled = noteHandler.handle("帮我记个笔记：测试内容", intentData, out);

        assertTrue(handled);
        String result = out.toString();
        assertTrue(result.contains("笔记草稿"));
        assertTrue(result.contains("测试笔记"));
        assertTrue(result.contains("[PENDING_CONFIRM]"));
    }
}
