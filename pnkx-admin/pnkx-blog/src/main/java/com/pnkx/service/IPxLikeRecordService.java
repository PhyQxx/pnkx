package com.pnkx.service;

import com.pnkx.domain.po.PxLikeRecord;

import java.util.List;

/**
 * 点赞记录Service接口
 *
 * @author pnkx
 * @date 2023-08-25
 */
public interface IPxLikeRecordService {
    /**
     * 查询点赞记录列表
     *
     * @param pxLikeRecord 点赞记录
     * @return 点赞记录集合
     */
    public List<PxLikeRecord> selectPxLikeRecordList(PxLikeRecord pxLikeRecord);

    /**
     * 点赞
     * @param id 内容id
     * @param type 类型
     * @return
     */
    public boolean like(Long id, String type);
}
