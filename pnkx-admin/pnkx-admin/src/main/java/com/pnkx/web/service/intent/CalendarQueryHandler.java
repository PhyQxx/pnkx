package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.CalendarAggregateService;
import com.pnkx.life.service.webarticle.WebArticleUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.io.OutputStream;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Component
public class CalendarQueryHandler implements IntentHandler {
    @Resource private AiClient ai;
    @Resource private CalendarAggregateService calendar;
    public String intentName() { return "calendar_query"; }
    public String promptDescription() {
        return "按日期范围查询日历安排、待办、纪念日等，最多90天。只读。日历按天聚合，不能判断具体时段冲突。slots:{startDate:yyyy-MM-dd,endDate:yyyy-MM-dd}";
    }
    static LocalDate[] range(JSONObject input) {
        try {
            LocalDate start=LocalDate.parse(input.getString("startDate"));
            LocalDate end=LocalDate.parse(input.getString("endDate"));
            if(end.isBefore(start) || ChronoUnit.DAYS.between(start,end)>=90)
                throw new IllegalArgumentException();
            return new LocalDate[]{start,end};
        } catch(Exception e) { throw new ServiceException("请提供有效的起止日期，查询范围最多90天且结束日期不能早于开始日期"); }
    }
    public boolean handle(String question,JSONObject slots,OutputStream out) throws java.io.IOException {
        try {
            var response=ai.chat("今天是"+LocalDate.now(ZoneId.of("Asia/Shanghai"))+"，时区Asia/Shanghai。"
                    +"从日历查询请求提取日期范围，仅返回JSON {\"startDate\":\"yyyy-MM-dd\",\"endDate\":\"yyyy-MM-dd\"}。"
                    +"单日查询起止相同；本周为周一至周日；没有日期或无法确定则字段填null，不猜测。",question);
            var input=WebArticleUtils.extractJson(response==null?null:response.getString("content"));
            if(input==null) throw new ServiceException("请说明要查询哪一天或哪段日期");
            var dates=range(input);
            var events=calendar.getMonthEvents(SecurityUtils.getUserId(),dates[0].toString(),dates[1].toString());
            var format=new SimpleDateFormat("yyyy-MM-dd"); format.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            StringBuilder text=new StringBuilder("**日历安排**\n\n").append(dates[0]).append(" 至 ").append(dates[1]).append("（北京时间）\n\n");
            if(events.isEmpty()) text.append("该范围内没有日历记录。\n");
            events.stream().sorted(java.util.Comparator.comparing(e -> e.getDate(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .limit(100).forEach(e -> text.append("- ").append(e.getDate()==null?"日期未注明":format.format(e.getDate()))
                            .append(" · ").append(e.getTitle()).append("（").append(typeLabel(e.getSourceType())).append(" #")
                            .append(e.getSourceId()).append("）\n"));
            if(events.size()>100) text.append("\n仅展示前100条，请缩小查询日期。\n");
            text.append("\n日历记录按天汇总，同日事项不代表时间冲突；判断冲突还需具体开始和结束时间。");
            IntentHandler.writeSse(out,text.toString()); IntentHandler.writeSse(out,"[DONE]");
        } catch(ServiceException e) { IntentHandler.writeSse(out,e.getMessage()); IntentHandler.writeSse(out,"[DONE]"); }
        catch(Exception e) { IntentHandler.writeSse(out,"日历查询失败，请稍后重试。"); IntentHandler.writeSse(out,"[DONE]"); }
        return true;
    }
    private String typeLabel(String type) {
        return switch(type==null?"":type) {
            case "todo" -> "待办"; case "commemoration" -> "纪念日"; case "menstruation" -> "生理期";
            case "bookkeeping" -> "记账"; case "meal", "meal_plan" -> "餐饮"; case "subscription" -> "订阅"; default -> "日历事项";
        };
    }
}
