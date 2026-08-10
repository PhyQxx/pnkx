package com.pnkx.mapper;

import com.pnkx.domain.po.PxBookkeepingAccount;
import com.pnkx.domain.po.PxBookkeepingClassification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by PHY
 * @classname PxBookkeepingAccountMapper
 * @date 2021-11-08 21:11
 * @description: 描述
 */
@Mapper
public interface PxBookkeepingAccountMapper {
    /**
     * 查询账本用户
     *
     * @param id 账本用户ID
     * @return 账本用户
     */
    public PxBookkeepingAccount selectPxBookkeepingAccountById(Long id);

    /**
     * 查询账本用户列表
     *
     * @param pxBookkeepingAccount 账本用户
     * @return 账本用户集合
     */
    public List<PxBookkeepingAccount> selectPxBookkeepingAccountList(PxBookkeepingAccount pxBookkeepingAccount);

    /**
     * 新增账本用户
     *
     * @param pxBookkeepingAccount 账本用户
     * @return 结果
     */
    public int insertPxBookkeepingAccount(PxBookkeepingAccount pxBookkeepingAccount);

    /**
     * 修改账本用户
     *
     * @param pxBookkeepingAccount 账本用户
     * @return 结果
     */
    public int updatePxBookkeepingAccount(PxBookkeepingAccount pxBookkeepingAccount);

    /**
     * 删除账本用户
     *
     * @param id 账本用户ID
     * @return 结果
     */
    public int deletePxBookkeepingAccountById(Long id);

    /**
     * 查询最近使用账户列表
     *
     * @param pxBookkeepingClassification 账户
     * @return 账户列表
     */
    List<PxBookkeepingAccount> getLatelyAccountList(PxBookkeepingClassification pxBookkeepingClassification);

    /**
     * 增量查询（离线同步用）
     */
    List<PxBookkeepingAccount> selectIncremental(@Param("createBy") String createBy, @Param("since") String since, @Param("offset") int offset, @Param("limit") int limit);
}
