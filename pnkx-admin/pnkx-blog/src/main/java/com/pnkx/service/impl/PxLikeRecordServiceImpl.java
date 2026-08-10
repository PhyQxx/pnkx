package com.pnkx.service.impl;

import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxLikeRecord;
import com.pnkx.mapper.PxLikeRecordMapper;
import com.pnkx.service.IPxLikeRecordService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 点赞记录Service业务层处理
 *
 * @author pnkx
 * @date 2023-08-25
 */
@Service
public class PxLikeRecordServiceImpl implements IPxLikeRecordService {

    @Resource
    private PxLikeRecordMapper pxLikeRecordMapper;

    /**
     * 查询点赞记录列表
     *
     * @param pxLikeRecord 点赞记录
     * @return 点赞记录
     */
    @Override
    public List<PxLikeRecord> selectPxLikeRecordList(PxLikeRecord pxLikeRecord) {
        return pxLikeRecordMapper.selectPxLikeRecordList(pxLikeRecord);
    }

    /**
     * 文章点赞
     *
     * @param id 文章id
     * @return 结果
     */
    @Override
    public boolean like(Long id, String type) {
        PxLikeRecord pxLikeRecord = new PxLikeRecord();
        pxLikeRecord.setItemId(id);
        pxLikeRecord.setType(type);
        PxLikeRecord record = pxLikeRecordMapper.selectPxLikeRecord(pxLikeRecord);
        if (StringUtils.isNotNull(record)) {
            pxLikeRecordMapper.deletePxLikeRecordById(record.getId());
        } else {
            pxLikeRecord.setCreateBy(SecurityUtils.getUserId());
            pxLikeRecord.setCreateTime(DateUtils.getNowDate());
            pxLikeRecordMapper.insertPxLikeRecord(pxLikeRecord);
        }
        return true;
    }
}
