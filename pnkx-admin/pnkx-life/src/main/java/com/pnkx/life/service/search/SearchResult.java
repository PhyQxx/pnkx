package com.pnkx.life.service.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.util.Arrays;
import java.util.List;

@Data
public class SearchResult {
    private String type;
    private String id;
    private String title;
    private String subtitle;
    private String createdAt;
    private String updatedAt;
    @JsonIgnore
    private String rawTags;
    private String route;
    private String appRoute;
    public List<String> getTags() {
        return rawTags == null ? List.of() : Arrays.stream(rawTags.replace('，', ',').split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }
}
