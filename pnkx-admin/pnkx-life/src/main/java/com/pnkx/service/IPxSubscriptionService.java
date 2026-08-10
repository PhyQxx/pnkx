package com.pnkx.service;

import com.pnkx.domain.po.PxSubscription;

import java.util.List;

/**
 * 订阅管理 Service
 *
 * @author PHY
 * @date 2026/07/05
 */
public interface IPxSubscriptionService {
    PxSubscription selectPxSubscriptionById(Long id);

    List<PxSubscription> selectPxSubscriptionList(PxSubscription pxSubscription);

    int insertPxSubscription(PxSubscription pxSubscription);

    int updatePxSubscription(PxSubscription pxSubscription);

    int deletePxSubscriptionByIds(Long[] ids);

    int deletePxSubscriptionById(Long id);

    /**
     * 月度归一化金额（年订阅折算到月）
     *
     * @param sub 订阅
     * @return 月均金额
     */
    java.math.BigDecimal monthlyNormalized(PxSubscription sub);

    /**
     * 年度预测金额
     *
     * @param sub 订阅
     * @return 年度总额
     */
    java.math.BigDecimal yearlyPredicted(PxSubscription sub);

    /**
     * 调度入口：扫描到期订阅，自动生成记账条目并推进下次扣费日期。
     *
     * @return 本次出账的订阅数
     */
    int generateDueRecords();
}
