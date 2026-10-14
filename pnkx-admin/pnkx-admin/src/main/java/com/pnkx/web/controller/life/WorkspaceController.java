package com.pnkx.web.controller.life;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.life.service.workspace.*;
import com.pnkx.life.service.search.SearchService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import com.pnkx.common.exception.ServiceException;

@RestController
@RequestMapping("/workspace")
public class WorkspaceController {
    @ExceptionHandler(com.pnkx.life.service.NoteRevisionConflictException.class)
    public org.springframework.http.ResponseEntity<AjaxResult> conflict(com.pnkx.life.service.NoteRevisionConflictException ex) {
        AjaxResult body=AjaxResult.error(409,ex.getMessage());
        body.put("errorCode",ex.getErrorCode()); body.put("data",Map.of("current",ex.getCurrent()));
        return org.springframework.http.ResponseEntity.status(409).body(body);
    }
    @Resource private com.pnkx.life.service.recurrence.TodoSeriesService series;
    @GetMapping("/todo-series") public AjaxResult series() { return AjaxResult.success(series.list()); }
    @PostMapping("/todo-series") public AjaxResult saveSeries(@RequestBody com.pnkx.life.service.recurrence.TodoSeries body,@RequestParam(defaultValue="future") String scope) { return AjaxResult.success(series.save(body,scope)); }
    @Resource private OrganizeService organize;
    @PostMapping("/organize/scan/{type}") public AjaxResult scan(@PathVariable String type,@RequestParam(defaultValue="0") long after) { return AjaxResult.success(organize.scan(type,after)); }
    @GetMapping("/organize") public AjaxResult suggestions(@RequestParam(defaultValue="0") long after) { return AjaxResult.success(organize.list(after)); }
    @PostMapping("/organize/{id}/apply") public AjaxResult apply(@PathVariable Long id,@RequestParam(defaultValue="false") boolean ignore) { return AjaxResult.success(organize.apply(id,ignore)); }
    @Resource private NoteChangeFeedService noteFeed;
    @GetMapping("/notes/changes") public AjaxResult changes(@RequestParam(defaultValue="0") String cursor) { return AjaxResult.success(noteFeed.page(cursor)); }
    @Resource private ResourceLifecycleService lifecycle;
    public record RestoreVersion(Long revision) {}
    @GetMapping("/notes/{id}/history") public AjaxResult history(@PathVariable Long id) { return AjaxResult.success(lifecycle.history(id)); }
    @GetMapping("/notes/{id}/history/{revision}") public AjaxResult historical(@PathVariable Long id,@PathVariable Long revision) { return AjaxResult.success(lifecycle.historical(id,revision)); }
    @PostMapping("/notes/{id}/history/{version}/restore") public AjaxResult restoreVersion(@PathVariable Long id,@PathVariable Long version,@RequestBody RestoreVersion body) { return AjaxResult.success(lifecycle.restoreVersion(id,version,body.revision())); }
    @GetMapping("/trash") public AjaxResult trash(@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize) { return AjaxResult.success(lifecycle.trash(pageNum,pageSize)); }
    @PostMapping("/trash/{type}/{id}/restore") public AjaxResult restore(@PathVariable String type,@PathVariable Long id) { lifecycle.restore(type,id); return AjaxResult.success(); }
    @Resource private ResourceTagService tags;
    @Resource private SearchService search;
    @Resource private ResourceAccessService access;
    @Resource private ResourceLinkService links;
    @Resource private ResourceConvertService convert;
    @GetMapping("/resources")
    public AjaxResult resources(String q, @RequestParam(defaultValue="all") String type, String startDate, String endDate,
            @RequestParam(required=false) List<Long> tagIds, @RequestParam(defaultValue="all") String tagMode,
            @RequestParam(defaultValue="false") boolean includeDescendants, @RequestParam(defaultValue="false") boolean untagged,
            @RequestParam(required=false) Boolean readFlag,@RequestParam(required=false) Boolean starFlag,
            @RequestParam(required=false) Boolean inbox, @RequestParam(defaultValue="false") boolean recent,
            @RequestParam(defaultValue="1") int pageNum, @RequestParam(defaultValue="20") int pageSize) {
        return AjaxResult.success(search.workspaceFiltered(q,type,startDate,endDate,tagIds,tagMode,includeDescendants,untagged,inbox,recent,pageNum,pageSize,readFlag,starFlag));
    }
    public record Conversion(String targetType, String requestId, String title, String content) {}
    @PostMapping("/webArticles/{id}/convert") public AjaxResult convert(@PathVariable Long id,@RequestBody Conversion body) {
        return AjaxResult.success(convert.convert(id,body.targetType(),body.requestId(),body.title(),body.content()));
    }
    public record LinkTarget(String type, Long id) {}
    @GetMapping("/resources/{type}/{id}/links") public AjaxResult links(@PathVariable String type, @PathVariable Long id) { return AjaxResult.success(links.outgoing(type,id)); }
    @GetMapping("/resources/{type}/{id}/backlinks") public AjaxResult backlinks(@PathVariable String type, @PathVariable Long id) { return AjaxResult.success(links.incoming(type,id)); }
    @PostMapping("/resources/{type}/{id}/links") public AjaxResult link(@PathVariable String type, @PathVariable Long id,@RequestBody LinkTarget body) { links.add(type,id,body.type(),body.id()); return AjaxResult.success(); }
    @DeleteMapping("/resources/{type}/{id}/links/{targetType}/{targetId}") public AjaxResult unlink(@PathVariable String type,@PathVariable Long id,@PathVariable String targetType,@PathVariable Long targetId) { links.remove(type,id,targetType,targetId); return AjaxResult.success(); }
    @PostMapping("/resources/{type}/{id}/visit") public AjaxResult visit(@PathVariable String type,@PathVariable Long id) { links.visit(type,id); return AjaxResult.success(); }
    @PutMapping("/resources/{type}/{id}/inbox") public AjaxResult inbox(@PathVariable String type,@PathVariable Long id,@RequestParam boolean value) { links.inbox(type,id,value); return AjaxResult.success(); }
    public record TagSelection(List<Long> tagIds) {}
    public record ResourceRef(String type, Long id) {}
    public record BatchInbox(List<ResourceRef> resources,Boolean value) {}
    @PostMapping("/resources/inbox/batch") public AjaxResult inboxBatch(@RequestBody BatchInbox body) {
        if(body.resources()==null||body.resources().contains(null)||body.resources().size()>100||body.value()==null)throw new ServiceException("批量参数不正确");
        List<Map<String,Object>> results=new ArrayList<>();
        for(ResourceRef ref:body.resources())try{links.inbox(ref.type(),ref.id(),body.value());results.add(Map.of("type",ref.type(),"id",ref.id(),"status","success"));}
        catch(Exception ex){results.add(Map.of("type",ref.type()==null?"":ref.type(),"id",ref.id()==null?0:ref.id(),"status","fail"));}
        return AjaxResult.success(results);
    }
    public record BatchTags(List<ResourceRef> resources, List<Long> tagIds) {}
    @PostMapping("/resources/tags/batch")
    public AjaxResult batch(@RequestBody BatchTags body) {
        if (body.resources() == null || body.resources().size() > 100 || body.resources().contains(null) || body.tagIds() == null || body.tagIds().size() > 50)
            throw new ServiceException("单次最多处理 100 条资料、50 个标签");
        List<Map<String,Object>> results = new ArrayList<>();
        for (ResourceRef ref : body.resources()) {
            try {
                LinkedHashSet<Long> combined = new LinkedHashSet<>(tags.forResource(ref.type(),ref.id()));
                combined.addAll(body.tagIds());
                tags.replace(ref.type(),ref.id(),List.copyOf(combined));
                results.add(Map.of("type",ref.type(),"id",ref.id(),"status","success"));
            } catch (Exception ex) {
                results.add(Map.of("type",ref.type() == null ? "" : ref.type(),"id",ref.id() == null ? 0 : ref.id(),"status","fail","message","资料不可访问或标签无效"));
            }
        }
        return AjaxResult.success(results);
    }
    public record Merge(Long targetId) {}
    @GetMapping("/tags/audit/{type}") public AjaxResult audit(@PathVariable String type,@RequestParam(defaultValue="0") long after) { return AjaxResult.success(tags.audit(type,after)); }
    @GetMapping("/tags") public AjaxResult tags() { return AjaxResult.success(tags.list()); }
    @PostMapping("/tags") public AjaxResult save(@RequestBody ResourceTag tag) { return AjaxResult.success(tags.save(tag)); }
    @DeleteMapping("/tags/{id}") public AjaxResult remove(@PathVariable Long id) { tags.remove(id); return AjaxResult.success(); }
    @PostMapping("/tags/{id}/merge") public AjaxResult merge(@PathVariable Long id, @RequestBody Merge body) { tags.merge(id, body.targetId()); return AjaxResult.success(); }
    @GetMapping("/resources/{type}/{id}") public AjaxResult resource(@PathVariable String type, @PathVariable Long id) { return AjaxResult.success(access.read(type,id)); }
    @GetMapping("/resources/{type}/{id}/tags") public AjaxResult resourceTags(@PathVariable String type, @PathVariable Long id) { return AjaxResult.success(tags.forResource(type,id)); }
    @PutMapping("/resources/{type}/{id}/tags") public AjaxResult replace(@PathVariable String type, @PathVariable Long id, @RequestBody TagSelection body) { return AjaxResult.success(tags.replace(type,id,body.tagIds())); }
    @PostMapping("/tags/backfill/{type}") public AjaxResult backfill(@PathVariable String type) { return AjaxResult.success(tags.backfill(type)); }
}
