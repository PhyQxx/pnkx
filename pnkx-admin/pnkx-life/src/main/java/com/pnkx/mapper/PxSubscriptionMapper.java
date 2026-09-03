package com.pnkx.mapper;

import com.pnkx.domain.po.PxSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 订阅管理 Mapper
 *
 * @author PHY
 * @date 2026/07/05
 */
@Mapper
public interface PxSubscriptionMapper {
    PxSubscription selectPxSubscriptionById(Long id);

    PxSubscription selectByClientUuid(String clientUuid);

    List<PxSubscription> selectPxSubscriptionList(PxSubscription pxSubscription);

    int insertPxSubscription(PxSubscription pxSubscription);

    int updatePxSubscription(PxSubscription pxSubscription);

    int deletePxSubscriptionById(Long id);

    int deletePxSubscriptionByIds(Long[] ids);

    /** 查询已到期（next_payment_date <= now）且启用的订阅，用于自动出账 */
    List<PxSubscription> selectDueSubscriptions(@Param("now") Date now);
}
