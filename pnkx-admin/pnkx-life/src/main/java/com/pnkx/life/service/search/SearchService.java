package com.pnkx.life.service.search;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.mapper.ResourceSearchMapper;
import com.pnkx.life.service.NoteAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import com.pnkx.life.service.workspace.ResourceTag;
import com.pnkx.life.mapper.WorkspaceMapper;
import java.util.*;

@Service
public class SearchService {
    public static final List<String> TYPES = List.of("article", "todo", "diary", "note", "bookkeeping",
            "commemoration", "subscription", "recipe", "shopping", "book", "webArticle", "file");
    private static final Map<String, String> SHARED = Map.of("todo", "todo", "commemoration", "commemoration",
            "recipe", "", "shopping", "shopping");
    @Resource private ResourceSearchMapper mapper;
    @Resource private WorkspaceMapper workspaceMapper;
    @Resource private DataPermissionService permissions;

    public record Page(List<SearchResult> rows, long total, int pageNum, int pageSize, boolean hasMore) {}

    @Transactional(readOnly = true)
    public Page search(String q, String type, String startDate, String endDate, String tag, int page, int size) {
        SearchQuery query = prepare(q, type, startDate, endDate, tag, page, size);
        long total = mapper.count(query);
        List<SearchResult> rows = total == 0 ? List.of() : mapper.search(query);
        rows.forEach(this::decorate);
        return new Page(rows, total, page, size, (long) query.getOffset() + rows.size() < total);
    }

    @Transactional(readOnly = true)
    public Page workspace(String q, String type, String start, String end, List<Long> tagIds, String mode,
                          boolean descendants, boolean untagged, int page, int size) {
        return workspace(q,type,start,end,tagIds,mode,descendants,untagged,null,false,page,size);
    }

    @Transactional(readOnly = true)
    public Page workspace(String q, String type, String start, String end, List<Long> tagIds, String mode,
                          boolean descendants, boolean untagged, Boolean inbox, boolean recent, int page, int size) {
        return workspaceFiltered(q,type,start,end,tagIds,mode,descendants,untagged,inbox,recent,page,size,null,null);
    }

    @Transactional(readOnly=true)
    public Page workspaceFiltered(String q,String type,String start,String end,List<Long> tagIds,String mode,
            boolean descendants,boolean untagged,Boolean inbox,boolean recent,int page,int size,Boolean readFlag,Boolean starFlag) {
        if((readFlag!=null||starFlag!=null)&&!"webArticle".equals(type))throw new ServiceException("阅读与收藏筛选只适用于剪藏");
        if (type != null && !type.equals("all") && !Set.of("note", "todo", "webArticle", "file").contains(type))
            throw new ServiceException("资料中心只支持笔记、剪藏、待办和文件");
        if (!Set.of("all", "any").contains(mode) || (tagIds != null && tagIds.size() > 20)) throw new ServiceException("标签筛选参数不正确");
        SearchQuery query = prepare(q, type, start, end, null, page, size);
        if (type == null || type.equals("all")) query.setTypes(List.of("note", "webArticle", "todo", "file"));
        List<ResourceTag> tags = workspaceMapper.tags(query.getUserId());
        List<List<Long>> groups = new ArrayList<>();
        for (Long id : tagIds == null ? List.<Long>of() : new LinkedHashSet<>(tagIds)) {
            if (id == null || tags.stream().noneMatch(tag -> tag.getId().equals(id))) throw NoteAccessService.unavailable();
            Set<Long> group = new LinkedHashSet<>(); group.add(id);
            if (descendants) {
                boolean changed;
                do {
                    changed = false;
                    for (ResourceTag tag : tags) if (group.contains(tag.getParentId())) changed |= group.add(tag.getId());
                } while (changed);
            }
            groups.add(List.copyOf(group));
        }
        query.setReadFlag(readFlag==null?null:readFlag?1:0);query.setStarFlag(starFlag==null?null:starFlag?1:0);
        query.setTagGroups(groups); query.setTagMode(mode); query.setUntagged(untagged); query.setInbox(inbox); query.setRecent(recent);
        long total = mapper.count(query);
        List<SearchResult> rows = total == 0 ? List.of() : mapper.search(query);
        rows.forEach(this::decorate);
        return new Page(rows, total, page, size, (long)query.getOffset() + rows.size() < total);
    }

