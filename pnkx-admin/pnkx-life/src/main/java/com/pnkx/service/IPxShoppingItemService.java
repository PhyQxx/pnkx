package com.pnkx.service;

import com.pnkx.domain.po.PxShoppingItem;

import java.util.List;

/**
 * @author PHY
 * @classname IPxShoppingItemService
 * @data 2026/07/05
 * @description 购物条目Service接口
 */
public interface IPxShoppingItemService {
    /**
     * 查询购物条目
     *
     * @param id 购物条目ID
     * @return 购物条目
     */
    public PxShoppingItem selectPxShoppingItemById(Long id);

    /**
     * 查询购物条目列表
     *
     * @param pxShoppingItem 购物条目
     * @return 购物条目集合
     */
    public List<PxShoppingItem> selectPxShoppingItemList(PxShoppingItem pxShoppingItem);

    /**
     * 新增购物条目
     *
     * @param pxShoppingItem 购物条目
     * @return 结果
     */
    public int insertPxShoppingItem(PxShoppingItem pxShoppingItem);

    /**
     * 修改购物条目
     *
     * @param pxShoppingItem 购物条目
     * @return 结果
     */
    public int updatePxShoppingItem(PxShoppingItem pxShoppingItem);

    /**
     * 批量删除购物条目
     *
     * @param ids 需要删除的购物条目ID
     * @return 结果
     */
    public int deletePxShoppingItemByIds(Long[] ids);

    /**
     * 删除购物条目信息
     *
     * @param id 购物条目ID
     * @return 结果
     */
    public int deletePxShoppingItemById(Long id);

    /**
     * 清空已勾选条目
     *
     * @param listId 购物清单ID
     * @return 结果
     */
    public int clearChecked(Long listId);
}
