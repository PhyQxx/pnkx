package com.pnkx.web.controller.life;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.life.service.workspace.WorkspaceQaStore;
import com.pnkx.web.service.WorkspaceQaService;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.util.Map;

@RestController
@RequestMapping("/workspace/qa")
public class WorkspaceQaController {
 @Resource private WorkspaceQaService service;
 @Resource private WorkspaceQaStore store;
 @PostMapping public AjaxResult ask(@RequestBody WorkspaceQaService.Question body) {return AjaxResult.success(service.ask(body,count->{}));}
 @PostMapping(value="/stream",produces="text/event-stream")
 public void stream(@RequestBody WorkspaceQaService.Question body,HttpServletResponse response) throws IOException {
  response.setContentType("text/event-stream;charset=UTF-8");response.setHeader("Cache-Control","no-store");response.setHeader("X-Accel-Buffering","no");
  Writer writer=new OutputStreamWriter(response.getOutputStream(),java.nio.charset.StandardCharsets.UTF_8);
  try {var result=service.ask(body,count->event(writer,"progress",Map.of("outputChars",count)));event(writer,"result",result);}
  catch(RuntimeException ex) {event(writer,"error",Map.of("message",ex instanceof com.pnkx.common.exception.ServiceException?ex.getMessage():"问答失败，请重试"));}
 }
 private static void event(Writer writer,String type,Object data) {
  try {writer.write("event: "+type+"\ndata: "+JSON.toJSONString(data)+"\n\n");writer.flush();}catch(IOException ex){throw new UncheckedIOException(ex);}
 }
 @GetMapping public AjaxResult list(){return AjaxResult.success(store.list());}
 @GetMapping("/{id}") public AjaxResult get(@PathVariable String id){return AjaxResult.success(store.detail(id));}
 @PostMapping("/{id}/cancel") public AjaxResult cancel(@PathVariable String id){store.cancel(id);return AjaxResult.success();}
 public record Draft(String type,String title,String content) {}
 @PostMapping("/{id}/apply") public AjaxResult apply(@PathVariable String id,@RequestBody Draft body){return AjaxResult.success(store.apply(id,body.type(),body.title(),body.content()));}
}
