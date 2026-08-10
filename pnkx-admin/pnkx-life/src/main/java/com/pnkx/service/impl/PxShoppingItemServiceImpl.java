package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.domain.po.PxShoppingItem;
import com.pnkx.mapper.PxShoppingItemMapper;
import com.pnkx.service.IPxShoppingItemService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxShoppingItemServiceImpl
 * @data 2026/07/05
 * @description 购物条目Service实现
 */
@Service
public class PxShoppingItemServiceImpl implements IPxShoppingItemService {
    @Resource
    private PxShoppingItemMapper pxShoppingItemMapper;

    /**
     * 查询购物条目
     *
     * @param id 购物条目ID
     * @return 购物条目
     */
    @Override
    public PxShoppingItem selectPxShoppingItemById(Long id) {
        return pxShoppingItemMapper.selectPxShoppingItemById(id);
    }

    /**
     * 查询购物条目列表
     *
     * @param pxShoppingItem 购物条目
     * @return 购物条目
     */
    @Override
    @DataScopeSelf
    public List<PxShoppingItem> selectPxShoppingItemList(PxShoppingItem pxShoppingItem) {
        return pxShoppingItemMapper.selectPxShoppingItemList(pxShoppingItem);
    }


    /**
     * 新增购物条目
     *
     * @param pxShoppingItem 购物条目
     * @return 结果
     */
    @Override
    public int insertPxShoppingItem(PxShoppingItem pxShoppingItem) {
        pxShoppingItem.setCreateTime(DateUtils.getNowDate());
        return pxShoppingItemMapper.insertPxShoppingItem(pxShoppingItem);
    }

    /**
     * 修改购物条目
     *
     * @param pxShoppingItem 购物条目
     * @return 结果
     */
    @Override
    public int updatePxShoppingItem(PxShoppingItem pxShoppingItem) {
        pxShoppingItem.setUpdateTime(DateUtils.getNowDate());
        return pxShoppingItemMapper.updatePxShoppingItem(pxShoppingItem);
    }

    /**
     * 批量删除购物条目
     *
     * @param ids 需要删除的购物条目ID
     * @return 结果
     */
    @Override
    public int deletePxShoppingItemByIds(Long[] ids) {
        return pxShoppingItemMapper.deletePxShoppingItemByIds(ids);
    }

    /**
     * 删除购物条目信息
     *
     * @param id 购物条目ID
     * @return 结果
     */
    @Override
    public int deletePxShoppingItemById(Long id) {
        return pxShoppingItemMapper.deletePxShoppingItemById(id);
    }

    /**
     * 清空已勾选条目
     *
     * @param listId 购物清单ID
     * @return 结果
     */
    @Override
    public int clearChecked(Long listId) {
        return pxShoppingItemMapper.clearChecked(listId);
    }
}
