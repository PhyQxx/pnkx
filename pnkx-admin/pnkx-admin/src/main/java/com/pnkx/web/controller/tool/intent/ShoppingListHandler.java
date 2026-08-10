package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxShoppingItem;
import com.pnkx.domain.po.PxShoppingList;
import com.pnkx.service.IPxShoppingItemService;
import com.pnkx.service.IPxShoppingListService;
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

            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, buildDraftMessage(items));
            return true;
        } catch (Exception e) {
            logger.error("AI购物清单解析失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            List<JSONObject> items = extractItems(draft);
            if (items.isEmpty()) {
                return false;
            }

            String userId = SecurityUtils.getUserId();
            Long listId = resolveListId(userId);

            for (JSONObject item : items) {
                PxShoppingItem shoppingItem = buildShoppingItem(item, listId, userId);
                shoppingItemService.insertPxShoppingItem(shoppingItem);
            }

            IntentHandler.writeSse(out, buildSuccessMessage(items));
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        } catch (Exception e) {
            logger.error("AI购物清单确认保存失败: {}", e.getMessage());
            return false;
        }
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

    /**
     * 确定目标清单：用户未指定时，取最近一个清单；无清单则新建默认清单
     */
    private Long resolveListId(String userId) {
        PxShoppingList query = new PxShoppingList();
        query.setCreateBy(userId);
        List<PxShoppingList> lists = shoppingListService.selectPxShoppingListList(query);
        if (lists != null && !lists.isEmpty()) {
            return lists.get(0).getId();
        }
        // 自动创建默认清单
        PxShoppingList newList = new PxShoppingList();
        newList.setName("购物清单");
        newList.setCreateBy(userId);
        shoppingListService.insertPxShoppingList(newList);
        logger.info("AI购物清单：用户无清单，已自动创建默认清单");
        return newList.getId();
    }

    private PxShoppingItem buildShoppingItem(JSONObject item, Long listId, String userId) {
        PxShoppingItem shoppingItem = new PxShoppingItem();
        shoppingItem.setListId(listId);
        shoppingItem.setName(item.getString("name"));
        String quantity = item.getString("quantity");
        if (isRealValue(quantity)) {
            shoppingItem.setQuantity(quantity);
        }
        shoppingItem.setChecked(false);
        shoppingItem.setAddedFromMeal(false);
        shoppingItem.setCreateBy(userId);
        return shoppingItem;
    }

    private JSONObject parseJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return new JSONObject();
        }
        return JSON.parseObject(content.substring(start, end + 1));
    }

    private String buildDraftMessage(List<JSONObject> items) {
        StringBuilder msg = new StringBuilder();
        msg.append("**购物清单草稿**\n\n");
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
