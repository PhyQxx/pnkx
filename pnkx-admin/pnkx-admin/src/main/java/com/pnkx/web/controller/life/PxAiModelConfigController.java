package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.ai.AiClient;
import com.pnkx.domain.po.PxAiModelConfig;
import com.pnkx.service.IPxAiModelConfigService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * AI模型配置管理
 *
 * @author PHY
 */
@RestController
@RequestMapping("/aiModel")
public class PxAiModelConfigController extends BaseController {

    @Resource
    private IPxAiModelConfigService pxAiModelConfigService;

    @Resource
    private AiClient aiClient;

    /**
     * 查询AI模型配置列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxAiModelConfig pxAiModelConfig) {
        startPage();
        List<PxAiModelConfig> list = pxAiModelConfigService.selectPxAiModelConfigList(pxAiModelConfig);
        return getDataTable(list);
    }

    /**
     * 获取默认模型
     */
    @GetMapping("/getDefault")
    public AjaxResult getDefault() {
        return AjaxResult.success(pxAiModelConfigService.getDefaultEnabledModel());
    }

    /**
     * 获取AI模型配置详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxAiModelConfigService.selectPxAiModelConfigById(id));
    }

    /**
     * 测试AI模型配置。编辑时未传API Key则使用已保存的密钥。
     */
    @PostMapping("/test")
    public AjaxResult test(@RequestBody PxAiModelConfig pxAiModelConfig) {
        if (pxAiModelConfig == null || StringUtils.isEmpty(pxAiModelConfig.getModelKey())
                || StringUtils.isEmpty(pxAiModelConfig.getBaseUrl())) {
            return AjaxResult.error("模型标识和API地址不能为空");
        }
        if (StringUtils.isEmpty(pxAiModelConfig.getApiKey()) && pxAiModelConfig.getId() != null) {
            PxAiModelConfig saved = pxAiModelConfigService.selectPxAiModelConfigById(pxAiModelConfig.getId());
            if (saved != null) {
                pxAiModelConfig.setApiKey(saved.getApiKey());
            }
        }
        if (StringUtils.isEmpty(pxAiModelConfig.getApiKey())) {
            return AjaxResult.error("API Key不能为空");
        }

        long start = System.currentTimeMillis();
        try {
            aiClient.testConnection(pxAiModelConfig);
            return AjaxResult.success("连接测试成功", System.currentTimeMillis() - start);
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String message = StringUtils.isEmpty(cause.getMessage()) ? "未知错误" : cause.getMessage();
            return AjaxResult.error("连接测试失败：" + message);
        }
    }

    /**
     * 新增AI模型配置
     */
    @Log(title = "AI模型配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxAiModelConfig pxAiModelConfig) {
        return toAjax(pxAiModelConfigService.insertPxAiModelConfig(pxAiModelConfig));
    }

    /**
     * 修改AI模型配置
     */
    @Log(title = "AI模型配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxAiModelConfig pxAiModelConfig) {
        return toAjax(pxAiModelConfigService.updatePxAiModelConfig(pxAiModelConfig));
    }

    /**
     * 删除AI模型配置
     */
    @Log(title = "AI模型配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxAiModelConfigService.deletePxAiModelConfigByIds(ids));
    }

    /**
     * 设为默认模型
     */
    @Log(title = "AI模型配置", businessType = BusinessType.UPDATE)
    @PutMapping("/setDefault/{id}")
    public AjaxResult setDefault(@PathVariable("id") Long id) {
        pxAiModelConfigService.setDefault(id);
        return AjaxResult.success();
    }
}
