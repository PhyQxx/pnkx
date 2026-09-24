package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.ServletUtils;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.mapper.PxMenstruationRecordMapper;
import com.pnkx.service.IPxMenstruationRecordService;
import com.pnkx.service.IPxLifeReminderService;
import com.pnkx.domain.po.PxMenstruationRecord;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 姨妈记录Service业务层处理
 *
 * @author pnkx
 * @date 2021-12-03
 */
@Service
public class PxMenstruationRecordServiceImpl implements IPxMenstruationRecordService {
    @Resource
    private PxMenstruationRecordMapper pxMenstruationRecordMapper;

    @Resource
    private DataPermissionService dataPermissionService;
    @Resource
    private IPxLifeReminderService lifeReminderService;

    /**
     * 查询姨妈记录
     *
     * @param id 姨妈记录ID
     * @return 姨妈记录
     */
    @Override
    public PxMenstruationRecord selectPxMenstruationRecordById(Long id) {
        PxMenstruationRecord record = pxMenstruationRecordMapper.selectPxMenstruationRecordById(id);
        // 数据归属校验：非管理员只能查看自己/群组成员的数据
        if (record != null && record.getCreateBy() != null) {
            List<Long> visibleUserIds = dataPermissionService.getVisibleUserIds();
            if (visibleUserIds != null && !visibleUserIds.contains(Long.valueOf(record.getCreateBy()))) {
                return null;
            }
        }
        return record;
    }

    /**
     * 查询姨妈记录列表
     *
     * @param pxMenstruationRecord 姨妈记录
     * @return 姨妈记录
     */
    @DataScopeSelf(onlySelf = true)
    @Override
    public List<PxMenstruationRecord> selectPxMenstruationRecordList(PxMenstruationRecord pxMenstruationRecord) {
        pxMenstruationRecord.setUserId(Long.valueOf(SecurityUtils.getUserId()));
        return pxMenstruationRecordMapper.selectPxMenstruationRecordList(pxMenstruationRecord);
    }


    /**
     * 查询姨妈记录列表
     *
     * @param pxMenstruationRecord 姨妈记录
     * @return 姨妈记录
     */
    @DataScopeSelf(onlySelf = true)
    @Override
    public List<PxMenstruationRecord> getPxMenstruationRecordList(PxMenstruationRecord pxMenstruationRecord) {
        pxMenstruationRecord.setUserId(Long.valueOf(SecurityUtils.getUserId()));
        return pxMenstruationRecordMapper.getPxMenstruationRecordList(pxMenstruationRecord);
    }


    /**
     * 新增姨妈记录
     *
     * @param pxMenstruationRecord 姨妈记录
     * @return 结果
     */
    @Override
    public int insertPxMenstruationRecord(PxMenstruationRecord pxMenstruationRecord) {
        if (StringUtils.isNotEmpty(pxMenstruationRecord.getClientUuid())) {
            PxMenstruationRecord existing = pxMenstruationRecordMapper.selectByClientUuid(pxMenstruationRecord.getClientUuid());
            if (existing != null) {
                pxMenstruationRecord.setId(existing.getId());
                return 1;
            }
        }
        pxMenstruationRecord.setCreateTime(DateUtils.getNowDate());
        pxMenstruationRecord.setCreateBy(SecurityUtils.getUserId());
        return pxMenstruationRecordMapper.insertPxMenstruationRecord(pxMenstruationRecord);
    }

    /**
     * 修改姨妈记录
     *
     * @param pxMenstruationRecord 姨妈记录
     * @return 结果
     */
    @Override
    public int updatePxMenstruationRecord(PxMenstruationRecord pxMenstruationRecord) {
        requireOwner(pxMenstruationRecordMapper.selectPxMenstruationRecordById(pxMenstruationRecord.getId()));
        pxMenstruationRecord.setUpdateTime(DateUtils.getNowDate());
        pxMenstruationRecord.setUpdateBy(SecurityUtils.getUserId());
        return pxMenstruationRecordMapper.updatePxMenstruationRecord(pxMenstruationRecord);
    }

    /**
     * 批量删除姨妈记录
     *
     * @param ids 需要删除的姨妈记录ID
     * @return 结果
     */
    @Override
    public int deletePxMenstruationRecordByIds(Long[] ids) {
        for (Long id : ids) requireOwner(pxMenstruationRecordMapper.selectPxMenstruationRecordById(id));
        int rows = pxMenstruationRecordMapper.deletePxMenstruationRecordByIds(ids);
        for (Long id : ids) lifeReminderService.unbindReminder("menstruation", id, SecurityUtils.getUserId());
        return rows;
    }

    /**
     * 删除姨妈记录信息
     *
     * @param id 姨妈记录ID
     * @return 结果
     */
    @Override
    public int deletePxMenstruationRecordById(Long id) {
        requireOwner(pxMenstruationRecordMapper.selectPxMenstruationRecordById(id));
        int rows = pxMenstruationRecordMapper.deletePxMenstruationRecordById(id);
        lifeReminderService.unbindReminder("menstruation", id, SecurityUtils.getUserId());
        return rows;
    }

    private void requireOwner(PxMenstruationRecord entity) {
        if (entity == null || !Objects.equals(SecurityUtils.getUserId(), entity.getCreateBy())) {
            throw new ServiceException("记录不存在或无权操作");
        }
    }

    /**
     * APP首页获取姨妈提醒列表
     * @param pxMenstruationRecord
     * @return
     */
    @DataScopeSelf(onlySelf = true)
    @Override
    public List<PxMenstruationRecord> selectMenstruationRecordList(PxMenstruationRecord pxMenstruationRecord) {
        return pxMenstruationRecordMapper.selectMenstruationRecordList(pxMenstruationRecord);
    }

    /**
     * 获取最后一次姨妈开始的记录（受数据权限限制）
     *
     * @return 最近一次姨妈开始记录
     */
    @Override
    public PxMenstruationRecord getLastStartDate() {
        PxMenstruationRecord query = new PxMenstruationRecord();
        List<Long> visibleUserIds = dataPermissionService.getVisibleUserIds();
        if (visibleUserIds == null) {
            query.getParams().put(DataScopeSelf.SCOPE_ALL, true);
        } else {
            query.getParams().put(DataScopeSelf.SCOPE_ALL, false);
            query.getParams().put(DataScopeSelf.SCOPE_USER_IDS, visibleUserIds);
        }
        return pxMenstruationRecordMapper.getLastStartDate(query);
    }
}
