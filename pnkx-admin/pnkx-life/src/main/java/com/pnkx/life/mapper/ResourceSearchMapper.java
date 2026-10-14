package com.pnkx.life.mapper;

import com.pnkx.life.service.search.SearchQuery;
import com.pnkx.life.service.search.SearchResult;
import java.util.List;

public interface ResourceSearchMapper {
    List<SearchResult> search(SearchQuery query);
    long count(SearchQuery query);
}
