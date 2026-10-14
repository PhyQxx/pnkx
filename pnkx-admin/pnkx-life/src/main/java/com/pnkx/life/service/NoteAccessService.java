package com.pnkx.life.service;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.domain.po.PxNoteFolder;
import com.pnkx.life.mapper.PxNoteFolderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 私有笔记权限；普通入口、目录树、AI 与离线写入共用。 */
@Service
public class NoteAccessService {
    @Resource
    private PxNoteFolderMapper folderMapper;

    public static String currentUserId() {
        String userId = SecurityUtils.getUserId();
        if (userId == null || userId.isBlank()) throw new ServiceException("请先登录");
        return userId;
    }

    public static ServiceException unavailable() {
        return new ServiceException("笔记或文件夹不存在或无权访问");
    }

    public PxNoteFolder requireFolder(Long folderId, String userId) {
        if (folderId == null || folderId == 0) return null;
        if (folderId < 0) throw unavailable();
        PxNoteFolder folder = folderMapper.selectPxNoteFolderById(folderId, userId);
        if (folder == null) throw unavailable();
        return folder;
    }

    /** 调用方在事务中锁定本用户目录后，检查移动不会生成环。 */
    public void requireValidParent(Long movingId, Long parentId, String userId) {
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0) {
            if (cursor.equals(movingId) || !visited.add(cursor)) {
                throw new ServiceException("不能将文件夹移入自身或子文件夹");
            }
            cursor = requireFolder(cursor, userId).getParentId();
        }
    }

    public static Long[] normalizeIds(Long[] ids) {
        if (ids == null || ids.length == 0 || ids.length > 500
                || Arrays.stream(ids).anyMatch(id -> id == null || id <= 0)) {
            throw new ServiceException("请选择有效的笔记或文件夹（最多500项）");
        }
        return Arrays.stream(ids).distinct().toArray(Long[]::new);
    }

    public static void requireOwnedBatch(Long[] ids, List<Long> ownedIds) {
        if (!new HashSet<>(ownedIds).containsAll(Arrays.asList(ids))) throw unavailable();
    }
}
