package com.pnkx.mapper;

import com.pnkx.domain.po.PxShoppingList;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 购物清单 Mapper
 *
 * @author PHY
 * @date 2026/07/05
 */
@Mapper
public interface PxShoppingListMapper {
    PxShoppingList selectPxShoppingListById(Long id);

    PxShoppingList selectByClientUuid(String clientUuid);

    List<PxShoppingList> selectPxShoppingListList(PxShoppingList pxShoppingList);

    int insertPxShoppingList(PxShoppingList pxShoppingList);

    int updatePxShoppingList(PxShoppingList pxShoppingList);

    int deletePxShoppingListById(Long id);

    int deletePxShoppingListByIds(Long[] ids);
}
