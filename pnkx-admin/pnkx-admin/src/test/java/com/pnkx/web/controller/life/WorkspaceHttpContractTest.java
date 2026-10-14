package com.pnkx.web.controller.life;

import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.framework.config.SecurityConfig;
import com.pnkx.framework.security.filter.IntegrationTokenFilter;
import com.pnkx.framework.security.filter.JwtAuthenticationTokenFilter;
import com.pnkx.framework.security.handle.AuthenticationEntryPointImpl;
import com.pnkx.framework.security.handle.LogoutSuccessHandlerImpl;
import com.pnkx.framework.web.exception.GlobalExceptionHandler;
import com.pnkx.framework.web.service.SysPermissionService;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.service.IPxNoteService;
import com.pnkx.life.service.NoteRevisionConflictException;
import com.pnkx.life.service.workspace.UserFile;
import com.pnkx.life.service.workspace.UserFileService;
import com.pnkx.life.service.workspace.WorkspaceQaStore;
import com.pnkx.system.service.ISysUserService;
import com.pnkx.web.service.WorkspaceQaService;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.nio.file.*;
import java.util.*;
import java.util.function.IntConsumer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real production security/MVC stack; only token lookup and business services are test doubles.
 * SQL ownership is separately exercised by ResourceSearchIntegrationTest / NoteIsolationIntegrationTest. */
class WorkspaceHttpContractTest {
 private AnnotationConfigWebApplicationContext context;
 private MockMvc mvc;
 private IPxNoteService notes;
 private WorkspaceQaService qa;
 private WorkspaceQaStore drafts;
 private UserFileService files;
 private Path content;
 private static final String QUESTION="{\"requestId\":\"qa-http-test-0001\",\"question\":\"总结正文\",\"type\":\"note\"}";

