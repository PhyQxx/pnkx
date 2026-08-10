package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxAiModelConfig;
import com.pnkx.mapper.PxAiModelConfigMapper;
import com.pnkx.service.IPxAiModelConfigService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * AI模型配置Service
 *
 * @author PHY
 */
@Service
public class PxAiModelConfigServiceImpl implements IPxAiModelConfigService {

    @Resource
    private PxAiModelConfigMapper pxAiModelConfigMapper;

    @Override
    public PxAiModelConfig selectPxAiModelConfigById(Long id) {
        return pxAiModelConfigMapper.selectPxAiModelConfigById(id);
    }

    @Override
    @DataScopeSelf
    public List<PxAiModelConfig> selectPxAiModelConfigList(PxAiModelConfig pxAiModelConfig) {
        return pxAiModelConfigMapper.selectPxAiModelConfigList(pxAiModelConfig);
    }


    @Override
    public int insertPxAiModelConfig(PxAiModelConfig pxAiModelConfig) {
        pxAiModelConfig.setCreateTime(DateUtils.getNowDate());
        pxAiModelConfig.setCreateBy(SecurityUtils.getUserId());
        pxAiModelConfig.setDelFlag(false);
        // 如果设为默认，先取消其他默认
        if ("1".equals(pxAiModelConfig.getIsDefault())) {
            pxAiModelConfigMapper.clearDefaultFlag();
        }
        return pxAiModelConfigMapper.insertPxAiModelConfig(pxAiModelConfig);
    }

    @Override
    public int updatePxAiModelConfig(PxAiModelConfig pxAiModelConfig) {
        pxAiModelConfig.setUpdateTime(DateUtils.getNowDate());
        pxAiModelConfig.setUpdateBy(SecurityUtils.getUserId());
        // 如果设为默认，先取消其他默认
        if ("1".equals(pxAiModelConfig.getIsDefault())) {
            pxAiModelConfigMapper.clearDefaultFlag();
        }
        return pxAiModelConfigMapper.updatePxAiModelConfig(pxAiModelConfig);
    }

    @Override
    public int deletePxAiModelConfigById(Long id) {
        return pxAiModelConfigMapper.deletePxAiModelConfigById(id);
    }

    @Override
    public int deletePxAiModelConfigByIds(Long[] ids) {
        return pxAiModelConfigMapper.deletePxAiModelConfigByIds(ids);
    }

    @Override
    public PxAiModelConfig getDefaultEnabledModel() {
        return pxAiModelConfigMapper.selectDefaultEnabledModel();
    }

    @Override
    public void setDefault(Long id) {
        pxAiModelConfigMapper.clearDefaultFlag();
        pxAiModelConfigMapper.setDefault(id);
    }
}
