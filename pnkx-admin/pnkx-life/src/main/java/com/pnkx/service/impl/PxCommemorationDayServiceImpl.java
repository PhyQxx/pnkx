package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.PxCommemorationDay;
import com.pnkx.mapper.PxCommemorationDayMapper;
import com.pnkx.service.IPxCommemorationDayService;
import com.pnkx.service.IPxLifeReminderService;
import com.pnkx.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.common.exception.ServiceException;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxCommemorationDayServiceImpl
 * @data 2021/11/29 16:33
 * @description 描述
 */
@Service
public class PxCommemorationDayServiceImpl implements IPxCommemorationDayService {
    @Resource
    private PxCommemorationDayMapper pxCommemorationDayMapper;
    @Resource
    private IPxLifeReminderService lifeReminderService;
    @Resource private DataPermissionService dataPermissionService;

    /**
     * 查询纪念日
     *
     * @param id 纪念日ID
     * @return 纪念日
     */
    @Override
    public PxCommemorationDay selectPxCommemorationDayById(Long id) {
        return pxCommemorationDayMapper.selectPxCommemorationDayById(id);
    }

    /**
     * 查询纪念日列表
     *
     * @param pxCommemorationDay 纪念日
     * @return 纪念日
     */
    @Override
    @DataScopeSelf(module = "commemoration")
    public List<PxCommemorationDay> selectPxCommemorationDayList(PxCommemorationDay pxCommemorationDay) {
        return pxCommemorationDayMapper.selectPxCommemorationDayList(pxCommemorationDay);
    }


    /**
     * 新增纪念日
     *
     * @param pxCommemorationDay 纪念日
     * @return 结果
     */
    @Override
    public int insertPxCommemorationDay(PxCommemorationDay pxCommemorationDay) {
        if (pxCommemorationDay.getCreateBy() == null) pxCommemorationDay.setCreateBy(SecurityUtils.getUserId());
        pxCommemorationDay.setCreateTime(DateUtils.getNowDate());
        return pxCommemorationDayMapper.insertPxCommemorationDay(pxCommemorationDay);
    }

    /**
     * 修改纪念日
     *
     * @param pxCommemorationDay 纪念日
     * @return 结果
     */
    @Override
    public int updatePxCommemorationDay(PxCommemorationDay pxCommemorationDay) {
        requireSharedWrite(pxCommemorationDayMapper.selectPxCommemorationDayById(pxCommemorationDay.getId()));
        pxCommemorationDay.setUpdateTime(DateUtils.getNowDate());
        return pxCommemorationDayMapper.updatePxCommemorationDay(pxCommemorationDay);
    }

    /**
     * 批量删除纪念日
     *
     * @param ids 需要删除的纪念日ID
     * @return 结果
     */
    @Override
    public int deletePxCommemorationDayByIds(Long[] ids) {
        for (Long id : ids) requireSharedWrite(pxCommemorationDayMapper.selectPxCommemorationDayById(id));
        int rows = pxCommemorationDayMapper.deletePxCommemorationDayByIds(ids);
        for (Long id : ids) lifeReminderService.unbindReminder("commemoration", id, SecurityUtils.getUserId());
        return rows;
    }

    /**
     * 删除纪念日信息
     *
     * @param id 纪念日ID
     * @return 结果
     */
    @Override
    public int deletePxCommemorationDayById(Long id) {
        requireSharedWrite(pxCommemorationDayMapper.selectPxCommemorationDayById(id));
        int rows = pxCommemorationDayMapper.deletePxCommemorationDayById(id);
        lifeReminderService.unbindReminder("commemoration", id, SecurityUtils.getUserId());
        return rows;
    }

    /**
     * APP首页查询纪念日列表
     * @param pxCommemorationDay
     * @return
     */
    @DataScopeSelf(module = "commemoration")
    @Override
    public List<PxCommemorationDay> getCommemorationDayList(PxCommemorationDay pxCommemorationDay) {
        return pxCommemorationDayMapper.getCommemorationDayList(pxCommemorationDay);
    }

    private void requireSharedWrite(PxCommemorationDay day) {
        if (day == null || !dataPermissionService.canWrite(day.getCreateBy(), "commemoration"))
            throw new ServiceException("记录不存在或无权操作");
    }
}
