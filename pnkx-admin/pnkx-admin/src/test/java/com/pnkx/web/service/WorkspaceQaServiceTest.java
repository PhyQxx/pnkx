package com.pnkx.web.service;

import com.pnkx.ai.AiClient;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.service.search.SearchResult;
import com.pnkx.life.service.search.SearchService;
import com.pnkx.life.service.workspace.*;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceQaServiceTest {
 private WorkspaceQaService service;
 private SearchService search;
 private WorkspaceQaStore store;
 private AiClient ai;
 private WorkspaceQaService.Question question=new WorkspaceQaService.Question("qa-test-request-01","证据是什么？","证据","note",null,null,List.of());
 @BeforeEach void setup() throws Exception {
  SysUser user=new SysUser();user.setUserId(101L);user.setUserName("test");
  LoginUser login=new LoginUser(user,Set.of());
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
  service=new WorkspaceQaService();search=mock(SearchService.class);store=mock(WorkspaceQaStore.class);ai=mock(AiClient.class);
  ResourceAccessService access=mock(ResourceAccessService.class);
  inject("search",search);inject("store",store);inject("aiClient",ai);inject("access",access);
  SearchResult hit=new SearchResult();hit.setType("note");hit.setId("71");hit.setTitle("标题");
  when(search.workspace(any(),any(),any(),any(),any(),any(),anyBoolean(),anyBoolean(),anyInt(),anyInt())).thenReturn(new SearchService.Page(List.of(hit),1,1,12,false));
  ResourceRecord row=new ResourceRecord();row.setId(71L);row.setTitle("标题");row.setContent("实际授权正文证据");row.setRevision(3L);
  when(access.read("note",71L)).thenReturn(row);
  when(store.finish(any(),eq("done"),any(),anyList(),any(),anyLong(),anyInt())).thenReturn(true);
  when(store.detail(any())).thenReturn(Map.of("answer","回答"));
 }
 private void inject(String name,Object value)throws Exception {var field=WorkspaceQaService.class.getDeclaredField(name);field.setAccessible(true);field.set(service,value);}
 private ChatResponse chunk(String text){return ChatResponse.builder().content(List.of(TextBlock.builder().text(text).build())).build();}
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
 @Test void actualBodyAndServerCitationsAreUsed() {
  when(ai.chatStream(any(),any())).thenReturn(Flux.just(chunk("根据"),chunk(" [资料 1]")));
  assertEquals("回答",service.ask(question,count->{}).get("answer"));
  ArgumentCaptor<String> prompt=ArgumentCaptor.forClass(String.class);verify(ai).chatStream(prompt.capture(),eq(question.question()));
  assertTrue(prompt.getValue().contains("实际授权正文证据"));
  verify(store).finish(eq(question.requestId()),eq("done"),eq("根据 [资料 1]"),argThat(sources->sources.size()==1 && sources.getFirst().id().equals("71") && sources.getFirst().revision()==3L),contains("匹配 1 条"),anyLong(),eq(9));
 }
 @Test void emptySearchDoesNotSpendModelCallOrCreateRun() {
  when(search.workspace(any(),any(),any(),any(),any(),any(),anyBoolean(),anyBoolean(),anyInt(),anyInt())).thenReturn(new SearchService.Page(List.of(),0,1,12,false));
  assertThrows(ServiceException.class,()->service.ask(question,count->{}));verifyNoInteractions(ai,store);
 }
 @Test void cancellationDisposesProviderAndDoesNotSavePartialAnswer() {
  AtomicBoolean disposed=new AtomicBoolean();when(store.cancelled("101",question.requestId())).thenReturn(true);
  when(ai.chatStream(any(),any())).thenReturn(Flux.concat(Flux.just(chunk("半截")),Flux.<ChatResponse>never()).doOnCancel(()->disposed.set(true)));
  assertThrows(ServiceException.class,()->service.ask(question,count->{}));assertTrue(disposed.get());
  verify(store,never()).finish(any(),eq("done"),any(),any(),any(),anyLong(),anyInt());
  verify(store).finish(eq(question.requestId()),eq("failed"),isNull(),anyList(),any(),anyLong(),eq(2));
 }
 @Test void sourceChangesBeforeDeliveryRejectAnswer() {
  when(ai.chatStream(any(),any())).thenReturn(Flux.just(chunk("回答")));
  when(store.finish(any(),eq("done"),any(),anyList(),any(),anyLong(),anyInt())).thenThrow(new ServiceException("引用资料已变化"));
  assertThrows(ServiceException.class,()->service.ask(question,count->{}));verify(store,never()).detail(any());
  verify(store).finish(any(),eq("failed"),isNull(),anyList(),any(),anyLong(),eq(2));
 }
 @Test void disconnectedStreamStopsProviderAndRecordsFailure() {
  AtomicBoolean disposed=new AtomicBoolean();when(ai.chatStream(any(),any())).thenReturn(Flux.concat(Flux.just(chunk("答案")),Flux.<ChatResponse>never()).doOnCancel(()->disposed.set(true)));
  assertThrows(ServiceException.class,()->service.ask(question,count->{if(count>0)throw new java.io.UncheckedIOException(new java.io.IOException("disconnect"));}));
  assertTrue(disposed.get());verify(store,never()).detail(any());
 }
}
