package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxBookkeepingClassification;
import com.pnkx.service.IPxBookkeepingClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author by QXX
 * @classname PxAdminBookkeepingClassificationController
 * @date 2021-11-18 10.24
 * @description: 描述
 */
@RestController
@RequestMapping("/bookkeeping/classification")
public class PxBookkeepingClassificationController extends BaseController {
    @Autowired
    private IPxBookkeepingClassificationService pxBookkeepingClassificationService;

    /**
     * 查询账本分类列表
     */
    @GetMapping("/getClassificationList")
    public AjaxResult getClassificationList(PxBookkeepingClassification pxBookkeepingClassification) {
        pxBookkeepingClassification.setTypeLevel("0");
        // 查询一级分类
        List<PxBookkeepingClassification> primaryList = pxBookkeepingClassificationService.selectPxBookkeepingClassificationList(pxBookkeepingClassification);
        // 查询二级分类
        pxBookkeepingClassification.setTypeLevel("1");
        List<PxBookkeepingClassification> secondaryList = pxBookkeepingClassificationService.selectPxBookkeepingClassificationList(pxBookkeepingClassification);
        for (PxBookkeepingClassification primary : primaryList) {
            List<PxBookkeepingClassification> children = primary.getChildren();
            if (children == null) {
                children = new java.util.ArrayList<>();
            }
            for (PxBookkeepingClassification secondary : secondaryList) {
                if (primary.getId().longValue() == secondary.getTypeParentId().longValue()) {
                    children.add(secondary);
                }
            }
            primary.setChildren(children);
        }
        // 获取最近使用分类
        List<PxBookkeepingClassification> latelyTypeList = pxBookkeepingClassificationService.getLatelyTypeList(pxBookkeepingClassification);
        // 添加最近使用分类
        PxBookkeepingClassification latelyType = new PxBookkeepingClassification();
        latelyType.setTypeName("最近使用");
        latelyType.setTypeLevel("0");
        latelyType.setTypeIcon("最近");
        latelyType.setChildren(latelyTypeList);
        primaryList.add(0, latelyType);
        return AjaxResult.success(primaryList);
    }

    /**
     * 查询账本分类列表（分页）
     */
    @GetMapping("/list")
    public TableDataInfo list(PxBookkeepingClassification pxBookkeepingClassification) {
        startPage();
        List<PxBookkeepingClassification> list = pxBookkeepingClassificationService.selectPxBookkeepingClassificationList(pxBookkeepingClassification);
        return getDataTable(list);
    }

    /**
     * 获取账本分类详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxBookkeepingClassificationService.selectPxBookkeepingClassificationById(id));
    }

    /**
     * 新增账本分类
     */
    @Log(title = "新增账本分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxBookkeepingClassification pxBookkeepingClassification) {
        int rows = pxBookkeepingClassificationService.insertPxBookkeepingClassification(pxBookkeepingClassification);
        if (rows > 0) {
            return AjaxResult.success(pxBookkeepingClassification.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改账本分类
     */
    @Log(title = "修改账本分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxBookkeepingClassification pxBookkeepingClassification) {
        return toAjax(pxBookkeepingClassificationService.updatePxBookkeepingClassification(pxBookkeepingClassification));
    }

    /**
     * 删除账本分类
     */
    @Log(title = "账本分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(pxBookkeepingClassificationService.deletePxBookkeepingClassificationById(id));
    }
}
