package com.pnkx.web.controller.life;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.life.service.impl.ShoppingTodoService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shoppingTodo")
public class PxShoppingTodoController {
    @Resource private ShoppingTodoService service;
    public record Arrange(Long todoId, String title, String deadline, List<Long> itemIds) {}
    @PostMapping
    public AjaxResult arrange(@RequestBody Arrange request) {
        return AjaxResult.success(service.arrange(request.todoId(), request.title(), request.deadline(), request.itemIds()));
    }
    public record CreateShopping(Long listId, List<String> names) {}
    @PostMapping("/{todoId}/items")
    public AjaxResult createShopping(@PathVariable Long todoId, @RequestBody CreateShopping request) {
        return AjaxResult.success(service.createShopping(todoId, request.listId(), request.names()));
    }
    @GetMapping("/{todoId}/choices")
    public AjaxResult choices(@PathVariable Long todoId) { return AjaxResult.success(service.choices(todoId)); }
    @GetMapping("/{todoId}")
    public AjaxResult items(@PathVariable Long todoId) { return AjaxResult.success(service.items(todoId)); }
    @DeleteMapping("/{todoId}/{itemId}")
    public AjaxResult unlink(@PathVariable Long todoId, @PathVariable Long itemId) {
        service.unlink(todoId,itemId); return AjaxResult.success();
    }
}
