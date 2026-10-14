package com.pnkx.web.controller.life;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.life.service.workspace.UserFileService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
@RestController
@RequestMapping("/workspace/files")
public class UserFileController {
 @Resource private UserFileService service;
 public record Edit(Long revision,Long parentId,String displayName){}
 @GetMapping public AjaxResult list(@RequestParam(defaultValue="0") Long parent,@RequestParam(defaultValue="0") long after){return AjaxResult.success(service.list(parent,after));}
 @GetMapping("/folders") public AjaxResult folders(){return AjaxResult.success(service.folders());}
 @GetMapping("/{id}") public AjaxResult get(@PathVariable Long id){return AjaxResult.success(service.get(id));}
 @PostMapping("/folders") public AjaxResult folder(@RequestBody Edit body){return AjaxResult.success(service.folder(body.parentId(),body.displayName()));}
 @PostMapping("/upload") public AjaxResult upload(@RequestParam(defaultValue="0") Long parent,@RequestParam MultipartFile file,@RequestParam(required=false) String displayName) throws IOException{return AjaxResult.success(service.upload(parent,file,displayName));}
 @PutMapping("/{id}") public AjaxResult edit(@PathVariable Long id,@RequestBody Edit body){return AjaxResult.success(service.edit(id,body.revision(),body.parentId(),body.displayName()));}
 @DeleteMapping("/{id}") public AjaxResult delete(@PathVariable Long id,@RequestParam Long revision){service.delete(id,revision);return AjaxResult.success();}
 @GetMapping("/{id}/text") public AjaxResult text(@PathVariable Long id) throws IOException {
  var row=service.get(id);if(!"text/plain".equals(row.getMime())||row.getByteSize()>2*1024*1024)throw new com.pnkx.common.exception.ServiceException("文件不支持文本预览");
  try(java.io.InputStream in=service.open(row)){return AjaxResult.success(new String(in.readAllBytes(),StandardCharsets.UTF_8));}
 }
 @GetMapping("/{id}/content") public ResponseEntity<org.springframework.core.io.InputStreamResource> content(@PathVariable Long id,@RequestParam(defaultValue="false") boolean preview) throws IOException {
  var row=service.get(id);boolean inline=preview&&!"application/octet-stream".equals(row.getMime());java.io.InputStream in=service.open(row);
  return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store, private").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'; style-src 'unsafe-inline'")
   .header(HttpHeaders.CONTENT_DISPOSITION,(inline?ContentDisposition.inline():ContentDisposition.attachment()).filename(row.getDisplayName(),StandardCharsets.UTF_8).build().toString())
   .contentType(MediaType.parseMediaType(inline?row.getMime():"application/octet-stream")).contentLength(row.getByteSize()).body(new org.springframework.core.io.InputStreamResource(in));
 }
}
