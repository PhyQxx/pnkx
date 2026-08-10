package com.pnkx.quartz.task;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.service.AiLifeReportDataService;
import com.pnkx.domain.po.PxLifeReportHistory;
import com.pnkx.mapper.PxLifeReportHistoryMapper;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.mapper.SysUserMapper;
import com.pnkx.system.service.ISysEmailService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * AI 生活报告定时任务。
 * <p>
 * 由 sys_job 表配置 invokeTarget = "aiLifeReportTask.generate" + cron 表达式调用。
 * 建议 cron 每周一早上 9 点执行：0 0 9 ? * MON
 * <p>
 * 流程：聚合本周数据 → AI 生成 Markdown 报告 → 邮件推送给博主（默认 admin）。
 * 报告无持久化（与 PxLifeReportController 保持一致），如需历史记录后续可建表归档。
 *
 * @author PHY
 * @date 2026/08/04
 */
@Component("aiLifeReportTask")
public class AiLifeReportTask {

    private static final Logger log = LoggerFactory.getLogger(AiLifeReportTask.class);

    /**
     * 报告接收人（博主本人）。系统为单用户博客，固定取 admin。
     * 如需支持多用户，可改为遍历用户列表或读取 sys.life.report.target.user 配置。
     */
    private static final Long TARGET_USER_ID = 1L;

    private static final String PERIOD = "week";
    private static final String REPORT_TYPE = "summary";

    @Resource
    private AiLifeReportDataService lifeReportDataService;

    @Resource
    private AiClient aiClient;

    @Resource
    private ISysEmailService sysEmailService;

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private PxLifeReportHistoryMapper reportHistoryMapper;

    /**
     * 生成本周生活报告并发送邮件。
     * <p>
     * invokeTarget: aiLifeReportTask.generate
     */
    public void generate() {
        long start = System.currentTimeMillis();
        log.info("【AI生活报告】开始生成本周报告，userId={}", TARGET_USER_ID);
        try {
            // 1. 聚合本周数据
            JSONObject reportData = lifeReportDataService.buildReportData(
                    TARGET_USER_ID.toString(), PERIOD, REPORT_TYPE);

            // 2. AI 生成 Markdown 报告
            //    prompt 末尾加生成时间，避免 AiClient 同步方法 1 小时 redis 缓存导致重复触发返回旧报告
            String generateTime = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String promptText = "你是一个生活分析专家。根据以下用户周度数据，生成一份生活报告。\n"
                    + "数据：" + reportData.toJSONString() + "\n\n"
                    + "报告要求：\n"
                    + "1. 消费总览与建议\n"
                    + "2. 心情与日记关键词\n"
                    + "3. 待办完成情况\n"
                    + "4. 下期建议\n"
                    + "请用 Markdown 格式输出，突出重点。\n"
                    + "（生成时间：" + generateTime + "）";

            JSONObject aiResult = aiClient.chat("你是一个专业的生活助理和数据分析师。", promptText);
            if (aiResult == null || aiResult.getString("content") == null) {
                log.warn("【AI生活报告】AI 返回为空，跳过发送");
                return;
            }
            String reportMarkdown = aiResult.getString("content");

            PxLifeReportHistory history = new PxLifeReportHistory();
            history.setUserId(TARGET_USER_ID.toString());
            history.setPeriod(PERIOD);
            history.setReportType(REPORT_TYPE);
            history.setSource("scheduled");
            history.setContent(reportMarkdown);
            reportHistoryMapper.insert(history);

            // 3. 邮件推送
            sendReportEmail(reportMarkdown, generateTime);

            log.info("【AI生活报告】生成并发送完成，耗时 {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("【AI生活报告】生成异常", e);
        }
    }

    /**
     * 发送报告邮件
     */
    private void sendReportEmail(String reportMarkdown, String dateLabel) throws Exception {
        SysUser user = userMapper.selectUserById(TARGET_USER_ID);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("【AI生活报告】用户 {} 未配置邮箱，跳过邮件推送", TARGET_USER_ID);
            return;
        }

        SysEmail email = new SysEmail();
        email.setReceiverEmail(user.getEmail());
        email.setSubject("【Pei你看雪】本周生活报告 " + dateLabel);
        // Markdown 文本作为邮件正文（邮件客户端大多能较好渲染 Markdown 纯文本）
        email.setContent(reportMarkdown);
        sysEmailService.sendMail(email);
        log.info("【AI生活报告】已发送至 {}", user.getEmail());
    }
}
