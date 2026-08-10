package com.pnkx.domain.vo;

import com.pnkx.domain.po.PxNoteFolder;
import lombok.Data;

import java.util.List;

/**
 * @author PHY
 * @classname PxNoteFolder
 * @data 2021/12/30 17:29
 * @description 笔记文件夹对象 px_note_folder
 */
@Data
public class PxNoteFolderVo extends PxNoteFolder
{
    private List<PxNoteFolderVo> children;

    public List<PxNoteFolderVo> getChildren() {
        return children;
    }
}
