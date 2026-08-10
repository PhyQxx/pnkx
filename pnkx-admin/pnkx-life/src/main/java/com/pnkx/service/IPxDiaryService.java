package com.pnkx.service;

import com.pnkx.domain.po.PxDiary;

import java.util.List;

/**
 * @author PHY
 * @classname IPxDiaryService
 * @data 2021/12/30 0030 17:53
 * @description 日记Service接口
 */
public interface IPxDiaryService {
    /**
     * 查询日记
     *
     * @param id 日记ID
     * @return 日记
     */
    public PxDiary selectPxDiaryById(Long id);

    /**
     * 查询日记列表
     *
     * @param pxDiary 日记
     * @return 日记集合
     */
    public List<PxDiary> selectPxDiaryList(PxDiary pxDiary);

    /**
     * 新增日记
     *
     * @param pxDiary 日记
     * @return 结果
     */
    public int insertPxDiary(PxDiary pxDiary);

    /**
     * 修改日记
     *
     * @param pxDiary 日记
     * @return 结果
     */
    public int updatePxDiary(PxDiary pxDiary);

    /**
     * 批量删除日记
     *
     * @param ids 需要删除的日记ID
     * @return 结果
     */
    public int deletePxDiaryByIds(Long[] ids);

    /**
     * 删除日记信息
     *
     * @param id 日记ID
     * @return 结果
     */
    public int deletePxDiaryById(Long id);

    /**
     * 全局检索日记
     * @param searchCode
     * @return
     */
    List<PxDiary> retrieval(String searchCode);

    /**
     * AI流式分析日记
     * @param isAll true=分析所有日记，false=分析本月日记
     * @param onChunk 接收流式文本块
     * @param onError 错误回调
     */
    void aiAnalysisStream(Boolean isAll, java.util.function.Consumer<String> onChunk, Runnable onError);
}
