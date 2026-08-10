package com.pnkx.system.service.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.ip.IpLocation;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.system.domain.SysNoticeRead;
import com.pnkx.system.domain.vo.SysNoticeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.pnkx.system.domain.SysNotice;
import com.pnkx.system.mapper.SysNoticeMapper;
import com.pnkx.system.service.ISysNoticeService;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 公告 服务层实现
 *
 * @author phy
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService {
    @Resource
    private SysNoticeMapper noticeMapper;

    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Transactional
    @Override
    public SysNoticeVo selectNoticeById(HttpServletRequest request, Long noticeId) {
        SysNoticeRead sysNoticeRead = new SysNoticeRead();
        sysNoticeRead.setNoticeId(noticeId);
        try {
            sysNoticeRead.setCreateBy(SecurityUtils.getUserId());
        } catch (Exception e) {
            sysNoticeRead.setCreateBy(null);
        }
        String ipAddr = IpUtils.getIpAddr(request);
        IpLocation location = IpUtils.getLocation(ipAddr);
        sysNoticeRead.setIp(ipAddr);
        String rectangle = IpUtils.getRectangle(ipAddr);
        sysNoticeRead.setLocation(rectangle);
        sysNoticeRead.setCountry(location.getCountry());
        sysNoticeRead.setProvince(location.getProvince());
        sysNoticeRead.setCity(location.getCity());
        noticeMapper.insertNoticeRead(sysNoticeRead);
        return noticeMapper.selectNoticeById(noticeId);
    }

    /**
     * 查询公告列表
     *
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<SysNoticeVo> selectNoticeList(SysNotice notice) {
        return noticeMapper.selectNoticeList(notice);
    }

    /**
     * 新增公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(SysNotice notice) {
        return noticeMapper.insertNotice(notice);
    }

    /**
     * 修改公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNotice notice) {
        return noticeMapper.updateNotice(notice);
    }

    /**
     * 删除公告对象
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeById(Long noticeId) {
        return noticeMapper.deleteNoticeById(noticeId);
    }

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds) {
        return noticeMapper.deleteNoticeByIds(noticeIds);
    }

    /**
     * 获取未读通知公告
     *
     * @param sysNoticeRead
     * @return
     */
    @Override
    public List<SysNotice> getUnreadNoticeList(SysNoticeRead sysNoticeRead) {
        return noticeMapper.getUnreadNoticeList(sysNoticeRead);
    }

    /**
     * 按条件查询已读通知
     * @param sysNoticeRead 已读通知
     * @return 已读通知列表
     */
    @Override
    public List<SysNoticeRead> selectNoticeRead(SysNoticeRead sysNoticeRead) {
        return noticeMapper.selectNoticeRead(sysNoticeRead);
    }
}
