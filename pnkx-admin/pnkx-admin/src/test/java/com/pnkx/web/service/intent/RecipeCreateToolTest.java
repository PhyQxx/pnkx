package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.life.domain.po.PxRecipe;
import com.pnkx.life.service.*;
import com.pnkx.web.service.operation.LocalCreateTools;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RecipeCreateToolTest {
    @Test void persistsIngredientsAndUsesStableIdempotencyKey() {
        var db = mock(JdbcTemplate.class);
        var recipes = mock(IPxRecipeService.class);
        var tool = new LocalCreateTools(db, mock(IPxNoteService.class), mock(IPxDiaryService.class), mock(IPxCommemorationDayService.class));
        ReflectionTestUtils.setField(tool, "recipes", recipes);
        var input = JSONObject.parseObject("{\"title\":\"蒸蛋\",\"notes\":\"蒸熟\",\"servings\":1,\"ingredients\":[{\"name\":\"鸡蛋\",\"quantity\":\"2个\"}]}");
        tool.validate("recipe.create", input, "1", false);
        when(recipes.insertPxRecipe(any())).thenAnswer(call -> {
            PxRecipe recipe = call.getArgument(0);
            assertEquals("plan:step1", recipe.getClientUuid());
            assertEquals("鸡蛋", recipe.getIngredients().get(0).getName());
            assertEquals("2个", recipe.getIngredients().get(0).getQuantity());
            assertEquals("0", recipe.getDelFlag());
            recipe.setId(42L);
            return 1;
        });
        when(db.queryForList(anyString(), eq(42L), eq("1")))
                .thenReturn(List.of(new HashMap<>(Map.of("id", 42L))));
        var result = tool.run("recipe.create", input, "step1", "1");
        assertEquals(42L, result.getLong("sourceId"));
        assertEquals("recipe", result.getString("sourceType"));
        input.put("ingredients", List.of());
        assertThrows(RuntimeException.class, () -> tool.validate("recipe.create", input, "1", false));
    }
}
