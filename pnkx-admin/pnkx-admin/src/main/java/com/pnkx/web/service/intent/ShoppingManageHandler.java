package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.webarticle.WebArticleUtils;
import com.pnkx.web.service.operation.ShoppingManageTool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.io.OutputStream;

@Component
public class ShoppingManageHandler implements ConfirmableIntentHandler {
    @Resource private AiClient ai;
    @Resource private ShoppingManageTool shopping;
    @Resource private LocalIntentPlanService plans;
    @Resource private AiPendingActionService pending;
    public String intentName() { return "shopping_manage"; }
    public String promptDescription() {
        return "查询已有购物商品，认领/释放认领，标记家里已有、已买到、改为待购。新增商品用shopping_list，转派使用assignment。slots:{action:query/claim/release/fulfillment,itemId,keyword,listId,state:needed/available/purchased}";
    }
    public boolean handle(String question,JSONObject slots,OutputStream out) throws java.io.IOException {
        try {
            var response=ai.chat("""
                提取已有购物商品操作，只返回JSON：
                {"action":"query|claim|release|fulfillment","itemId":null,"keyword":null,"listId":null,"state":null}
                ID只能来自用户明确提供的编号，不得猜测；名称放keyword。
                家里已有=available，已买到=purchased，改为待购=needed，均为fulfillment。
                我来买=claim，释放认领=release。查看清单=query。转派或新增不属于此工具，action=unsupported。
                """,question);
            var draft=WebArticleUtils.extractJson(response==null?null:response.getString("content"));
            if(draft==null) throw new ServiceException("未能解析购物操作，请提供商品和具体操作");
            if("query".equals(draft.getString("action"))) {
                var rows=shopping.find(draft);
                StringBuilder message=new StringBuilder("**购物商品查询**\n\n");
                if(rows.isEmpty()) message.append("没有匹配的商品。");
                rows.stream().limit(20).forEach(row -> message.append("- #").append(row.get("id")).append(" ")
                        .append(row.get("name")).append("（清单：").append(row.get("listName")).append("，状态：")
                        .append(label(String.valueOf(row.get("state")))).append("）\n"));
                if(rows.size()>20) message.append("仅显示前20条，请缩小查询范围。");
                IntentHandler.writeSse(out,message.toString());
                IntentHandler.writeSse(out,"[DONE]");
                return true;
            }
            shopping.prepare(draft,SecurityUtils.getUserId());
            plans.prepare(draft,"shopping.manage","修改购物商品");
            pending.save(slots.getString("requestId"),intentName(),draft);
            IntentHandler.writePlanEvent(out,draft);
            var target=draft.getJSONObject("target");
            IntentHandler.writeSse(out,"**购物操作预览**\n\n- 商品：#"+draft.getLong("itemId")+" "+target.getString("name")
                    +"\n- 清单："+target.getString("listName")+"\n- 当前状态："+label(target.getString("state"))
                    +"\n- 操作："+label("fulfillment".equals(draft.getString("action"))?draft.getString("state"):draft.getString("action"))
                    +"\n\n[PENDING_CONFIRM]");
        } catch(ServiceException e) {
            IntentHandler.writeSse(out,e.getMessage()); IntentHandler.writeSse(out,"[DONE]");
        } catch(Exception e) {
            IntentHandler.writeSse(out,"购物操作预览失败，尚未执行修改，请重试。"); IntentHandler.writeSse(out,"[DONE]");
        }
        return true;
    }
    private String label(String value) {
        return switch(value==null?"":value) {
            case "needed" -> "待购"; case "available" -> "家里已有"; case "purchased" -> "已买到";
            case "claim" -> "由我认领"; case "release" -> "释放认领"; default -> "未注明";
        };
    }
    public boolean confirm(JSONObject draft,OutputStream out) throws java.io.IOException { return plans.confirm(draft,out); }
}
