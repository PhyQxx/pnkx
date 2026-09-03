package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxShoppingList;
import com.pnkx.mapper.PxShoppingItemMapper;
import com.pnkx.mapper.PxShoppingListMapper;
import com.pnkx.service.IPxShoppingListService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxShoppingListServiceImpl
 * @data 2026/07/05
 * @description 购物清单Service实现
 */
@Service
public class PxShoppingListServiceImpl implements IPxShoppingListService {
    @Resource
    private PxShoppingListMapper pxShoppingListMapper;

    @Resource
    private PxShoppingItemMapper pxShoppingItemMapper;

    /**
     * 查询购物清单
     *
     * @param id 购物清单ID
     * @return 购物清单
     */
    @Override
    public PxShoppingList selectPxShoppingListById(Long id) {
        return pxShoppingListMapper.selectPxShoppingListById(id);
    }

    /**
     * 查询购物清单列表
     *
     * @param pxShoppingList 购物清单
     * @return 购物清单
     */
    @Override
    @DataScopeSelf
    public List<PxShoppingList> selectPxShoppingListList(PxShoppingList pxShoppingList) {
        return pxShoppingListMapper.selectPxShoppingListList(pxShoppingList);
    }


    /**
     * 新增购物清单
     *
     * @param pxShoppingList 购物清单
     * @return 结果
     */
    @Override
    public int insertPxShoppingList(PxShoppingList pxShoppingList) {
        if (StringUtils.isNotEmpty(pxShoppingList.getClientUuid())) {
            PxShoppingList existing = pxShoppingListMapper.selectByClientUuid(
                    pxShoppingList.getClientUuid());
            if (existing != null) {
                pxShoppingList.setId(existing.getId());
                return 1;
            }
        }
        pxShoppingList.setCreateBy(SecurityUtils.getUserId());
        pxShoppingList.setCreateTime(DateUtils.getNowDate());
        return pxShoppingListMapper.insertPxShoppingList(pxShoppingList);
    }

    /**
     * 修改购物清单
     *
     * @param pxShoppingList 购物清单
     * @return 结果
     */
    @Override
    public int updatePxShoppingList(PxShoppingList pxShoppingList) {
        pxShoppingList.setUpdateTime(DateUtils.getNowDate());
        return pxShoppingListMapper.updatePxShoppingList(pxShoppingList);
    }

    /**
     * 批量删除购物清单
     *
     * @param ids 需要删除的购物清单ID
     * @return 结果
     */
    @Override
    public int deletePxShoppingListByIds(Long[] ids) {
        for (Long id : ids) {
            pxShoppingItemMapper.deleteByListId(id);
        }
        return pxShoppingListMapper.deletePxShoppingListByIds(ids);
    }

    /**
     * 删除购物清单信息
     *
     * @param id 购物清单ID
     * @return 结果
     */
    @Override
    public int deletePxShoppingListById(Long id) {
        pxShoppingItemMapper.deleteByListId(id);
        return pxShoppingListMapper.deletePxShoppingListById(id);
    }
}
