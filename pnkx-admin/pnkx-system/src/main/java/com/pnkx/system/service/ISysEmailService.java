package com.pnkx.system.service;

import com.pnkx.system.domain.SysEmail;

import java.util.List;

/**
 * @author by PHY
 * @Classname IPxAdminEmailService
 * @date 2021-06-18 09:44
 */
public interface ISysEmailService {
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
     * @param pxEmail 邮件记录
     * @return 邮件记录集合
     */
    public List<SysEmail> selectSysEmailList(SysEmail pxEmail);

    /**
     * 修改邮件记录
     *
     * @param pxEmail 邮件记录
     * @return 结果
     */
    public int updateSysEmail(SysEmail pxEmail);

    /**
     * 批量删除邮件记录
     *
     * @param ids 需要删除的邮件记录ID
     * @return 结果
     */
    public int deleteSysEmailByIds(Long[] ids);

    /**
     * 删除邮件记录信息
     *
     * @param id 邮件记录ID
     * @return 结果
     */
    public int deleteSysEmailById(Long id);

    /**
     * 发送邮件
     * @param email
     * @throws Exception
     */
    void sendMail(SysEmail email) throws Exception;
}
