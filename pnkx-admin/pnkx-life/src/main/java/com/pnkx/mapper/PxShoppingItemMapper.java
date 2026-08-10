package com.pnkx.mapper;

import com.pnkx.domain.po.PxShoppingItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物条目 Mapper
 *
 * @author PHY
 * @date 2026/07/05
 */
@Mapper
public interface PxShoppingItemMapper {
    PxShoppingItem selectPxShoppingItemById(Long id);

    List<PxShoppingItem> selectPxShoppingItemList(PxShoppingItem pxShoppingItem);

    int insertPxShoppingItem(PxShoppingItem pxShoppingItem);

    int insertBatch(@Param("list") List<PxShoppingItem> list);

    int updatePxShoppingItem(PxShoppingItem pxShoppingItem);

    int deletePxShoppingItemById(Long id);

    int deletePxShoppingItemByIds(Long[] ids);

    int deleteByListId(@Param("listId") Long listId);

    /** 清空已勾选的条目 */
    int clearChecked(@Param("listId") Long listId);
}
