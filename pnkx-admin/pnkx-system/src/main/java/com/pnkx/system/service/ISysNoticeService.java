package com.pnkx.system.service;

import java.util.List;

import com.pnkx.system.domain.SysNotice;
import com.pnkx.system.domain.SysNoticeRead;
import com.pnkx.system.domain.vo.SysNoticeVo;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 公告 服务层
 *
 * @author phy
 */
public interface ISysNoticeService {
    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    public SysNoticeVo selectNoticeById(HttpServletRequest request, Long noticeId);

    /**
     * 查询公告列表
     *
     * @param notice 公告信息
     * @return 公告集合
     */
    public List<SysNoticeVo> selectNoticeList(SysNotice notice);

    /**
     * 新增公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    public int insertNotice(SysNotice notice);

    /**
     * 修改公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    public int updateNotice(SysNotice notice);

    /**
     * 删除公告信息
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    public int deleteNoticeById(Long noticeId);

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    public int deleteNoticeByIds(Long[] noticeIds);

    /**
     * 获取未读通知公告
     *
     * @param sysNoticeRead
     * @return
     */
    List<SysNotice> getUnreadNoticeList(SysNoticeRead sysNoticeRead);

    /**
     *
     * @param sysNoticeRead
     * @return
     */
    List<SysNoticeRead> selectNoticeRead(SysNoticeRead sysNoticeRead);
}
