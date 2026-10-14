package com.pnkx.life.service.workspace;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.mapper.OrganizeMapper;
import com.pnkx.life.mapper.WorkspaceMapper;
import com.pnkx.life.service.IPxNoteService;
import com.pnkx.life.service.NoteAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class OrganizeService {
    @Resource private com.pnkx.life.mapper.PxNoteMapper noteMapper;
    @Resource private WorkspaceMapper workspace;
    @Resource private OrganizeMapper mapper;
    @Resource private ResourceAccessService access;
    @Resource private ResourceLinkService links;
    @Resource private IPxNoteService notes;
    private String owner() { return NoteAccessService.currentUserId(); }
    private String fingerprint(ResourceRecord row,List<Long> tags) {
        try {
            // Length-prefixed fields avoid separator collisions in user-controlled content.
            StringBuilder data=new StringBuilder();
            for(Object value:Arrays.asList(row.getTitle(),row.getContent(),row.getRichText(),row.getTags(),row.getRevision(),row.getFetchStatus(),tags)) {
                String text=String.valueOf(value); data.append(text.length()).append(':').append(text);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data.toString().getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private void suggest(String owner,String type,ResourceRecord row,List<Long> tags,String rule,String reason,String title) {
        OrganizeSuggestion suggestion=new OrganizeSuggestion(); suggestion.setOwnerId(owner);suggestion.setResourceType(type);
        suggestion.setResourceId(row.getId());suggestion.setFingerprint(fingerprint(row,tags));suggestion.setTitle(row.getTitle());
        suggestion.setRuleCode(rule);suggestion.setReason(reason);suggestion.setProposedTitle(title);mapper.insert(suggestion);
    }
    @Transactional
    public Map<String,Object> scan(String type,long after) {
        ResourceType.validate(type,1L); if(after<0) throw new ServiceException("扫描游标不正确");
        String owner=owner(); workspace.ensureLock(owner); workspace.lockOwner(owner);
        List<ResourceRecord> rows=workspace.backfillRows(owner,type,after);
        Map<String,Long> bodies=new HashMap<>();
        for(ResourceRecord row:rows) {
            List<Long> tags=workspace.resourceTags(owner,type,row.getId());
            if(type.equals("webArticle") && "2".equals(row.getFetchStatus())) suggest(owner,type,row,tags,"fetch_failed","网页抓取失败；请核查错误原因后在剪藏页重试，不能据此认定链接永久失效",null);
            if(tags.isEmpty()) suggest(owner,type,row,tags,"untagged","尚未归入任何主题；加入收件箱后整理",null);
            String body=org.jsoup.Jsoup.parse(row.getContent()==null?"":row.getContent()).text().strip();
            if(body.isEmpty() && (type.equals("note") || type.equals("webArticle"))) suggest(owner,type,row,tags,"empty","没有正文；请人工核查。不会自动删除",null);
            if(type.equals("note") && (row.getTitle()==null || row.getTitle().isBlank()) && !body.isBlank())
                suggest(owner,type,row,tags,"title","使用正文前 80 字补充空白标题",body.substring(0,Math.min(body.length(),80)));
            if(!body.isEmpty() && (type.equals("note") || type.equals("webArticle"))) {
                Long previous=bodies.putIfAbsent(body,row.getId());
                if(previous!=null) suggest(owner,type,row,tags,"duplicate","本批正文与同类型资料 #"+previous+" 一致；仅提示核查，不自动合并",null);
            }
        }
        return Map.of("checked",rows.size(),"lastId",rows.isEmpty()?after:rows.getLast().getId(),"done",rows.size()<100,"coverage","每批最多 100 条，重复候选限本批正文比较");
    }
    public Map<String,Object> list(long after) {
        if(after<0) throw new ServiceException("游标不正确");
        List<OrganizeSuggestion> visible=new ArrayList<>();
        List<OrganizeSuggestion> scanned=mapper.list(owner(),after);
        for(OrganizeSuggestion suggestion:scanned) {
            try { access.owned(suggestion.getResourceType(),suggestion.getResourceId(),false);visible.add(suggestion); }
            catch(ServiceException ignored) { /* Never return stale titles after removal or loss of access. */ }
        }
        return Map.of("rows",visible,"nextAfter",scanned.isEmpty()?after:scanned.getLast().getId(),"hasMore",scanned.size()==100);
    }
    @Transactional
    public String apply(Long id,boolean ignore) {
        String owner=owner(); workspace.ensureLock(owner);workspace.lockOwner(owner);
        OrganizeSuggestion suggestion=mapper.lock(owner,id);
        if(suggestion==null) throw NoteAccessService.unavailable();
        if("note".equals(suggestion.getResourceType())){noteMapper.ensureSync(owner);noteMapper.lockSync(owner);}
        ResourceRecord row=access.owned(suggestion.getResourceType(),suggestion.getResourceId(),true);
        if(!"ready".equals(suggestion.getStatus())) return suggestion.getStatus();
        if(ignore) { mapper.status(owner,id,"ignored");return "ignored"; }
        if(!suggestion.getFingerprint().equals(fingerprint(row,workspace.resourceTags(owner,suggestion.getResourceType(),row.getId())))) {
            mapper.status(owner,id,"stale"); return "stale";
        }
        if("title".equals(suggestion.getRuleCode())) {
            PxNote note=new PxNote();note.setId(row.getId());note.setRevision(row.getRevision());note.setTitle(suggestion.getProposedTitle());notes.updatePxNote(note);
        } else links.inbox(suggestion.getResourceType(),row.getId(),true);
        mapper.status(owner,id,"applied"); return "applied";
    }
}
