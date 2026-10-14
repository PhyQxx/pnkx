package com.pnkx.life.service.workspace;

import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.mapper.WorkspaceMapper;
import com.pnkx.life.service.NoteAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResourceAccessService {
    @Resource private WorkspaceMapper mapper;
    @Resource private DataPermissionService permissions;
    public ResourceRecord read(String type, Long id) {
        ResourceType.validate(type, id);
        String owner = NoteAccessService.currentUserId();
        List<Long> owners = type.equals("todo") ? permissions.getVisibleUserIds("todo") : List.of(Long.valueOf(owner));
        ResourceRecord row = mapper.resource(type, id, owners, owners == null, false);
        if (row == null) throw NoteAccessService.unavailable();
        return row;
    }
    public ResourceRecord owned(String type, Long id, boolean lock) {
        ResourceType.validate(type, id);
        ResourceRecord row = mapper.resource(type, id, List.of(Long.valueOf(NoteAccessService.currentUserId())), false, lock);
        if (row == null) throw NoteAccessService.unavailable();
        return row;
    }
}