    public SearchQuery prepare(String q, String type, String startDate, String endDate, String tag, int page, int size) {
        if (page < 1 || page > 100000 || size < 1 || size > 100) throw new ServiceException("分页参数超出范围");
        String keyword = q == null ? "" : q.trim();
        String selectedTag = tag == null ? "" : tag.trim();
        if (keyword.length() > 200 || selectedTag.length() > 100 || selectedTag.contains(","))
            throw new ServiceException("搜索词或标签格式不正确");
        List<String> types = type == null || type.isBlank() || type.equals("all") ? TYPES : List.of(type);
        if (!TYPES.containsAll(types)) throw new ServiceException("不支持的资源类型");
        SearchQuery query = new SearchQuery();
        query.setUserId(NoteAccessService.currentUserId());
        query.setTypes(types);
        query.setKeyword(keyword.isEmpty() ? null : "%" + keyword.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_").toLowerCase(Locale.ROOT) + "%");
        query.setTag(selectedTag.isEmpty() ? null : selectedTag.toLowerCase(Locale.ROOT));
        try {
            LocalDate start = startDate == null || startDate.isBlank() ? null : LocalDate.parse(startDate);
            LocalDate end = endDate == null || endDate.isBlank() ? null : LocalDate.parse(endDate);
            if (start != null && end != null && start.isAfter(end)) throw new ServiceException("开始日期不能晚于结束日期");
            query.setStart(start == null ? null : start + " 00:00:00");
            query.setEnd(end == null ? null : end.plusDays(1) + " 00:00:00");
        } catch (java.time.DateTimeException ex) {
            throw new ServiceException("日期必须为有效的 yyyy-MM-dd");
        }
        Map<String, List<Long>> scopes = new HashMap<>();
        for (String selected : types) if (SHARED.containsKey(selected)) {
            scopes.put(selected, permissions.getVisibleUserIds(SHARED.get(selected)));
        }
        query.setScopes(scopes);
        query.setOffset((page - 1) * size);
        query.setLimit(size);
        return query;
    }

    private void decorate(SearchResult row) {
        String id = row.getId();
        String pc, app;
        switch (row.getType()) {
            case "article" -> { pc = "/blog/articledetails?adminArticleId=" + id; app = "/pages_blog/article/detail?id=" + id; }
            case "todo" -> { pc = "/mytool/todo?toDoId=" + id; app = "/pages_life/todo/edit?id=" + id; }
            case "diary" -> { pc = "/mytool/diary?diaryId=" + id; app = "/pages_life/diary/edit?id=" + id; }
            case "note" -> { pc = "/note?noteId=" + id; app = "/pages_life/note/detail?id=" + id; }
            case "bookkeeping" -> { pc = "/mytool/bookkeeping/record?recordId=" + id; app = "/pages_life/bookkeeping/record/index"; }
            case "commemoration" -> { pc = "/commemorationDay?id=" + id; app = "/pages_life/commemorationDay/add?id=" + id; }
            case "subscription" -> { pc = "/mytool/subscription?id=" + id; app = "/pages_life/subscription/index?highlight=" + id; }
            case "recipe" -> { pc = "/mytool/recipe?id=" + id; app = "/pages_life/recipe/detail?id=" + id; }
            case "shopping" -> { pc = "/mytool/shoppingList?id=" + id; app = "/pages_life/shoppingList/detail?id=" + id; }
            case "book" -> { pc = "/myapp/book?id=" + id; app = "/pages_life/book/detail?id=" + id; }
            case "file" -> { pc = "/resources/files?id=" + id; app = "/pages_life/workspace/files?id=" + id; }
            case "webArticle" -> { pc = "/myapp/webArticle?id=" + id; app = "/pages_life/webArticle/detail?id=" + id; }
            default -> throw new IllegalStateException("Unknown resource type");
        }
        row.setRoute(pc);
        row.setAppRoute(app);
        if (row.getTitle() == null || row.getTitle().isBlank()) row.setTitle("未命名记录");
    }
}
