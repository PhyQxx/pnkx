package com.pnkx.life.service;

import com.pnkx.life.domain.po.PxNote;

/** Only contains a note already verified to belong to the current user. */
public class NoteRevisionConflictException extends RuntimeException {
    private final PxNote current;
    private final String errorCode;

    public NoteRevisionConflictException(PxNote current, boolean missingRevision) {
        super(missingRevision ? "缺少笔记修订号，请保留草稿并重新加载"
                : "笔记已在其他位置更新，请保留草稿后处理冲突");
        this.current = current;
        this.errorCode = missingRevision ? "NOTE_REVISION_REQUIRED" : "NOTE_REVISION_CONFLICT";
    }

    public PxNote getCurrent() { return current; }
    public String getErrorCode() { return errorCode; }
}
