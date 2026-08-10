package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxShoppingList;
import com.pnkx.service.IPxShoppingListService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxShoppingListController
 * @data 2026/07/05
 * @description 购物清单Controller
 */
@RestController
@RequestMapping("/shoppingList")
public class PxShoppingListController extends BaseController {
    @Resource
    private IPxShoppingListService pxShoppingListService;

    /**
     * 查询购物清单列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxShoppingList pxShoppingList) {
        startPage();
        List<PxShoppingList> list = pxShoppingListService.selectPxShoppingListList(pxShoppingList);
        return getDataTable(list);
    }

    /**
     * 获取购物清单详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxShoppingListService.selectPxShoppingListById(id));
    }

    /**
     * 新增购物清单
     */
    @Log(title = "购物清单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxShoppingList pxShoppingList) {
        int rows = pxShoppingListService.insertPxShoppingList(pxShoppingList);
        if (rows > 0) {
            return AjaxResult.success(pxShoppingList.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改购物清单
     */
    @Log(title = "购物清单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxShoppingList pxShoppingList) {
        return toAjax(pxShoppingListService.updatePxShoppingList(pxShoppingList));
    }

    /**
     * 删除购物清单
     */
    @Log(title = "购物清单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxShoppingListService.deletePxShoppingListByIds(ids));
    }
}
