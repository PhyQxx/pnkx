package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.ServletUtils;
import com.pnkx.domain.po.PxNote;
import com.pnkx.domain.po.PxNoteFolder;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.mapper.PxNoteMapper;
import com.pnkx.service.IPxNoteService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxNoteServiceImpl
 * @data 2021/12/30 17:35
 * @description 笔记Service业务层处理
 */
@Service
public class PxNoteServiceImpl implements IPxNoteService {
    @Resource
    private PxNoteMapper pxNoteMapper;

    /**
     * 查询笔记
     *
     * @param id 笔记ID
     * @return 笔记
     */
    @Override
    public PxNote selectPxNoteById(Long id) {
        return pxNoteMapper.selectPxNoteById(id);
    }

    /**
     * 查询笔记列表
     *
     * @param pxNote 笔记
     * @return 笔记
     */
    @Override
    @DataScopeSelf
    public List<PxNoteFolder> selectPxNoteList(PxNote pxNote) {
        return pxNoteMapper.selectPxNoteList(pxNote);
    }


    /**
     * 新增笔记
     *
     * @param pxNote 笔记
     * @return 结果
     */
    @Override
    public PxNote insertPxNote(PxNote pxNote) {
        pxNote.setCreateTime(DateUtils.getNowDate());
        pxNote.setCreateBy(SecurityUtils.getUserId());
        pxNote.setUpdateTime(DateUtils.getNowDate());
        pxNote.setUpdateBy(SecurityUtils.getUserId());
        pxNoteMapper.insertPxNote(pxNote);
        return pxNote;
    }

    /**
     * 修改笔记
     *
     * @param pxNote 笔记
     * @return 结果
     */
    @Override
    public PxNote updatePxNote(PxNote pxNote) {
        pxNote.setUpdateTime(DateUtils.getNowDate());
        pxNote.setUpdateBy(SecurityUtils.getUserId());
        pxNoteMapper.updatePxNote(pxNote);
        return pxNote;
    }

    /**
     * 批量删除笔记
     *
     * @param ids 需要删除的笔记ID
     * @return 结果
     */
    @Override
    public int deletePxNoteByIds(Long[] ids) {
        return pxNoteMapper.deletePxNoteByIds(ids);
    }

    /**
     * 删除笔记信息
     *
     * @param id 笔记ID
     * @return 结果
     */
    @Override
    public int deletePxNoteById(Long id) {
        return pxNoteMapper.deletePxNoteById(id);
    }
}