 @Configuration @EnableWebMvc @Import(SecurityConfig.class)
 static class Config {
  @Bean AuthenticationEntryPointImpl unauthorizedHandler(){return new AuthenticationEntryPointImpl();}
  @Bean LogoutSuccessHandlerImpl logoutSuccessHandler(){return new LogoutSuccessHandlerImpl();}
  @Bean JwtAuthenticationTokenFilter authenticationTokenFilter(){return new JwtAuthenticationTokenFilter();}
  @Bean IntegrationTokenFilter integrationTokenFilter(){return new IntegrationTokenFilter();}
  @Bean CorsFilter corsFilter(){return new CorsFilter(new UrlBasedCorsConfigurationSource());}
  @Bean PxNoteController noteController(){return new PxNoteController();}
  @Bean UserFileController fileController(){return new UserFileController();}
  @Bean WorkspaceQaController qaController(){return new WorkspaceQaController();}
  @Bean GlobalExceptionHandler exceptionHandler(){return new GlobalExceptionHandler();}
 }
 @BeforeEach void setup() throws Exception {
  context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Config.class);
  context.addBeanFactoryPostProcessor(factory->{
   factory.registerSingleton("tokenService",mock(TokenService.class));
   factory.registerSingleton("userDetailsService",mock(UserDetailsService.class));
   factory.registerSingleton("userService",mock(ISysUserService.class));
   factory.registerSingleton("permissionService",mock(SysPermissionService.class));
   factory.registerSingleton("pxNoteService",mock(IPxNoteService.class));
   factory.registerSingleton("fileService",mock(UserFileService.class));
   factory.registerSingleton("qaService",mock(WorkspaceQaService.class));
   factory.registerSingleton("qaStore",mock(WorkspaceQaStore.class));
  });context.refresh();
  mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();
  notes=context.getBean(IPxNoteService.class);qa=context.getBean(WorkspaceQaService.class);drafts=context.getBean(WorkspaceQaStore.class);files=context.getBean(UserFileService.class);
  when(context.getBean(TokenService.class).getLoginUserFromToken(any(HttpServletRequest.class))).thenAnswer(invocation->{
   String header=invocation.<HttpServletRequest>getArgument(0).getHeader("Authorization");
   if(!List.of("Bearer test-alice","Bearer test-bob").contains(Objects.toString(header,"")))return null;
   SysUser user=new SysUser();user.setUserId(header.endsWith("alice")?101L:202L);user.setUserName(header.endsWith("alice")?"alice":"bob");return new LoginUser(user,Set.of());
  });
 }
 @AfterEach void cleanup() throws Exception {if(context!=null)context.close();if(content!=null)Files.deleteIfExists(content);}
 @Test void privateRoutesRejectAnonymousAndDoNotCallBusinessServices() throws Exception {
  for(String path:List.of("/note/71","/workspace/files/81/content","/workspace/qa"))
   mvc.perform(get(path)).andExpect(jsonPath("$.code").value(401));
  mvc.perform(post("/workspace/qa/stream").contentType(MediaType.APPLICATION_JSON).content(QUESTION)).andExpect(jsonPath("$.code").value(401));
  verifyNoInteractions(notes,files,qa,drafts);
 }
 @Test void twoTokensBindDifferentOwnersAndNoAuthenticationLeaksToNextRequest() throws Exception {
  when(notes.selectPxNoteById(71L)).thenAnswer(invocation->{
   if(!"101".equals(SecurityUtils.getUserId()))throw new ServiceException("资料不存在或不可访问");
   PxNote note=new PxNote();note.setId(71L);note.setContent("alice private body");return note;
  });
  mvc.perform(get("/note/71").header("Authorization","Bearer test-alice")).andExpect(jsonPath("$.data.content").value("alice private body"));
  mvc.perform(get("/note/71").header("Authorization","Bearer test-bob")).andExpect(jsonPath("$.code").value(500)).andExpect(content().string(not(containsString("alice private body"))));
  mvc.perform(get("/note/71")).andExpect(jsonPath("$.code").value(401));
  verify(notes,times(2)).selectPxNoteById(71L);
 }
 @Test void staleAndMissingRevisionUseHttp409WithRecoverableCurrentVersion() throws Exception {
  PxNote current=new PxNote();current.setId(71L);current.setRevision(5L);current.setContent("server version");
  when(notes.updatePxNote(any())).thenThrow(new NoteRevisionConflictException(current,false)).thenThrow(new NoteRevisionConflictException(current,true));
  for(String code:List.of("NOTE_REVISION_CONFLICT","NOTE_REVISION_REQUIRED"))
   mvc.perform(put("/note").header("Authorization","Bearer test-alice").contentType(MediaType.APPLICATION_JSON).content("{\"id\":71,\"revision\":4,\"content\":\"local draft\"}"))
    .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value(code)).andExpect(jsonPath("$.data.current.revision").value(5)).andExpect(jsonPath("$.data.current.content").value("server version"));
 }
 @Test void sseSerializesProgressAndFinalSourcesWithoutAsyncSecurityDispatch() throws Exception {
  when(qa.ask(any(),any())).thenAnswer(invocation->{
   assertEquals("101",SecurityUtils.getUserId());invocation.<IntConsumer>getArgument(1).accept(12);
   return Map.of("answer","回答\n[资料 1]","citations",List.of(Map.of("type","note","id","71")),"coverage","覆盖说明");
  });
  var result=mvc.perform(post("/workspace/qa/stream").header("Authorization","Bearer test-alice").contentType(MediaType.APPLICATION_JSON).content(QUESTION))
   .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Accel-Buffering","no")).andReturn();
  String body=result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertTrue(body.startsWith("event: progress\ndata: "));assertTrue(body.contains("event: result\ndata: "));assertTrue(body.contains("\\n[资料 1]"));assertFalse(result.getRequest().isAsyncStarted());
 }
 @Test void sseFailureIsAnErrorEventAndNeverAResultEvent() throws Exception {
  when(qa.ask(any(),any())).thenThrow(new ServiceException("引用资料已变化，请重新提问生成草稿"));
  mvc.perform(post("/workspace/qa/stream").header("Authorization","Bearer test-alice").contentType(MediaType.APPLICATION_JSON).content(QUESTION))
   .andExpect(content().string(containsString("event: error\n"))).andExpect(content().string(not(containsString("event: result"))));
 }
 @Test void cancellationUsesRequestIdentityAndAuthenticatedOwner() throws Exception {
  List<String> owners=new ArrayList<>();doAnswer(invocation->{owners.add(SecurityUtils.getUserId());return null;}).when(drafts).cancel("qa-http-test-0001");
  mvc.perform(post("/workspace/qa/qa-http-test-0001/cancel").header("Authorization","Bearer test-bob").contentType(MediaType.APPLICATION_JSON).content("{\"ownerId\":\"101\"}"))
   .andExpect(jsonPath("$.code").value(200));assertEquals(List.of("202"),owners);
 }
 @Test void fileDownloadUsesAuthorizedServiceAndPrivateResponseHeaders() throws Exception {
  content=Files.createTempFile("workspace-http-",".txt");Files.writeString(content,"private text");
  UserFile file=new UserFile();file.setId(81L);file.setDisplayName("资料.txt");file.setMime("text/plain");file.setByteSize(Files.size(content));
  mvc.perform(get("/workspace/files/81/content").header("Authorization","Bearer test-alice"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store, private")).andExpect(header().string("X-Content-Type-Options","nosniff"))
   .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;"))).andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM)).andExpect(content().string("private text"));
 }
}
