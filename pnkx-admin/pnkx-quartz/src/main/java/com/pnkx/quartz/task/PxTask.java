package com.pnkx.quartz.task;

import com.pnkx.common.constant.RedisConstants;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.template.TemplateUtils;
import com.pnkx.domain.po.PxChatMessageInfo;
import com.pnkx.domain.po.PxToDo;
import com.pnkx.mapper.PxCardUserMapper;
import com.pnkx.mapper.PxToDoMapper;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.mapper.SysUserMapper;
import com.pnkx.system.service.ISysConfigService;
import com.pnkx.system.service.ISysEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时任务调度测试
 *
 * @author phy
 */
@Component("pxTask")
public class PxTask {
    @Resource
    private PxCardUserMapper pxCardUserMapper;
    @Resource
    private PxToDoMapper pxToDoMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private ISysEmailService sysEmailService;
    @Resource
    private ISysConfigService sysConfigService;
    @Resource
    private RedisTemplate redisTemplate;

    private static Logger logger = LoggerFactory.getLogger(PxTask.class);

    /**
     * 定时清理在线聊天室缓存
     */
    public void cleanChat() {
        ValueOperations<String, List<PxChatMessageInfo>> operations = redisTemplate.opsForValue();
        operations.set(RedisConstants.PX_CHAT_MEMBER, new ArrayList<>());
        operations.set(RedisConstants.PX_CHAT_MESSAGE, new ArrayList<>());
    }

    /**
     * 定时发放卡券
     */
    public void grantCard() {
        pxCardUserMapper.regularGrantCard();
    }

    /**
     * 待办提醒
     */
    public void toDoReminder() {
        PxToDo pxToDo = new PxToDo();
        pxToDo.setStatus(false);
        // 未完成的待办列表
        List<PxToDo> pxToDoList = pxToDoMapper.selectPxToDoList(pxToDo);
        
        // 从配置中获取提前提醒的时间（小时），默认4小时
        int remindHoursBefore = 4;
        String remindHoursConfig = sysConfigService.selectConfigByKey("sys.todo.remind.hours.before");
        if (StringUtils.isNotEmpty(remindHoursConfig)) {
            try {
                remindHoursBefore = Integer.parseInt(remindHoursConfig);
            } catch (NumberFormatException e) {
                logger.error("配置 sys.todo.remind.hours.before 格式错误: {}", remindHoursConfig);
            }
        }
        
        // 从配置中获取超时后停止提醒的时间（天），默认3天
        int stopRemindDaysAfter = 3;
        String stopRemindDaysConfig = sysConfigService.selectConfigByKey("sys.todo.remind.stop.days.after");
        if (StringUtils.isNotEmpty(stopRemindDaysConfig)) {
            try {
                stopRemindDaysAfter = Integer.parseInt(stopRemindDaysConfig);
            } catch (NumberFormatException e) {
                logger.error("配置 sys.todo.remind.stop.days.after 格式错误: {}", stopRemindDaysConfig);
            }
        }

        for (PxToDo toDo : pxToDoList) {
            String planEndTimeStr = toDo.getPlanEndTime();
            if (StringUtils.isEmpty(planEndTimeStr)) {
                continue;
            }
            LocalDateTime planEndTime = LocalDateTime.parse(planEndTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // 判断计划结束时间距离当前是否小于设定的提前提醒时间
            if (LocalDateTime.now().isAfter(planEndTime.minusHours(remindHoursBefore))) {
                
                // 如果当前时间已经超过了设定的停止提醒时间，则不再提醒
                if (LocalDateTime.now().isAfter(planEndTime.plusDays(stopRemindDaysAfter))) {
                    continue; // 超过3天不再提醒
                }
                
                // 负责人
                String performer = toDo.getPerformer();
                if (StringUtils.isNotEmpty(performer)) {
                    String[] performers = performer.split(",");
                    for (String userId : performers) {
                        SysEmail sysEmail = new SysEmail();
                        SysUser sysUser = userMapper.selectUserById(Long.valueOf(userId));
                        String activationTemplate = TemplateUtils.getTemplate("todo");
                        activationTemplate = activationTemplate.replace("template-nickName", sysUser.getNickName());
                        activationTemplate = activationTemplate.replace("template-todoTitle", toDo.getContent());
                        StringBuilder todoRemind = new StringBuilder();
                        Duration duration;
                        if (LocalDateTime.now().isAfter(planEndTime)) {
                            duration = Duration.between(planEndTime, LocalDateTime.now());
                            todoRemind.append("已超时");
                        } else {
                            duration = Duration.between(LocalDateTime.now(), planEndTime);
                            todoRemind.append("还有");
                        }
                        long hours = duration.toHours();
                        long minutes = duration.toMinutes() % 60;
                        if (hours > 0) {
                            todoRemind.append(hours).append("个小时");
                        }
                        todoRemind.append(minutes).append("分钟,请及时处理~");
                        activationTemplate = activationTemplate.replace("template-todoRemind", todoRemind);
                        sysEmail.setReceiverEmail(sysUser.getEmail());
                        // 将待办标题加入到通知标题中，防止所有的提醒标题一样导致辨识度低
                        sysEmail.setSubject("\uD83D\uDC49 叮咚！「Pei你看雪博客」您的待办提醒: " + toDo.getContent());
                        sysEmail.setContent(activationTemplate);
                        try {
                            sysEmailService.sendMail(sysEmail);
                        } catch (Exception e) {
                            logger.error("发送待办提醒通知异常，异常信息为：{}", e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
