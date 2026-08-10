package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxShoppingItem;
import com.pnkx.service.IPxShoppingItemService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxShoppingItemController
 * @data 2026/07/05
 * @description 购物条目Controller
 */
@RestController
@RequestMapping("/shoppingItem")
public class PxShoppingItemController extends BaseController {
    @Resource
    private IPxShoppingItemService pxShoppingItemService;

    /**
     * 查询购物条目列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxShoppingItem pxShoppingItem) {
        startPage();
        List<PxShoppingItem> list = pxShoppingItemService.selectPxShoppingItemList(pxShoppingItem);
        return getDataTable(list);
    }

    /**
     * 获取购物条目详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxShoppingItemService.selectPxShoppingItemById(id));
    }

    /**
     * 新增购物条目
     */
    @Log(title = "购物条目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxShoppingItem pxShoppingItem) {
        int rows = pxShoppingItemService.insertPxShoppingItem(pxShoppingItem);
        if (rows > 0) {
            return AjaxResult.success(pxShoppingItem.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改购物条目
     */
    @Log(title = "购物条目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxShoppingItem pxShoppingItem) {
        return toAjax(pxShoppingItemService.updatePxShoppingItem(pxShoppingItem));
    }

    /**
     * 删除购物条目
     */
    @Log(title = "购物条目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxShoppingItemService.deletePxShoppingItemByIds(ids));
    }

    /**
     * 清空已勾选条目
     */
    @Log(title = "购物条目", businessType = BusinessType.DELETE)
    @DeleteMapping("/clearChecked/{listId}")
    public AjaxResult clearChecked(@PathVariable("listId") Long listId) {
        return toAjax(pxShoppingItemService.clearChecked(listId));
    }
}
