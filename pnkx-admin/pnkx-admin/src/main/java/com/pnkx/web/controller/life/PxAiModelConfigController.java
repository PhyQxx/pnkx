package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
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
