package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.domain.SysNoticeRead;
import com.ruoyi.system.domain.vo.SysNoticeVo;

/**
 * 通知公告表 数据层
 *
 * @author ruoyi
 */
public interface SysNoticeMapper
{
    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    public SysNoticeVo selectNoticeById(Long noticeId);

    /**
     * 新增通知已读记录
     * @param sysNoticeRead
     * @return
     */
    int insertNoticeRead(SysNoticeRead sysNoticeRead);

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
     * 批量删除公告
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
     * @param sysNoticeRead
     * @return
     */
    List<SysNotice> getUnreadNoticeList(SysNoticeRead sysNoticeRead);

    /**
     * 按条件查询已读通知
     * @param sysNoticeRead
     * @return
     */
    List<SysNoticeRead> selectNoticeRead(SysNoticeRead sysNoticeRead);
}
