package com.pnkx.service;

import com.pnkx.domain.po.PxShoppingList;

import java.util.List;

/**
 * @author PHY
 * @classname IPxShoppingListService
 * @data 2026/07/05
 * @description 购物清单Service接口
 */
public interface IPxShoppingListService {
    /**
     * 查询购物清单
     *
     * @param id 购物清单ID
     * @return 购物清单
     */
    public PxShoppingList selectPxShoppingListById(Long id);

    /**
     * 查询购物清单列表
     *
     * @param pxShoppingList 购物清单
     * @return 购物清单集合
     */
    public List<PxShoppingList> selectPxShoppingListList(PxShoppingList pxShoppingList);

    /**
     * 新增购物清单
     *
     * @param pxShoppingList 购物清单
     * @return 结果
     */
    public int insertPxShoppingList(PxShoppingList pxShoppingList);

    /**
     * 修改购物清单
     *
     * @param pxShoppingList 购物清单
     * @return 结果
     */
    public int updatePxShoppingList(PxShoppingList pxShoppingList);

    /**
     * 批量删除购物清单
     *
     * @param ids 需要删除的购物清单ID
     * @return 结果
     */
    public int deletePxShoppingListByIds(Long[] ids);

    /**
     * 删除购物清单信息
     *
     * @param id 购物清单ID
     * @return 结果
     */
    public int deletePxShoppingListById(Long id);
}
