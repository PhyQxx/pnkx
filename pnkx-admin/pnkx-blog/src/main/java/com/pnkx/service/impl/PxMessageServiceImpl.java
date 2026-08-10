package com.pnkx.service.impl;

import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.template.TemplateUtils;
import com.pnkx.domain.po.PxLeaveMessage;
import com.pnkx.mapper.PxMessageMapper;
import com.pnkx.service.IPxMessageService;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.mapper.SysUserMapper;
import com.pnkx.system.mapper.SysUserPostMapper;
import com.pnkx.system.service.ISysConfigService;
import com.pnkx.system.service.ISysEmailService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * 文章Service业务层处理
 *
 * @author phy
 * @date 2021-01-26
 */
@Service
public class PxMessageServiceImpl implements IPxMessageService {
    private static final Logger log = LoggerFactory.getLogger(PxMessageServiceImpl.class);
    @Resource
    private PxMessageMapper pxMessageMapper;
    @Resource
    private ISysEmailService sysEmailService;
    @Resource
    private ISysConfigService configService;
    @Resource
    private SysUserMapper userMapper;


    /**
     * 留言
     *
     * @param pxLeaveMessage 参数
     * @return 留言结果
     */
    @Override
    public Integer addMessage(PxLeaveMessage pxLeaveMessage) {
        pxLeaveMessage.setCreateBy(SecurityUtils.getUserId());
        pxLeaveMessage.setCreateTime(DateUtils.getNowDate());
        pxLeaveMessage.setAvatar(SecurityUtils.getLoginUser().getUser().getAvatar());
        Integer result = pxMessageMapper.addMessage(pxLeaveMessage);
        SysEmail sysEmail = new SysEmail();
        if (StringUtils.isNull(pxLeaveMessage.getParentId())) {
            // 发送给博客主人有人留言
            String activationTemplate = TemplateUtils.getTemplate("message");
            activationTemplate = activationTemplate.replace("template-nickName", SecurityUtils.getLoginUser().getUser().getNickName());
            activationTemplate = activationTemplate.replace("template-messageContent", pxLeaveMessage.getContent());
            if ("0".equals(pxLeaveMessage.getMessageBoard())) {
                // 文章
                activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS + "post/" + pxLeaveMessage.getArticleId());
            } else if ("1".equals(pxLeaveMessage.getMessageBoard())) {
                // 留言板
                activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS + "message");
            } else if ("3".equals(pxLeaveMessage.getMessageBoard())) {
                // 友链
                activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS + "link");
            }
            String siteEmail = configService.selectConfigByKey("siteEmail");
            String[] emails = siteEmail.split(",");
            sysEmail.setReceiverEmail(emails[0]);
            sysEmail.setCcEmail(emails[1]);
            sysEmail.setSubject("\uD83D\uDC49 叮咚！「Pei你看雪博客」上有人留言了");
            sysEmail.setContent(activationTemplate);
        } else {
            // 发送给被回复者有人回复
            String activationTemplate = TemplateUtils.getTemplate("reply");
            PxLeaveMessage replyMessage = pxMessageMapper.getMessageById(pxLeaveMessage.getReplyId());
            activationTemplate = activationTemplate.replace("template-nickName", replyMessage.getNickName());
            activationTemplate = activationTemplate.replace("template-messageContent", replyMessage.getContent());
            activationTemplate = activationTemplate.replace("template-replyNickName", SecurityUtils.getLoginUser().getUser().getNickName());
            activationTemplate = activationTemplate.replace("template-replyMessageContent", pxLeaveMessage.getContent());
            if ("0".equals(pxLeaveMessage.getMessageBoard())) {
                // 文章
                activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS + "post/" + pxLeaveMessage.getArticleId());
            } else if ("1".equals(pxLeaveMessage.getMessageBoard())) {
                // 留言板
                activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS + "message");
            } else if ("3".equals(pxLeaveMessage.getMessageBoard())) {
                // 友链
                activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS + "link");
            }
            SysUser sysUser = userMapper.selectUserById(pxLeaveMessage.getReplyUserId());
            sysEmail.setReceiverEmail(sysUser.getEmail());
            sysEmail.setSubject("\uD83D\uDC49 叮咚！「Pei你看雪博客」上有人@了您");
            sysEmail.setContent(activationTemplate);
        }
        try {
            sysEmailService.sendMail(sysEmail);
        } catch (Exception e) {
            throw new ServiceException("发送邮件异常");
        }
        return result;
    }

    /**
     * 查询留言列表
     *
     * @param pxLeaveMessage 留言
     * @return 留言
     */
    @Override
    public List<PxLeaveMessage> selectPxLeaveMessageList(PxLeaveMessage pxLeaveMessage) {
        List<PxLeaveMessage> list = pxMessageMapper.selectPxLeaveMessageList(pxLeaveMessage);
        enrichLeaveMessage(list, pxLeaveMessage);
        return list;
    }

    /**
     * 补充留言的昵称、点赞数及回复列表
     *
     * @param list           留言集合
     * @param pxLeaveMessage 查询参数
     */
    private void enrichLeaveMessage(List<PxLeaveMessage> list, PxLeaveMessage pxLeaveMessage) {
        list.forEach(item -> {
            item.setLikeNumber(item.getArticleLikeNumber());
            if (StringUtils.isEmpty(item.getCreateBy())) {
                item.setNickName(item.getAuthorName());
            }
            // 补充回复列表
            PxLeaveMessage replyParams = new PxLeaveMessage();
            if (StringUtils.isNull(pxLeaveMessage.getParentId())) {
                replyParams.setParentId(item.getId());
                List<PxLeaveMessage> replyList = pxMessageMapper.selectPxLeaveMessageList(replyParams);
                // 只要3条
                if (replyList.size() > 2) {
                    replyList = replyList.subList(0, 3);
                }
                replyList.forEach(reply -> {
                    reply.setLikeNumber(reply.getCommentLikeNumber());
                    // 补充回复信息
                    if (StringUtils.isNotNull(reply.getReplyId())) {
                        PxLeaveMessage replyMessage = pxMessageMapper.getMessageById(reply.getReplyId());
                        if (StringUtils.isEmpty(replyMessage.getCreateBy())) {
                            reply.setReplyUserId(null);
                            reply.setReplyNickName(replyMessage.getAuthorName());
                        } else {
                            reply.setReplyUserId(new Long(replyMessage.getCreateBy()));
                            reply.setReplyNickName(replyMessage.getNickName());
                        }
                    }
                });
                item.setReplyList(replyList);
            }
        });
    }

    /**
     * 查询留言审核
     *
     * @param pxLeaveMessage 留言
     * @return 留言
     */
    @Override
    public List<PxLeaveMessage> selectPxLeaveMessageExamine(PxLeaveMessage pxLeaveMessage) {
        return pxMessageMapper.selectPxLeaveMessageList(pxLeaveMessage);
    }

    /**
     * 修改留言
     *
     * @param pxLeaveMessage 留言
     * @return 结果
     */
    @Override
    public int updatePxLeaveMessage(PxLeaveMessage pxLeaveMessage) {
        PxLeaveMessage original = pxMessageMapper.getMessageById(pxLeaveMessage.getId());
        pxLeaveMessage.setUpdateBy(SecurityUtils.getUserId());
        pxLeaveMessage.setUpdateTime(DateUtils.getNowDate());
        int rows = pxMessageMapper.updatePxLeaveMessage(pxLeaveMessage);
        if (rows > 0 && original != null && pxLeaveMessage.getState() != null
                && !Objects.equals(original.getState(), pxLeaveMessage.getState())) {
            sendAuditResultEmail(original, pxLeaveMessage.getState(), pxLeaveMessage.getRemark());
        }
        return rows;
    }

    private void sendAuditResultEmail(PxLeaveMessage message, String state, String auditRemark) {
        String receiver = message.getAuthorMailbox();
        if (StringUtils.isEmpty(receiver) && StringUtils.isNotEmpty(message.getCreateBy())) {
            SysUser user = userMapper.selectUserById(Long.valueOf(message.getCreateBy()));
            receiver = user == null ? null : user.getEmail();
        }
        if (StringUtils.isEmpty(receiver)) {
            return;
        }
        String result = "1".equals(state) ? "审核通过" : "审核未通过";
        String reason = StringUtils.isEmpty(auditRemark) ? "如有疑问，请联系站点管理员。" : auditRemark;
        SysEmail email = new SysEmail();
        email.setReceiverEmail(receiver);
        email.setSubject("【Pei你看雪】留言" + result);
        email.setContent("<p>您好，您提交的留言已完成审核。</p><p><strong>结果：" + result
                + "</strong></p><p>留言内容：" + escapeHtml(message.getContent()) + "</p><p>说明："
                + escapeHtml(reason) + "</p>");
        try {
            sysEmailService.sendMail(email);
        } catch (Exception e) {
            // 审核结果已落库，邮件失败不应回滚审核操作。
            log.error("发送留言审核结果邮件失败，messageId={}", message.getId(), e);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
