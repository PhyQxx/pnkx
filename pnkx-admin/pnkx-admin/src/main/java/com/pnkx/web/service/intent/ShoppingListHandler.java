package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.domain.po.PxShoppingItem;
import com.pnkx.life.domain.po.PxShoppingList;
import com.pnkx.life.service.IPxShoppingItemService;
import com.pnkx.life.service.IPxShoppingListService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.List;

/**
 * 购物清单意图处理器（向购物清单添加商品）。
 * 用户未指定具体清单时，默认加入最近使用的清单；若无任何清单则自动创建一个默认清单。
 */
@Component
public class ShoppingListHandler implements ConfirmableIntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingListHandler.class);

    private static final String PARSE_PROMPT = """
            你是一个购物清单解析器。从用户输入中提取要购买的商品信息，返回 JSON：
            {
              "items": [
                {"name": "商品名称", "quantity": "数量/规格，如 2斤、一盒，无法判断则为 null"}
              ]
            }
            注意：用户可能一次提到多个商品，都要提取出来。
            只返回 JSON，不要返回其他内容。
            """;

    @Resource
    private AiClient aiClient;

    @Resource private LocalIntentPlanService localPlans;
    @Resource private com.pnkx.web.service.operation.ShoppingCreateTool shoppingCreates;

    @Resource
    private IPxShoppingItemService shoppingItemService;

    @Resource
    private IPxShoppingListService shoppingListService;

    @Resource
    private AiPendingActionService pendingActionService;

    @Override
    public String intentName() {
        return "shopping_list";
    }

    @Override
    public String promptDescription() {
        return "用户想往购物清单里添加要买的东西。slots: {\"items\": [{\"name\": \"商品名\", \"quantity\": \"数量\"}]}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) {
        try {
            JSONObject result = aiClient.chat(PARSE_PROMPT, question);
            if (result == null || result.getString("content") == null) {
                return false;
            }

            JSONObject parsed = parseJsonObject(result.getString("content"));
            List<JSONObject> items = extractItems(parsed);
            if (items.isEmpty()) {
                return false;
            }

            parsed.put("items", new com.alibaba.fastjson.JSONArray(items));
            shoppingCreates.prepare(parsed, SecurityUtils.getUserId());
            localPlans.prepare(parsed,"shopping.add","添加购物清单商品");
            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, buildDraftMessage(items, parsed));
            return true;
        } catch (Exception e) {
            logger.error("AI购物清单解析失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) throws java.io.IOException {
        return localPlans.confirm(draft,out);
    }

    /**
     * 解析出商品列表；兼容 AI 返回 items 数组或单个对象
     */
    @SuppressWarnings("unchecked")
    private List<JSONObject> extractItems(JSONObject parsed) {
        Object itemsObj = parsed.get("items");
        if (itemsObj instanceof List) {
            return (List<JSONObject>) itemsObj;
        }
        // 兼容 AI 偶尔返回单对象的情况
        if (parsed.containsKey("name")) {
            return List.of(parsed);
        }
        return List.of();
    }

    private JSONObject parseJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return new JSONObject();
        }
        return JSON.parseObject(content.substring(start, end + 1));
    }

    private String buildDraftMessage(List<JSONObject> items, JSONObject draft) {
        StringBuilder msg = new StringBuilder();
        msg.append("**购物清单草稿**\n\n");
        msg.append("目标：").append(draft.getString("listName")).append(draft.getBooleanValue("createList") ? "（确认后新建）" : "（已有清单）").append("\n\n");
        appendItems(msg, items);
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String buildSuccessMessage(List<JSONObject> items) {
        StringBuilder msg = new StringBuilder();
        msg.append("**已加入购物清单**\n\n");
        appendItems(msg, items);
        return msg.toString();
    }

    private void appendItems(StringBuilder msg, List<JSONObject> items) {
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.get(i);
            msg.append(i + 1).append(". ").append(item.getString("name"));
            if (isRealValue(item.getString("quantity"))) {
                msg.append("（").append(item.getString("quantity")).append("）");
            }
            msg.append("\n");
        }
    }

    private boolean isRealValue(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
