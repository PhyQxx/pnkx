package com.pnkx.life.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.domain.po.PxBookkeepingAccount;
import com.pnkx.life.domain.po.PxBookkeepingClassification;
import com.pnkx.life.domain.po.PxBookkeepingRecord;
import com.pnkx.life.mapper.PxBookkeepingAccountMapper;
import com.pnkx.life.mapper.PxBookkeepingRecordMapper;
import com.pnkx.life.service.IPxBookKeepingAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author by PHY
 * @classname PxBookkeepingServiceImpl
 * @date 2021-11-08 20:45
 * @description: 描述
 */

@Service
public class PxBookkeepingAccountServiceImpl implements IPxBookKeepingAccountService {
    @Autowired
    private PxBookkeepingAccountMapper pxBookkeepingAccountMapper;
    @Resource
    private PxBookkeepingRecordMapper pxBookkeepingRecordMapper;

    /**
     * 查询账本用户
     *
     * @param id 账本用户ID
     * @return 账本用户
     */
    @Override
    public PxBookkeepingAccount selectPxBookkeepingAccountById(Long id) {
        return pxBookkeepingAccountMapper.selectPxBookkeepingAccountById(id);
    }

    /**
     * 查询账本用户列表
     *
     * @param pxBookkeepingAccount 账本用户
     * @return 账本用户
     */
    @Override
    @DataScopeSelf
    public List<PxBookkeepingAccount> selectPxBookkeepingAccountList(PxBookkeepingAccount pxBookkeepingAccount) {
        return pxBookkeepingAccountMapper.selectPxBookkeepingAccountList(pxBookkeepingAccount);
    }


    /**
     * 新增账本用户
     *
     * @param pxBookkeepingAccount 账本用户
     * @return 结果
     */
    @Override
    public int insertPxBookkeepingAccount(PxBookkeepingAccount pxBookkeepingAccount) {
        pxBookkeepingAccount.setCreateTime(DateUtils.getNowDate());
        pxBookkeepingAccount.setCreateBy(SecurityUtils.getUserId());
        int result = pxBookkeepingAccountMapper.insertPxBookkeepingAccount(pxBookkeepingAccount);
        PxBookkeepingRecord pxBookkeepingRecord = new PxBookkeepingRecord();
        pxBookkeepingRecord.setType(1L);
        pxBookkeepingRecord.setCreateTime(DateUtils.getNowDate());
        pxBookkeepingRecord.setCreateBy(SecurityUtils.getUserId());
        pxBookkeepingRecord.setAccount(pxBookkeepingAccount.getId());
        pxBookkeepingRecord.setMoney(pxBookkeepingAccount.getBalance());
        pxBookkeepingRecord.setPayTime(DateUtils.getNowDate());
        pxBookkeepingRecord.setRemark("修改账户余额");
        pxBookkeepingRecordMapper.insertPxBookkeepingRecord(pxBookkeepingRecord);
        return result;
    }

    /**
     * 修改账本用户
     *
     * @param pxBookkeepingAccount 账本用户
     * @return 结果
     */
    @Override
    public int updatePxBookkeepingAccount(PxBookkeepingAccount pxBookkeepingAccount) {
        pxBookkeepingAccount.setUpdateTime(DateUtils.getNowDate());
        pxBookkeepingAccount.setUpdateBy(SecurityUtils.getUserId());
        int result = pxBookkeepingAccountMapper.updatePxBookkeepingAccount(pxBookkeepingAccount);
        // 余额调整差额记账：差额 = 用户设定余额 - 当前流水结余（selectById 返回计算值）。
        // 必须用 BigDecimal 精确计算：原先 Float 超十万即丢精度，且 String.valueOf 会把
        // 1.0E-4 这类科学计数法脏串写进金额列
        PxBookkeepingAccount oldPxBookkeepingAccount = pxBookkeepingAccountMapper.selectPxBookkeepingAccountById(pxBookkeepingAccount.getId());
        BigDecimal oldBalance = oldPxBookkeepingAccount != null && oldPxBookkeepingAccount.getBalance() != null
                ? oldPxBookkeepingAccount.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = pxBookkeepingAccount.getBalance() != null
                ? pxBookkeepingAccount.getBalance() : oldBalance;
        BigDecimal delta = newBalance.subtract(oldBalance);
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            PxBookkeepingRecord pxBookkeepingRecord = new PxBookkeepingRecord();
            pxBookkeepingRecord.setType(1L);
            pxBookkeepingRecord.setCreateTime(DateUtils.getNowDate());
            pxBookkeepingRecord.setCreateBy(SecurityUtils.getUserId());
            pxBookkeepingRecord.setAccount(pxBookkeepingAccount.getId());
            pxBookkeepingRecord.setMoney(delta);
            pxBookkeepingRecord.setPayTime(DateUtils.getNowDate());
            pxBookkeepingRecord.setRemark("修改账户余额");
            pxBookkeepingRecordMapper.insertPxBookkeepingRecord(pxBookkeepingRecord);
        }
        return result;
    }

    /**
     * 删除账本用户信息
     *
     * @param id 账本用户ID
     * @return 结果
     */
    @Override
    public int deletePxBookkeepingAccountById(Long id) {
        pxBookkeepingRecordMapper.deletePxBookkeepingRecordByAccountId(id);
        return pxBookkeepingAccountMapper.deletePxBookkeepingAccountById(id);
    }

    /**
     * 查询最近使用账户列表
     *
     * @param pxBookkeepingClassification 账户
     * @return 账户列表
     */
    @DataScopeSelf(alias = "r", onlySelf = true)
    @Override
    public List<PxBookkeepingAccount> getLatelyAccountList(PxBookkeepingClassification pxBookkeepingClassification) {
        List<PxBookkeepingAccount> latelyAccountList = pxBookkeepingAccountMapper.getLatelyAccountList(pxBookkeepingClassification);
        // 去重返回前10条
        List<PxBookkeepingAccount> result = new ArrayList<>();
        for (PxBookkeepingAccount bookkeepingAccount : latelyAccountList) {
            if (!result.contains(bookkeepingAccount)) {
                result.add(bookkeepingAccount);
            }
            if (result.size() >= 10) {
                break;
            }
        }
        return result;
    }
}
