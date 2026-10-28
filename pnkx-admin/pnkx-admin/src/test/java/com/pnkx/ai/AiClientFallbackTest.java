package com.pnkx.ai;

import com.pnkx.ai.domain.po.PxAiModelConfig;
import com.pnkx.ai.mapper.PxAiModelConfigMapper;
import com.pnkx.common.core.redis.RedisCache;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.Model;
import io.agentscope.core.message.TextBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AiClientFallbackTest {
    private AiClient client;
    private PxAiModelConfigMapper mapper;
    private Model primary;
    private Model backup;
    private ChatResponse response;

    @BeforeEach
    public void setUp() {
        client = new AiClient();
        mapper = mock(PxAiModelConfigMapper.class);
        primary = mock(Model.class);
        backup = mock(Model.class);
        ReflectionTestUtils.setField(client, "aiModelConfigMapper", mapper);
        ReflectionTestUtils.setField(client, "redisCache", mock(RedisCache.class));
        Map<Long, Model> cache = (Map<Long, Model>) ReflectionTestUtils.getField(client, "modelCache");
        cache.put(1L, primary);
        cache.put(2L, backup);
        when(mapper.selectEnabledModelsForFallback()).thenReturn(List.of(config(1L), config(2L)));
        response = mock(ChatResponse.class);
        when(response.getContent()).thenReturn(List.of(TextBlock.builder().text("备用回答").build()));
        when(backup.stream(anyList(), isNull(), any())).thenReturn(Flux.just(response));
    }

    private PxAiModelConfig config(long id) {
        PxAiModelConfig config = new PxAiModelConfig();
        config.setId(id);
        config.setModelKey("model-" + id);
        config.setTemperature(0.7);
        return config;
    }

    @Test
    public void syncFailureUsesBackupAndReturnsActualModel() {
        when(primary.stream(anyList(), isNull(), any())).thenReturn(Flux.error(new IllegalStateException("offline")));
        assertEquals("model-2", client.chat("system", "question").getString("model"));
    }

    @Test
    public void syncEmptyUsesBackup() {
        when(primary.stream(anyList(), isNull(), any())).thenReturn(Flux.empty());
        assertEquals("备用回答", client.chat("system", "question").getString("content"));
    }

    @Test
    public void primarySuccessDoesNotCallBackup() {
        when(primary.stream(anyList(), isNull(), any())).thenReturn(Flux.just(response));
        assertEquals("model-1", client.chat("system", "question").getString("model"));
        verifyNoInteractions(backup);
    }

    @Test
    public void streamFailureAndEmptyUseBackup() {
        when(primary.stream(anyList(), isNull(), any())).thenReturn(Flux.error(new IllegalStateException("offline")));
        assertEquals(List.of(response), client.chatStream("system", "question").collectList().block());
        when(primary.stream(anyList(), isNull(), any())).thenReturn(Flux.empty());
        assertEquals(List.of(response), client.chatStream("system", "question").collectList().block());
    }

    @Test
    public void streamDoesNotSwitchAfterOutput() {
        when(primary.stream(anyList(), isNull(), any())).thenReturn(
                Flux.concat(Flux.just(response), Flux.error(new IllegalStateException("broken"))));
        assertThrows(IllegalStateException.class, () -> client.chatStream("system", "question").collectList().block());
        verifyNoInteractions(backup);
    }

    @Test
    public void streamTimeoutUsesBackup() {
        ReflectionTestUtils.setField(client, "idleTimeoutSeconds", 1L);
        when(primary.stream(anyList(), isNull(), any())).thenReturn(Flux.never());
        assertEquals(List.of(response), client.chatStream("system", "question").collectList().block());
    }

    @Test
    public void allFailedTerminates() {
        when(primary.stream(anyList(), isNull(), any())).thenReturn(Flux.error(new IllegalStateException("offline")));
        when(backup.stream(anyList(), isNull(), any())).thenReturn(Flux.error(new IllegalStateException("offline")));
        assertNull(client.chat("system", "question"));
        assertThrows(IllegalStateException.class, () -> client.chatStream("system", "question").collectList().block());
    }

    @Test
    public void noEnabledModelsTerminates() {
        when(mapper.selectEnabledModelsForFallback()).thenReturn(List.of());
        assertNull(client.chat("system", "question"));
        assertThrows(IllegalStateException.class, () -> client.chatStream("system", "question").collectList().block());
    }
}
