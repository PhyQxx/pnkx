package com.pnkx.mapper;

import com.pnkx.domain.po.PxAiModelConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI模型配置Mapper
 *
 * @author PHY
 */
@Mapper
public interface PxAiModelConfigMapper {

    /**
     * 查询AI模型配置
     *
     * @param id AI模型配置主键
     * @return AI模型配置
     */
    PxAiModelConfig selectPxAiModelConfigById(Long id);

    /**
     * 查询AI模型配置列表
     *
     * @param pxAiModelConfig AI模型配置
     * @return AI模型配置集合
     */
    List<PxAiModelConfig> selectPxAiModelConfigList(PxAiModelConfig pxAiModelConfig);

    /**
     * 新增AI模型配置
     *
     * @param pxAiModelConfig AI模型配置
     * @return 结果
     */
    int insertPxAiModelConfig(PxAiModelConfig pxAiModelConfig);

    /**
     * 修改AI模型配置
     *
     * @param pxAiModelConfig AI模型配置
     * @return 结果
     */
    int updatePxAiModelConfig(PxAiModelConfig pxAiModelConfig);

    /**
     * 删除AI模型配置
     *
     * @param id AI模型配置主键
     * @return 结果
     */
    int deletePxAiModelConfigById(Long id);

    /**
     * 批量删除AI模型配置
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deletePxAiModelConfigByIds(Long[] ids);

    /**
     * 获取默认启用的模型
     *
     * @return 默认模型
     */
    PxAiModelConfig selectDefaultEnabledModel();

    /**
     * 取消所有默认标识
     */
    void clearDefaultFlag();

    /**
     * 设置默认模型
     *
     * @param id 模型ID
     */
    void setDefault(Long id);
}
