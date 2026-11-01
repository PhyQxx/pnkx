package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.webarticle.WebArticleUtils;
import com.pnkx.web.service.operation.AssignmentTool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.io.OutputStream;

@Component
public class AssignmentHandler implements ConfirmableIntentHandler {
    @Resource private AiClient ai;
    @Resource private AssignmentTool assignments;
    @Resource private LocalIntentPlanService plans;
    @Resource private AiPendingActionService pending;
    public String intentName() { return "assignment"; }
    public String promptDescription() {
        return "查询待办或购物商品的负责人、可转派成员、分工记录，或将事项转派给指定成员。需明确事项编号与成员ID；转派需原因且确认后执行。slots:{action:query/transfer,sourceType:todo/shopping,sourceId,assignee,reason,page}";
    }
    public boolean handle(String question, JSONObject slots, OutputStream out) throws java.io.IOException {
        try {
            var response = ai.chat("""
                提取分工请求，仅返回JSON：
                {"action":"query|transfer","sourceType":"todo|shopping","sourceId":null,"assignee":null,"reason":null,"page":1}
                sourceId仅能使用用户明确提供的事项编号，assignee仅能使用明确的成员ID，不能从姓名、手机号或关系猜测ID。
                不清楚的字段填null，不补造转派原因；只查询负责人/候选人/记录用query。
                """, question);
            var draft = WebArticleUtils.extractJson(response == null ? null : response.getString("content"));
            if (draft == null) throw new ServiceException("请说明事项类型、编号，以及查询分工还是转派");
            String actor = SecurityUtils.getUserId();
            if ("query".equals(draft.getString("action"))) {
                var detail = assignments.detail(draft, actor);
                StringBuilder text = new StringBuilder("**分工与转派记录**\n\n当前负责人：")
                        .append(member(detail.getString("assignee"))).append("\n\n");
                var candidates = detail.getJSONArray("candidates");
                if (detail.getBooleanValue("canTransfer")) {
                    text.append("可转派成员ID：");
                    for (int i=0; candidates != null && i<candidates.size(); i++)
                        text.append(candidates.getJSONObject(i).getString("userId")).append(" ");
                } else text.append("当前账号可查看记录，无转派权限。");
                var history = detail.getJSONArray("history");
                text.append("\n\n");
                if (history == null || history.isEmpty()) text.append("本页没有分工记录。");
                else for (int i=0;i<history.size();i++) {
                    var h=history.getJSONObject(i);
                    text.append("- ").append(member(h.getString("previous_assignee"))).append(" → ")
                            .append(member(h.getString("assignee"))).append("：").append(h.getString("reason"))
                            .append("（").append(h.getString("create_time")).append("）\n");
                }
                IntentHandler.writeSse(out,text.toString()); IntentHandler.writeSse(out,"[DONE]"); return true;
            }
            if (!"transfer".equals(draft.getString("action"))) throw new ServiceException("仅支持查询分工或转派");
            assignments.prepare(draft,actor);
            plans.prepare(draft,"assignment.transfer","转派事项");
            pending.save(slots.getString("requestId"),intentName(),draft);
            IntentHandler.writePlanEvent(out,draft);
            IntentHandler.writeSse(out,"**转派预览**\n\n- 事项："+("todo".equals(draft.getString("sourceType"))?"待办":"购物商品")
                    +" #"+draft.getLong("sourceId")+"\n- 负责人："+member(draft.getString("expectedAssignee"))+" → "+member(draft.getString("assignee"))
                    +"\n- 原因："+draft.getString("reason")+"\n\n[PENDING_CONFIRM]");
        } catch(ServiceException e) { IntentHandler.writeSse(out,e.getMessage()); IntentHandler.writeSse(out,"[DONE]"); }
        catch(Exception e) { IntentHandler.writeSse(out,"分工查询或预览失败，尚未执行转派，请重试。"); IntentHandler.writeSse(out,"[DONE]"); }
        return true;
    }
    private String member(String id) { return id == null || id.isBlank() ? "未分配" : "成员 "+id; }
    public boolean confirm(JSONObject draft,OutputStream out) throws java.io.IOException { return plans.confirm(draft,out); }
}
