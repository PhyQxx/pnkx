package com.pnkx.life.service.search;

import lombok.Data;
import java.util.List;
import java.util.Map;

/** Constructed by SearchService, never bound from a public request. */
@Data
public class SearchQuery {
    private String userId;
    private List<String> types;
    private Map<String, List<Long>> scopes;
    private String keyword;
    private String tag;
    private String start;
    private String end;
    private List<List<Long>> tagGroups;
    private String tagMode;
    private boolean untagged;
    private Integer readFlag;
    private Integer starFlag;
    private Boolean inbox;
    private boolean recent;
    private int offset;
    private int limit;
}
