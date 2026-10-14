package com.pnkx.life.service.workspace;

import com.pnkx.common.exception.ServiceException;
import java.util.Set;

public final class ResourceType {
    public static final Set<String> TYPES = Set.of("note", "webArticle", "todo", "file");
    private ResourceType() {}
    public static void validate(String type, Long id) {
        if (!TYPES.contains(type == null ? "" : type) || id == null || id <= 0)
            throw new ServiceException("资料类型或编号不正确");
    }
}
