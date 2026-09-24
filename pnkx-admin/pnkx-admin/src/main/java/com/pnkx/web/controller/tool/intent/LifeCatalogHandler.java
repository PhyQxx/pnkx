package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxBook;
import com.pnkx.domain.po.PxRecipe;
import com.pnkx.domain.po.PxSubscription;
import com.pnkx.service.CalendarAggregateService;
import com.pnkx.service.IPxBookService;
import com.pnkx.service.IPxBookkeepingBudgetService;
import com.pnkx.service.IPxRecipeService;
import com.pnkx.service.IPxSubscriptionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.YearMonth;

/** 订阅、预算、阅读、菜谱和日历的统一只读工具。 */
@Component
public class LifeCatalogHandler implements IntentHandler {
    @Resource private IPxSubscriptionService subscriptionService;
    @Resource private IPxBookkeepingBudgetService budgetService;
    @Resource private IPxBookService bookService;
    @Resource private IPxRecipeService recipeService;
    @Resource private CalendarAggregateService calendarService;

    @Override
    public String intentName() { return "life_catalog"; }

    @Override
    public String promptDescription() {
        return "用户想查询订阅、预算、阅读、菜谱或日历。slots: {\"category\":\"subscription/budget/book/recipe/calendar\",\"keyword\":\"可选关键词\",\"month\":\"yyyy-MM\"}";
    }

    @Override
    public boolean handle(String question, JSONObject slots, OutputStream out) {
        try {
            String category = normalizeCategory(slots.getString("category"), question);
            String keyword = slots.getString("keyword");
            Object data;
            switch (category) {
                case "subscription" -> {
                    PxSubscription query = new PxSubscription();
                    query.setName(keyword);
                    data = subscriptionService.selectPxSubscriptionList(query);
                }
                case "budget" -> data = budgetService.getBudgetStatus(
                        validMonth(slots.getString("month")) ? slots.getString("month") : YearMonth.now().toString());
                case "book" -> {
                    PxBook query = new PxBook();
                    query.setCreateBy(SecurityUtils.getUserId());
                    query.setTitle(keyword);
                    data = bookService.selectBookList(query);
                }
                case "recipe" -> {
                    PxRecipe query = new PxRecipe();
                    query.setTitle(keyword);
                    data = recipeService.selectPxRecipeList(query);
                }
                case "calendar" -> {
                    LocalDate start = LocalDate.now();
                    data = calendarService.getMonthEvents(SecurityUtils.getUserId(), start.toString(), start.plusDays(30).toString());
                }
                default -> { return false; }
            }
            String json = JSONObject.toJSONString(data);
            if (json.length() > 8000) json = json.substring(0, 8000) + "…";
            IntentHandler.writeSse(out, "**查询结果（" + categoryLabel(category) + "）**\n\n```json\n" + json + "\n```");
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalizeCategory(String category, String question) {
        if (category != null && !category.isBlank()) return category;
        if (question.contains("订阅")) return "subscription";
        if (question.contains("预算")) return "budget";
        if (question.contains("书") || question.contains("阅读")) return "book";
        if (question.contains("菜谱") || question.contains("食谱")) return "recipe";
        if (question.contains("日历") || question.contains("安排")) return "calendar";
        return "";
    }

    private boolean validMonth(String value) { return value != null && value.matches("\\d{4}-\\d{2}"); }

    private String categoryLabel(String category) {
        return switch (category) {
            case "subscription" -> "订阅";
            case "budget" -> "预算";
            case "book" -> "阅读";
            case "recipe" -> "菜谱";
            case "calendar" -> "日历";
            default -> "生活数据";
        };
    }
}
