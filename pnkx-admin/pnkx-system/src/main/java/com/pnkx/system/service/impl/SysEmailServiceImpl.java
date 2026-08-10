package com.pnkx.system.service.impl;

import com.pnkx.common.notify.FeishuISysNotify;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.email.EmailUtils;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.mapper.SysEmailMapper;
import com.pnkx.system.service.ISysConfigService;
import com.pnkx.system.service.ISysEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Objects;

/**
 * @author by PHY
 * @Classname SysEmailServiceImpl
 * @date 2021-06-18 09:43
 */
@Service
public class SysEmailServiceImpl implements ISysEmailService {

    @Resource
    private SysEmailMapper sysEmailMapper;
    @Resource
    private JavaMailSender javaMailSender;
    @Resource
    private ISysConfigService sysConfigService;
    
    @Value("${spring.mail.username}")
    private String from;

    /**
     * 查询邮件记录
     *
     * @param id 邮件记录ID
     * @return 邮件记录
     */
    @Override
    public SysEmail selectSysEmailById(Long id) {
        return sysEmailMapper.selectSysEmailById(id);
    }

    /**
     * 查询邮件记录列表
     *
     * @param sysEmail 邮件记录
     * @return 邮件记录
     */
    @Override
    public List<SysEmail> selectSysEmailList(SysEmail sysEmail) {
        return sysEmailMapper.selectSysEmailList(sysEmail);
    }

    /**
     * 修改邮件记录
     *
     * @param sysEmail 邮件记录
     * @return 结果
     */
    @Override
    public int updateSysEmail(SysEmail sysEmail) {
        sysEmail.setUpdateTime(DateUtils.getNowDate());
        return sysEmailMapper.updateSysEmail(sysEmail);
    }

    /**
     * 批量删除邮件记录
     *
     * @param ids 需要删除的邮件记录ID
     * @return 结果
     */
    @Override
    public int deleteSysEmailByIds(Long[] ids) {
        return sysEmailMapper.deleteSysEmailByIds(ids);
    }

    /**
     * 删除邮件记录信息
     *
     * @param id 邮件记录ID
     * @return 结果
     */
    @Override
    public int deleteSysEmailById(Long id) {
        return sysEmailMapper.deleteSysEmailById(id);
    }

    @Override
    @Transactional
    public void sendMail(SysEmail email) throws MessagingException {
        // 判断是否开启飞书通知
        String feishuNotifyEnable = sysConfigService.selectConfigByKey("sys.notify.feishu.enable");
        if (StringUtils.isNotEmpty(feishuNotifyEnable) && "true".equals(feishuNotifyEnable)) {
            // 飞书通知
            String feishuWebhookUrl = sysConfigService.selectConfigByKey("sys.notify.feishu.webhook");
            FeishuISysNotify.sendNotification(feishuWebhookUrl, email.getSubject(), "");
        }
        
        // 判断是否开启邮件通知
        String emailNotifyEnable = sysConfigService.selectConfigByKey("sys.notify.email.enable");
        if (StringUtils.isEmpty(emailNotifyEnable) || "true".equals(emailNotifyEnable)) { // 默认开启
            sysEmailMapper.insertSysEmail(email);
            String[] to = EmailUtils.validEmail(email.getReceiverEmail());
            if (to != null && to.length > 0) {
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setFrom(from);
                helper.setTo(to);
                if (StringUtils.isNotEmpty(email.getCcEmail())) {
                    helper.setCc(Objects.requireNonNull(EmailUtils.validEmail(email.getCcEmail())));
                }
                helper.setSubject(email.getSubject());
                helper.setText(email.getContent(), true);
                javaMailSender.send(message);
            }
        }
    }
}
