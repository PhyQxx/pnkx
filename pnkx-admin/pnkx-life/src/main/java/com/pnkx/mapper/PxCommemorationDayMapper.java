package com.pnkx.mapper;

import com.pnkx.domain.po.PxCommemorationDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author PHY
 * @classname PxCommemorationDayMapper
 * @data 2021/11/29 16:30
 * @description 纪念日
 */
@Mapper
public interface PxCommemorationDayMapper {
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
     * 删除纪念日
     *
     * @param id 纪念日ID
     * @return 结果
     */
    public int deletePxCommemorationDayById(Long id);

    /**
     * 批量删除纪念日
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxCommemorationDayByIds(Long[] ids);

    /**
     * APP首页查询纪念日列表
     * @param pxCommemorationDay
     * @return
     */
    List<PxCommemorationDay> getCommemorationDayList(PxCommemorationDay pxCommemorationDay);

    /**
     * 根据客户端唯一标识查询纪念日（幂等去重）
     */
    PxCommemorationDay selectByClientUuid(@Param("clientUuid") String clientUuid);

    /**
     * 增量查询纪念日（离线同步用）
     */
    List<PxCommemorationDay> selectIncremental(@Param("createBy") String createBy, @Param("since") String since, @Param("offset") int offset, @Param("limit") int limit);
}
