package com.pnkx.system.mapper;

import com.pnkx.system.domain.SysEmail;

import java.util.List;

/**
 * @author by PHY
 * @Classname PxAdminEmailMapper
 * @date 2021-06-18 09:45
 */
public interface SysEmailMapper {

    /**
     * 查询邮件记录
     *
     * @param id 邮件记录ID
     * @return 邮件记录
     */
    public SysEmail selectSysEmailById(Long id);

    /**
     * 查询邮件记录列表
     *
     * @param SysEmail 邮件记录
     * @return 邮件记录集合
     */
    public List<SysEmail> selectSysEmailList(SysEmail SysEmail);

    /**
     * 新增邮件记录
     *
     * @param SysEmail 邮件记录
     * @return 结果
     */
    public int insertSysEmail(SysEmail SysEmail);

    /**
     * 修改邮件记录
     *
     * @param SysEmail 邮件记录
     * @return 结果
     */
    public int updateSysEmail(SysEmail SysEmail);

    /**
     * 删除邮件记录
     *
     * @param id 邮件记录ID
     * @return 结果
     */
    public int deleteSysEmailById(Long id);

    /**
     * 批量删除邮件记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteSysEmailByIds(Long[] ids);
}
