package com.pnkx.web.controller.chat;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxCustomReplyRule;
import com.pnkx.service.IPxCustomReplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat/customReply")
public class PxCustomReplyController extends BaseController {

    @Resource
    private IPxCustomReplyService pxCustomReplyService;

    /**
     * 查询自定义回复规则列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxCustomReplyRule pxCustomReplyRule) {
        startPage();
        List<PxCustomReplyRule> list = pxCustomReplyService.selectPxCustomReplyRuleList(pxCustomReplyRule);
        return getDataTable(list);
    }

    /**
     * 导出自定义回复规则列表
     */
    @Log(title = "自定义回复规则", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxCustomReplyRule pxCustomReplyRule) {
        List<PxCustomReplyRule> list = pxCustomReplyService.selectPxCustomReplyRuleList(pxCustomReplyRule);
        ExcelUtil<PxCustomReplyRule> util = new ExcelUtil<PxCustomReplyRule>(PxCustomReplyRule. class);
        return util.exportExcel(list, "rule");
    }

    /**
     * 获取自定义回复规则详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxCustomReplyService.selectPxCustomReplyRuleById(id));
    }

    /**
     * 新增自定义回复规则
     */
    @Log(title = "自定义回复规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxCustomReplyRule pxCustomReplyRule) {
        return toAjax(pxCustomReplyService.insertPxCustomReplyRule(pxCustomReplyRule));
    }

    /**
     * 修改自定义回复规则
     */
    @Log(title = "自定义回复规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxCustomReplyRule pxCustomReplyRule) {
        return toAjax(pxCustomReplyService.updatePxCustomReplyRule(pxCustomReplyRule));
    }

    /**
     * 删除自定义回复规则
     */
    @Log(title = "自定义回复规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxCustomReplyService.deletePxCustomReplyRuleByIds(ids));
    }
}
