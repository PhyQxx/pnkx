package com.pnkx.system.mapper;

import com.pnkx.system.domain.SysAppVersion;

import java.util.List;

/**
 * App版本管理Mapper接口
 *
 * @author pnkx
 * @date 2024-07-25
 */
public interface SysAppVersionMapper {

    /**
     * 查询最新的生效版本信息
     *
     * @param appType 平台
     * @return 最新的生效版本信息
     */
    SysAppVersion selectLatestVersionByAppType(String appType);

    /**
     * 查询App版本列表
     *
     * @param sysAppVersion App版本信息
     * @return App版本列表
     */
    List<SysAppVersion> selectSysAppVersionList(SysAppVersion sysAppVersion);

    /**
     * 查询App版本详情
     *
     * @param id App版本ID
     * @return App版本详情
     */
    SysAppVersion selectSysAppVersionById(Long id);

    /**
     * 新增App版本
     *
     * @param sysAppVersion App版本
     * @return 结果
     */
    int insertSysAppVersion(SysAppVersion sysAppVersion);

    /**
     * 修改App版本
     *
     * @param sysAppVersion App版本
     * @return 结果
     */
    int updateSysAppVersion(SysAppVersion sysAppVersion);

    /**
     * 批量删除App版本
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteSysAppVersionByIds(Long[] ids);
}
