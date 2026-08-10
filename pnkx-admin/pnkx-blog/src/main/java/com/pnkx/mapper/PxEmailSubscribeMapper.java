package com.pnkx.mapper;

import com.pnkx.domain.po.PxEmailSubscribe;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author by PHY
 * @Classname PxAdminSubscribeMapper
 * @date 2021-06-17 15:36
 */
@Mapper
public interface PxEmailSubscribeMapper {

    /**
     * 查询订阅
     *
     * @param id 订阅ID
     * @return 订阅
     */
    PxEmailSubscribe selectPxEmailSubscribeById(Long id);

    /**
     * 查询订阅列表
     *
     * @param pxEmailSubscribe 订阅
     * @return 订阅集合
     */
    List<PxEmailSubscribe> selectPxEmailSubscribeList(PxEmailSubscribe pxEmailSubscribe);

    /**
     * 新增订阅
     *
     * @param pxEmailSubscribe 订阅
     * @return 结果
     */
    int insertPxEmailSubscribe(PxEmailSubscribe pxEmailSubscribe);

    /**
     * 修改订阅
     *
     * @param pxEmailSubscribe 订阅
     * @return 结果
     */
    int updatePxEmailSubscribe(PxEmailSubscribe pxEmailSubscribe);

    /**
     * 删除订阅
     *
     * @param id 订阅ID
     * @return 结果
     */
    int deletePxEmailSubscribeById(Long id);

    /**
     * 批量删除订阅
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deletePxEmailSubscribeByIds(Long[] ids);
}
