package com.pnkx.life.service.workspace;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.mapper.WorkspaceMapper;
import com.pnkx.life.mapper.PxWebArticleMapper;
import com.pnkx.life.domain.po.PxWebArticle;
import com.pnkx.life.service.NoteAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.*;

@Service
public class ResourceTagService {
    @Resource private WorkspaceMapper mapper;
    @Resource private PxWebArticleMapper articleMapper;
    @Resource private ResourceAccessService access;
    private String actor() { return NoteAccessService.currentUserId(); }
    private void lock(String owner) { mapper.ensureLock(owner); mapper.lockOwner(owner); }
    public List<ResourceTag> list() { return mapper.tags(actor()); }
    public List<Long> forResource(String type, Long id) {
        access.read(type, id);
        return mapper.resourceTags(actor(), type, id);
    }
    private String name(String value) {
        String result = Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFKC);
        if (result.isBlank() || result.length() > 100 || result.contains(",") || result.contains("，") || result.chars().anyMatch(Character::isISOControl))
            throw new ServiceException("标签名需为 1–100 字，不能含逗号或控制字符");
        return result;
    }
    private ResourceTag require(List<ResourceTag> tags, Long id) {
        return tags.stream().filter(t -> t.getId().equals(id)).findFirst().orElseThrow(NoteAccessService::unavailable);
    }
    private void parent(List<ResourceTag> tags, Long id, Long parentId) {
        Set<Long> seen = new HashSet<>();
        for (Long cursor = parentId; cursor != null && cursor != 0;) {
            if (cursor.equals(id) || !seen.add(cursor)) throw new ServiceException("标签不能移动到自身或其子标签下");
            cursor = require(tags, cursor).getParentId();
        }
    }
    @Transactional
    public ResourceTag save(ResourceTag input) {
        String owner = actor(); lock(owner);
        List<ResourceTag> tags = mapper.tags(owner);
        String title = name(input.getName());
        String normalized = title.toLowerCase(Locale.ROOT);
        ResourceTag existing = input.getId() == null ? null : require(tags, input.getId());
        if (tags.stream().anyMatch(t -> t.getNormalizedName().equals(normalized) && !t.getId().equals(input.getId())))
            throw new ServiceException("已有同名标签，请使用合并功能");
        if (existing != null && !Objects.equals(existing.getRevision(), input.getRevision())) throw new ServiceException("标签已更新，请刷新后重试");
        input.setOwnerId(owner); input.setName(title); input.setNormalizedName(normalized);
        if (input.getParentId() == null) input.setParentId(0L);
        parent(tags, input.getId(), input.getParentId());
        if (input.getColor() == null) input.setColor("#409EFF");
        if (!input.getColor().matches("#[0-9a-fA-F]{6}")) throw new ServiceException("标签颜色格式不正确");
        if (existing == null) mapper.insertTag(input);
        else {
            if (mapper.updateTag(input) != 1) throw new ServiceException("标签已更新，请刷新后重试");
            for (Map<String,Object> resource : mapper.taggedResources(owner, input.getId())) project(owner, resource.get("type").toString(), ((Number)resource.get("id")).longValue());
        }
        return require(mapper.tags(owner), input.getId());
    }
    @Transactional
    public List<Long> replace(String type, Long id, List<Long> tagIds) {
        String owner = actor(); lock(owner);
        access.owned(type, id, true);
        Set<Long> unique = checkedIds(tagIds);
        List<ResourceTag> tags = mapper.tags(owner);
        unique.forEach(tagId -> require(tags, tagId));
        mapper.clearResourceTags(owner, type, id);
        unique.forEach(tagId -> mapper.addRelation(owner, type, id, tagId));
        project(owner, type, id);
        return List.copyOf(unique);
    }
    private Set<Long> checkedIds(List<Long> ids) {
        if (ids == null || ids.size() > 50 || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new ServiceException("标签数量不能超过 50");
        return new LinkedHashSet<>(ids);
    }
    private void project(String owner, String type, Long id) {
        if (type.equals("note")) return;
        Set<Long> ids = new HashSet<>(mapper.resourceTags(owner, type, id));
        String text = String.join(",", mapper.tags(owner).stream().filter(t -> ids.contains(t.getId())).map(ResourceTag::getName).toList());
        if (text.length() > (type.equals("todo") ? 200 : 500)) throw new ServiceException("标签总长度超过旧接口兼容限制，请减少标签");
        mapper.projectTags(owner, type, id, text);
    }
    @Transactional
    public void remove(Long id) {
        String owner = actor(); lock(owner);
        List<ResourceTag> tags = mapper.tags(owner);
        ResourceTag tag = require(tags, id);
        List<Map<String,Object>> resources = mapper.taggedResources(owner, id);
        for (ResourceTag child : tags) if (Objects.equals(child.getParentId(), id)) {
            child.setParentId(tag.getParentId()); mapper.updateTag(child);
        }
        mapper.clearTagRelations(owner, id); mapper.deleteTag(owner, id);
        for (Map<String,Object> resource : resources) project(owner, resource.get("type").toString(), ((Number)resource.get("id")).longValue());
    }
    @Transactional
    public void merge(Long sourceId, Long targetId) {
        if (Objects.equals(sourceId, targetId)) throw new ServiceException("不能合并到自身");
        String owner = actor(); lock(owner);
        List<ResourceTag> tags = mapper.tags(owner);
        ResourceTag source = require(tags, sourceId);
        require(tags, targetId);
        List<Map<String,Object>> resources = mapper.taggedResources(owner, sourceId);
        // Move source children to its parent first; this also handles merging into a descendant.
        for (ResourceTag child : tags) if (Objects.equals(child.getParentId(), sourceId)) {
            child.setParentId(source.getParentId()); mapper.updateTag(child);
        }
        for (Map<String,Object> resource : resources) mapper.addRelation(owner, resource.get("type").toString(), ((Number)resource.get("id")).longValue(), targetId);
        mapper.clearTagRelations(owner, sourceId); mapper.deleteTag(owner, sourceId);
        for (Map<String,Object> resource : resources) project(owner, resource.get("type").toString(), ((Number)resource.get("id")).longValue());
    }

    /** Internal compatibility hook. owner must come from the persisted business row, never a request DTO. */
    @Transactional
    public void syncLegacy(String owner, String type, Long id, String text) {
        ResourceType.validate(type, id);
        if (type.equals("note")) return;
        lock(owner);
        ResourceRecord resource = mapper.resource(type, id, List.of(Long.valueOf(owner)), false, true);
        if (resource == null) throw NoteAccessService.unavailable();
        List<ResourceTag> existing = mapper.tags(owner);
        Set<Long> ids = new LinkedHashSet<>();
        for (String raw : (text == null ? "" : text).replace('，', ',').split(",")) {
            if (raw.isBlank()) continue;
            String title = name(raw), normalized = title.toLowerCase(Locale.ROOT);
            ResourceTag tag = existing.stream().filter(t -> t.getNormalizedName().equals(normalized)).findFirst().orElse(null);
            if (tag == null) {
                tag = new ResourceTag(); tag.setOwnerId(owner); tag.setName(title); tag.setNormalizedName(normalized);
                tag.setParentId(0L); tag.setColor("#409EFF"); mapper.insertTag(tag); existing.add(tag);
            }
            ids.add(tag.getId());
        }
        if (ids.size() > 50) throw new ServiceException("标签数量不能超过 50");
        mapper.clearResourceTags(owner, type, id);
        ids.forEach(tagId -> mapper.addRelation(owner, type, id, tagId));
        project(owner, type, id);
    }
    @Transactional
    public void applyArticleAnalysis(PxWebArticle result) {
        PxWebArticle row = articleMapper.selectPxWebArticleById(result.getId(), null);
        if (row == null) return;
        lock(row.getCreateBy());
        ResourceRecord current = mapper.resource("webArticle", row.getId(), List.of(Long.valueOf(row.getCreateBy())), false, true);
        if (current == null) return;
        articleMapper.updateAiResult(result);
        if (result.getTags() != null) syncLegacy(row.getCreateBy(), "webArticle", row.getId(), result.getTags());
    }

    @Transactional
    public Map<String,Object> backfill(String type) {
        if (!Set.of("todo", "webArticle").contains(type)) throw new ServiceException("不支持的旧标签类型");
        String owner = actor(); lock(owner);
        Long after = mapper.checkpoint(owner, type);
        List<ResourceRecord> rows = mapper.backfillRows(owner, type, after == null ? 0 : after);
        for (ResourceRecord row : rows) syncLegacy(owner, type, row.getId(), row.getTags());
        long last = rows.isEmpty() ? after == null ? 0 : after : rows.getLast().getId();
        mapper.checkpointSave(owner, type, last, rows.size() < 100);
        return Map.of("processed", rows.size(), "lastId", last, "done", rows.size() < 100);
    }
    /** Bounded reconciliation report. Does not mutate either representation. */
    @Transactional(readOnly=true)
    public Map<String,Object> audit(String type,long after) {
        if (!Set.of("todo","webArticle").contains(type) || after<0) throw new ServiceException("对账参数不正确");
        String owner=actor();
        List<ResourceRecord> rows=mapper.auditRows(owner,type,after);
        List<ResourceTag> all=mapper.tags(owner);
        List<Map<String,Object>> differences=new ArrayList<>();
        for(ResourceRecord row:rows) {
            Set<String> legacy=new java.util.TreeSet<>();
            for(String value:(row.getTags()==null?"":row.getTags()).replace('，',',').split(",")) if(!value.isBlank()) legacy.add(name(value).toLowerCase(Locale.ROOT));
            Set<Long> ids=new HashSet<>(mapper.resourceTags(owner,type,row.getId()));
            Set<String> unified=new java.util.TreeSet<>();
            for(ResourceTag tag:all) if(ids.contains(tag.getId())) unified.add(tag.getNormalizedName());
            if(!legacy.equals(unified)) differences.add(Map.of("id",row.getId().toString(),"legacy",legacy,"unified",unified));
        }
        return Map.of("checked",rows.size(),"differences",differences,"lastId",rows.isEmpty()?after:rows.getLast().getId(),"done",rows.size()<100);
    }

}
