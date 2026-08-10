package com.pnkx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.template.TemplateUtils;
import com.pnkx.domain.po.PxFriendLink;
import com.pnkx.mapper.PxFriendLinkMapper;
import com.pnkx.service.IPxFriendLinkService;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.service.ISysEmailService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author by PHY
 * @Classname PxFriendLinkServiceImpl
 * @date 2021-04-30 11:44
 */
@Service
public class PxFriendLinkServiceImpl extends ServiceImpl<PxFriendLinkMapper, PxFriendLink> implements IPxFriendLinkService {
    private static final Logger log = LoggerFactory.getLogger(PxFriendLinkServiceImpl.class);
    @Resource
    private PxFriendLinkMapper pxFriendLinkMapper;
    @Resource
    private ISysEmailService sysEmailService;

    /**
     * 查询友链
     *
     * @param id 友链ID
     * @return 友链
     */
    @Override
    public PxFriendLink selectPxFriendLinkById(Long id) {
        return pxFriendLinkMapper.selectById(id);
    }

    /**
     * 查询友链列表
     *
     * @param pxFriendLink 友链
     * @return 友链
     */
    @DataScopeSelf
    @Override
    public List<PxFriendLink> selectPxFriendLinkList(PxFriendLink pxFriendLink) {
        return pxFriendLinkMapper.selectList(buildQueryWrapper(pxFriendLink));
    }

    /**
     * 查询友链列表（分页）
     */
    @DataScopeSelf
    @Override
    public IPage<PxFriendLink> selectPxFriendLinkList(IPage<PxFriendLink> page, PxFriendLink pxFriendLink) {
        return pxFriendLinkMapper.selectPage(page, buildQueryWrapper(pxFriendLink));
    }

    /**
     * 新增友链
     *
     * @param pxFriendLink 友链
     * @return 结果
     */
    @Override
    public int insertPxFriendLink(PxFriendLink pxFriendLink) {
        pxFriendLink.setCreateBy(SecurityUtils.getUserId());
        pxFriendLink.setCreateTime(DateUtils.getNowDate());
        return pxFriendLinkMapper.insert(pxFriendLink);
    }

    /**
     * 修改友链
     *
     * @param pxFriendLink 友链
     * @return 结果
     */
    @Override
    public int updatePxFriendLink(PxFriendLink pxFriendLink) {
        pxFriendLink.setUpdateBy(SecurityUtils.getUserName());
        pxFriendLink.setUpdateTime(DateUtils.getNowDate());
        PxFriendLink original = pxFriendLinkMapper.selectById(pxFriendLink.getId());
        int rows = pxFriendLinkMapper.updateById(pxFriendLink);
        if (rows > 0 && original != null && pxFriendLink.getStatus() != null
                && !pxFriendLink.getStatus().equals(original.getStatus())) {
            sendAuditResult(original, pxFriendLink.getStatus(), pxFriendLink.getRemark());
        }
        return rows;
    }

    private void sendAuditResult(PxFriendLink link, String status, String auditRemark) {
        if (StringUtils.isEmpty(link.getEmail()) || (!"1".equals(status) && !"2".equals(status))) return;
        boolean approved = "1".equals(status);
        String resultText = approved ? "已通过" : "未通过";
        String reason = StringUtils.isEmpty(auditRemark) ? (approved ? "欢迎加入友链。" : "如有疑问，请联系站点管理员。") : auditRemark;
        String template = TemplateUtils.getTemplate("link")
                .replace("template-lineTheme", link.getTitle() + "的博主您好，您的友链申请" + resultText)
                .replace("template-lineTitle", StringUtils.nvl(link.getTitle(), ""))
                .replace("template-lineRemark", reason)
                .replace("template-lineUrl", StringUtils.nvl(link.getUrl(), ""))
                .replace("template-lineImg", StringUtils.nvl(link.getImg(), ""))
                .replace("template-lineEmail", link.getEmail());
        SysEmail email = new SysEmail();
        email.setReceiverEmail(link.getEmail());
        email.setSubject("【Pei你看雪】友链申请" + resultText);
        email.setContent(template);
        try {
            sysEmailService.sendMail(email);
        } catch (Exception e) {
            // 审核结果已经保存，通知失败不回滚业务操作。
            log.error("发送友链审核结果邮件失败，linkId={}", link.getId(), e);
        }
    }

    /**
     * 批量删除友链
     *
     * @param ids 需要删除的友链ID
     * @return 结果
     */
    @Override
    public int deletePxFriendLinkByIds(Long[] ids) {
        return pxFriendLinkMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除友链信息
     *
     * @param id 友链ID
     * @return 结果
     */
    @Override
    public int deletePxFriendLinkById(Long id) {
        return pxFriendLinkMapper.deleteById(id);
    }

    private LambdaQueryWrapper<PxFriendLink> buildQueryWrapper(PxFriendLink pxFriendLink) {
        LambdaQueryWrapper<PxFriendLink> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNull(pxFriendLink)) {
            return wrapper.orderByDesc(PxFriendLink::getCreateTime);
        }
        wrapper.eq(StringUtils.isNotEmpty(pxFriendLink.getImg()), PxFriendLink::getImg, pxFriendLink.getImg())
                .like(StringUtils.isNotEmpty(pxFriendLink.getTitle()), PxFriendLink::getTitle, pxFriendLink.getTitle())
                .eq(StringUtils.isNotEmpty(pxFriendLink.getUrl()), PxFriendLink::getUrl, pxFriendLink.getUrl())
                .eq(StringUtils.isNotNull(pxFriendLink.getStatus()), PxFriendLink::getStatus, pxFriendLink.getStatus())
                .eq(StringUtils.isNotEmpty(pxFriendLink.getVersion()), PxFriendLink::getVersion, pxFriendLink.getVersion());
        appendDataScope(wrapper, pxFriendLink.getParams());
        return wrapper.orderByDesc(PxFriendLink::getCreateTime);
    }

    private void appendDataScope(LambdaQueryWrapper<PxFriendLink> wrapper, Map<String, Object> params) {
        if (params == null || Boolean.TRUE.equals(params.get(DataScopeSelf.SCOPE_ALL))) {
            return;
        }
        Object scopeUserIds = params.get(DataScopeSelf.SCOPE_USER_IDS);
        if (!(scopeUserIds instanceof Collection<?> userIds)) {
            return;
        }
        List<String> createByList = new ArrayList<>();
        for (Object userId : userIds) {
            if (StringUtils.isNotNull(userId)) {
                createByList.add(String.valueOf(userId));
            }
        }
        if (createByList.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(PxFriendLink::getCreateBy, createByList);
    }
}
