package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.entity.SysDictData;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.domain.po.PxBookkeepingAccount;
import com.pnkx.domain.po.PxBookkeepingClassification;
import com.pnkx.service.IPxBookKeepingAccountService;
import com.pnkx.system.service.ISysDictTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by PHY
 * @classname PxAdminBookkeepingAccountController
 * @date 2021-11-08 21:01
 * @description: 描述
 */
@Tag(name = "生活工具-生活账本-账户管理")
@RestController
@RequestMapping("/bookkeeping/account")
public class PxBookkeepingAccountController extends BaseController {
    @Resource
    private IPxBookKeepingAccountService pxBookkeepingAccountService;
    @Resource
    private ISysDictTypeService dictTypeService;
    /**
     * 查询最近使用账户列表
     */
    @Operation(summary = "查询账户列表")
    @Log(title = "查询账户列表")
    @GetMapping("/getAccountList")
    public AjaxResult getAccountList(PxBookkeepingClassification pxBookkeepingClassification) {
        // 获取账户列表类型
        List<SysDictData> accountTypeDict = dictTypeService.selectDictDataByType("px_bookkeeping_account_type");
        if (accountTypeDict.isEmpty()) {
            return AjaxResult.error("未找到账户类型字典数据，请先添加字典数据");
        }
        // 查询账户列表
        List<PxBookkeepingAccount> list = pxBookkeepingAccountService.selectPxBookkeepingAccountList(new PxBookkeepingAccount());
        // 构建账户类型
        List<PxBookkeepingAccount> accountTypeList = accountTypeDict.stream().map(dict -> {
            PxBookkeepingAccount account = new PxBookkeepingAccount();
            account.setAccountType(dict.getDictValue());
            account.setAccountName(dict.getDictLabel());
            account.setAccountIcon(dict.getRemark());
            account.setChildren(new ArrayList<>());
            return account;
        }).collect(Collectors.toList());
        // 根据账户类型进行分组
        for (PxBookkeepingAccount account : list) {
            for (PxBookkeepingAccount type : accountTypeList) {
                if (account.getAccountType().equals(type.getAccountType())) {
                    type.getChildren().add(account);
                    break;
                }
            }
        }
        // 添加最近使用账户
        List<PxBookkeepingAccount> recentlyUsedAccounts = pxBookkeepingAccountService.getLatelyAccountList(pxBookkeepingClassification);
        // 为最近使用账户填充余额（getLatelyAccountList不计算balance，从完整列表中获取）
        for (PxBookkeepingAccount recentAcc : recentlyUsedAccounts) {
            for (PxBookkeepingAccount acc : list) {
                if (acc.getId().equals(recentAcc.getId())) {
                    recentAcc.setBalance(acc.getBalance());
                    break;
                }
            }
        }
        PxBookkeepingAccount recentlyUsed = new PxBookkeepingAccount();
        recentlyUsed.setAccountName("最近使用");
        recentlyUsed.setAccountIcon("最近");
        recentlyUsed.setChildren(recentlyUsedAccounts);
        accountTypeList.add(0, recentlyUsed);
        return AjaxResult.success(accountTypeList);
    }

    /**
     * 查询账本用户列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxBookkeepingAccount pxBookkeepingAccount) {
        startPage();
        List<PxBookkeepingAccount> list = pxBookkeepingAccountService.selectPxBookkeepingAccountList(pxBookkeepingAccount);
        return getDataTable(list);
    }

    /**
     * 获取账本用户详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxBookkeepingAccountService.selectPxBookkeepingAccountById(id));
    }

    /**
     * 新增账本用户
     */
    @Operation(summary = "新增账本用户")
    @Log(title = "新增账本用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxBookkeepingAccount pxBookkeepingAccount) {
        int rows = pxBookkeepingAccountService.insertPxBookkeepingAccount(pxBookkeepingAccount);
        if (rows > 0) {
            return AjaxResult.success(pxBookkeepingAccount.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改账本用户
     */
    @Log(title = "修改账本用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxBookkeepingAccount pxBookkeepingAccount) {
        return toAjax(pxBookkeepingAccountService.updatePxBookkeepingAccount(pxBookkeepingAccount));
    }

    /**
     * 删除账本用户
     */
    @Log(title = "账本用户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(pxBookkeepingAccountService.deletePxBookkeepingAccountById(id));
    }
}
