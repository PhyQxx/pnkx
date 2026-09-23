package com.pnkx.service.impl;

import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.template.TemplateUtils;
import com.pnkx.domain.po.PxArticle;
import com.pnkx.domain.po.PxEmailSubscribe;
import com.pnkx.mapper.PxEmailSubscribeMapper;
import com.pnkx.service.IPxEmailSubscribeService;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.service.ISysEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author by PHY
 * @Classname PxAdminSubscribeServiceImpl
 * @date 2021-06-17 15:34
 */
@Service
public class PxEmailSubscribeServiceImpl implements IPxEmailSubscribeService {

    private static final Logger logger = LoggerFactory.getLogger(PxEmailSubscribeServiceImpl.class);

    @Resource
    private PxEmailSubscribeMapper pxEmailSubscribeMapper;

    @Resource
    private ISysEmailService sysEmailService;

    /**
     * 查询订阅
     *
     * @param id 订阅ID
     * @return 订阅
     */
    @Override
    public PxEmailSubscribe selectPxEmailSubscribeById(Long id) {
        return pxEmailSubscribeMapper.selectPxEmailSubscribeById(id);
    }

    /**
     * 查询订阅列表
     *
     * @param pxEmailSubscribe 订阅
     * @return 订阅
     */
    @Override
    public List<PxEmailSubscribe> selectPxEmailSubscribeList(PxEmailSubscribe pxEmailSubscribe) {
        return pxEmailSubscribeMapper.selectPxEmailSubscribeList(pxEmailSubscribe);
    }

    /**
     * 新增订阅
     *
     * @param pxEmailSubscribe 订阅
     * @return 结果
     */
    @Override
    public int insertPxEmailSubscribe(PxEmailSubscribe pxEmailSubscribe) {
        pxEmailSubscribe.setCreateTime(DateUtils.getNowDate());
        return pxEmailSubscribeMapper.insertPxEmailSubscribe(pxEmailSubscribe);
    }

    /**
     * 修改订阅
     *
     * @param pxEmailSubscribe 订阅
     * @return 结果
     */
    @Override
    public int updatePxEmailSubscribe(PxEmailSubscribe pxEmailSubscribe) {
        pxEmailSubscribe.setUpdateTime(DateUtils.getNowDate());
        return pxEmailSubscribeMapper.updatePxEmailSubscribe(pxEmailSubscribe);
    }

    /**
     * 批量删除订阅
     *
     * @param ids 需要删除的订阅ID
     * @return 结果
     */
    @Override
    public int deletePxEmailSubscribeByIds(Long[] ids) {
        return pxEmailSubscribeMapper.deletePxEmailSubscribeByIds(ids);
    }

    /**
     * 删除订阅信息
     *
     * @param id 订阅ID
     * @return 结果
     */
    @Override
    public int deletePxEmailSubscribeById(Long id) {
        return pxEmailSubscribeMapper.deletePxEmailSubscribeById(id);
    }

    /**
     * 新文章发布通知：异步向全部订阅者群发邮件（失败仅记录日志，不阻塞发文）
     */
    @Async("notifyExecutor")
    @Override
    public void notifyNewArticle(PxArticle article) {
        List<PxEmailSubscribe> subscribers = pxEmailSubscribeMapper.selectPxEmailSubscribeList(new PxEmailSubscribe());
        if (subscribers.isEmpty()) {
            logger.info("无邮件订阅者，跳过新文章通知");
            return;
        }
        String template = TemplateUtils.getTemplate("newArticle");
        String url = WebsiteAddressConstants.WEB_SITE_ADDRESS + "article/" + article.getId();
        String summary = StringUtils.isEmpty(article.getContent()) ? "点击查看最新内容"
                : StringUtils.strip(article.getContent().replaceAll("<[^>]+>", "")).substring(0, Math.min(120, article.getContent().replaceAll("<[^>]+>", "").length()));
        String content = template.replace("template-title", article.getTitle())
                .replace("template-summary", summary + "……")
                .replace("template-url", url);
        int success = 0;
        for (PxEmailSubscribe subscriber : subscribers) {
            try {
                SysEmail email = new SysEmail();
                email.setReceiverEmail(subscriber.getSubscribeMail());
                email.setSubject("「Pei你看雪」新文章：" + article.getTitle());
                email.setContent(content);
                sysEmailService.sendMail(email);
                success++;
            } catch (Exception e) {
                logger.error("新文章通知发送失败, 订阅者: {}", subscriber.getSubscribeMail(), e);
            }
        }
        logger.info("新文章《{}》通知完成：{}/{} 成功", article.getTitle(), success, subscribers.size());
    }
}
