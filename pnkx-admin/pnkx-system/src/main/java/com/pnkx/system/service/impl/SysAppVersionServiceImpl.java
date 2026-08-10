package com.pnkx.system.service.impl;

import com.pnkx.system.domain.SysAppVersion;
import com.pnkx.system.mapper.SysAppVersionMapper;
import com.pnkx.system.service.ISysAppVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * App版本管理Service业务层处理
 *
 * @author pnkx
 * @date 2024-07-25
 */
@Service
public class SysAppVersionServiceImpl implements ISysAppVersionService {

    @Autowired
    private SysAppVersionMapper sysAppVersionMapper;

    @Override
    public SysAppVersion checkUpdate(String appType) {
        return sysAppVersionMapper.selectLatestVersionByAppType(appType);
    }

    @Override
    public List<SysAppVersion> selectSysAppVersionList(SysAppVersion sysAppVersion) {
        return sysAppVersionMapper.selectSysAppVersionList(sysAppVersion);
    }

    @Override
    public SysAppVersion selectSysAppVersionById(Long id) {
        return sysAppVersionMapper.selectSysAppVersionById(id);
    }

    @Override
    public int insertSysAppVersion(SysAppVersion sysAppVersion) {
        return sysAppVersionMapper.insertSysAppVersion(sysAppVersion);
    }

    @Override
    public int updateSysAppVersion(SysAppVersion sysAppVersion) {
        return sysAppVersionMapper.updateSysAppVersion(sysAppVersion);
    }

    @Override
    public int deleteSysAppVersionByIds(Long[] ids) {
        return sysAppVersionMapper.deleteSysAppVersionByIds(ids);
    }
}
