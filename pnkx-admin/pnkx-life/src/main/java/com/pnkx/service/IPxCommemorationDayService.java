package com.pnkx.service;

import com.pnkx.domain.po.PxCommemorationDay;

import java.util.List;

/**
 * @author PHY
 * @classname IPxCommemorationDayService
 * @data 2021/11/29 16:32
 * @description 纪念日Service接口
 */
public interface IPxCommemorationDayService {
    /**
     * 查询纪念日
     *
     * @param id 纪念日ID
     * @return 纪念日
     */
    public PxCommemorationDay selectPxCommemorationDayById(Long id);

    /**
     * 查询纪念日列表
     *
     * @param pxCommemorationDay 纪念日
     * @return 纪念日集合
     */
    public List<PxCommemorationDay> selectPxCommemorationDayList(PxCommemorationDay pxCommemorationDay);

    /**
     * 新增纪念日
     *
     * @param pxCommemorationDay 纪念日
     * @return 结果
     */
    public int insertPxCommemorationDay(PxCommemorationDay pxCommemorationDay);

    /**
     * 修改纪念日
     *
     * @param pxCommemorationDay 纪念日
     * @return 结果
     */
    public int updatePxCommemorationDay(PxCommemorationDay pxCommemorationDay);

    /**
     * 批量删除纪念日
     *
     * @param ids 需要删除的纪念日ID
     * @return 结果
     */
    public int deletePxCommemorationDayByIds(Long[] ids);

    /**
     * 删除纪念日信息
     *
     * @param id 纪念日ID
     * @return 结果
     */
    public int deletePxCommemorationDayById(Long id);

    /**
     * APP首页查询纪念日列表
     * @param pxCommemorationDay
     * @return
     */
    List<PxCommemorationDay> getCommemorationDayList(PxCommemorationDay pxCommemorationDay);
}
