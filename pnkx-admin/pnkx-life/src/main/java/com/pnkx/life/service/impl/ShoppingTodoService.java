package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.life.mapper.PxToDoMapper;
import com.pnkx.life.service.IPxToDoService;
import jakarta.annotation.Resource;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingTodoService {
    public static final String LABEL = "采购";
    @Resource private JdbcTemplate db;
    @Resource private com.pnkx.life.mapper.PxShoppingItemMapper shoppingItems;
    @Resource private com.pnkx.life.mapper.PxShoppingListMapper shoppingLists;
    @Resource private PxToDoMapper mapper;
    @Resource private IPxToDoService todos;
    @Resource private DataPermissionService permissions;

    public static boolean hasLabel(String labels) {
        return labels != null && Arrays.stream(labels.split(",")).map(String::trim).anyMatch(LABEL::equals);
    }

    private PxToDo requireTodo(Long id, boolean write) {
        PxToDo todo = write ? mapper.lockById(id) : mapper.selectPxToDoById(id);
        if (todo == null || !allowed(todo.getCreateBy(), "todo", write)
                || !allowed(todo.getCreateBy(), "shopping", write))
            throw new ServiceException("待办不存在或无权访问关联购物项");
        return todo;
    }

    private boolean allowed(String owner, String module, boolean write) {
        if (write) return permissions.canWrite(owner, module);
        List<Long> ids = permissions.getVisibleUserIds(module);
        return ids == null || ids.stream().anyMatch(id -> String.valueOf(id).equals(owner));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long arrange(Long todoId, String title, String deadline, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty() || itemIds.size() > 100 || itemIds.stream().anyMatch(Objects::isNull))
            throw new ServiceException("请选择1至100个购物项");
        PxToDo todo;
        if (todoId == null) {
            if (title == null || title.isBlank() || title.length() > 200) throw new ServiceException("请填写采购待办名称（最多200字）");
            if (deadline == null || deadline.isBlank()) throw new ServiceException("请选择采购时间");
            try { java.time.LocalDateTime.parse(deadline, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
            catch (Exception e) { throw new ServiceException("采购时间格式不正确"); }
            todo = new PxToDo();
            todo.setContent(title.trim()); todo.setLabel(LABEL); todo.setStatus(false); todo.setKanbanStatus(0);
            todo.setCreateBy(SecurityUtils.getUserId()); todo.setPerformer(SecurityUtils.getUserId());
            todo.setPlanEndTime(deadline);
            todos.insertPxToDo(todo);
        } else { todo = requireTodo(todoId, true); }
        if (!hasLabel(todo.getLabel()) || Boolean.TRUE.equals(todo.getStatus()) || Integer.valueOf(2).equals(todo.getKanbanStatus()))
            throw new ServiceException("只能关联未完成且带有采购标签的待办");
        for (Long id : new TreeSet<>(itemIds)) {
            var rows = db.queryForList("select create_by,del_flag from px_shopping_item where id=? for update", id);
            if (rows.isEmpty() || !"0".equals(rows.get(0).get("del_flag"))
                    || !Objects.equals(todo.getCreateBy(), rows.get(0).get("create_by"))
                    || !permissions.canWrite(String.valueOf(rows.get(0).get("create_by")), "shopping"))
                throw new ServiceException("商品须与待办属于同一创建者，且具有购物编辑权限");
            var links = db.queryForList("select todo_id from px_shopping_todo_link where item_id=?", id);
            if (!links.isEmpty()) {
                if (((Number) links.get(0).get("todo_id")).longValue() == todo.getId()) continue;
                throw new ServiceException("商品已关联其他采购待办，请先解除关联");
            }
            db.update("insert into px_shopping_todo_link(item_id,todo_id) values (?,?)", id, todo.getId());
        }
        return todo.getId();
    }

    public Map<String,Object> choices(Long todoId) {
        PxToDo todo = requireTodo(todoId, false);
        return Map.of("lists", db.queryForList("select id,name from px_shopping_list where create_by=? and del_flag='0' order by id desc", todo.getCreateBy()),
            "items", db.queryForList("select i.id,i.name from px_shopping_item i where i.create_by=? and i.del_flag='0' and coalesce(i.checked,0)=0 and coalesce(i.fulfillment_state,'needed')='needed' and not exists (select 1 from px_shopping_todo_link l where l.item_id=i.id) order by i.id desc", todo.getCreateBy()));
    }

    /** 新购物项和关联在同一事务内创建，失败不留下半成品清单。 */
    @Transactional(rollbackFor = Exception.class)
    public Long createShopping(Long todoId, Long listId, List<String> names) {
        PxToDo todo = requireTodo(todoId, true);
        if (!hasLabel(todo.getLabel()) || Boolean.TRUE.equals(todo.getStatus()) || Integer.valueOf(2).equals(todo.getKanbanStatus()))
            throw new ServiceException("请先保存采购标签，且待办必须未完成");
        if (names == null || names.isEmpty() || names.size() > 100 || names.stream().anyMatch(n -> n == null || n.isBlank() || n.trim().length() > 128))
            throw new ServiceException("请填写1至100个商品，每个名称不超过128字");
        if (listId == null) {
            var list = new com.pnkx.life.domain.po.PxShoppingList();
            String title = todo.getContent() == null ? "采购清单" : todo.getContent();
            list.setName(title.substring(0, Math.min(title.length(),64)));
            list.setCreateBy(todo.getCreateBy()); list.setCreateTime(new java.util.Date()); list.setDelFlag("0");
            shoppingLists.insertPxShoppingList(list); listId = list.getId();
        } else {
            var rows = db.queryForList("select create_by,del_flag from px_shopping_list where id=? for update", listId);
            if (rows.isEmpty() || !"0".equals(rows.get(0).get("del_flag")) || !Objects.equals(todo.getCreateBy(), rows.get(0).get("create_by")))
                throw new ServiceException("请选择同一创建者的有效购物清单");
        }
        for (String name : names) {
            var item = new com.pnkx.life.domain.po.PxShoppingItem();
            item.setName(name.trim()); item.setQuantity("1"); item.setListId(listId);
            item.setCreateBy(todo.getCreateBy()); item.setCreateTime(new java.util.Date()); item.setDelFlag("0"); item.setChecked(false);
            shoppingItems.insertPxShoppingItem(item);
            db.update("insert into px_shopping_todo_link(item_id,todo_id) values (?,?)", item.getId(), todoId);
        }
        return listId;
    }

    public Map<String,Object> items(Long todoId) {
        PxToDo todo = requireTodo(todoId, false);
        var items = db.queryForList("select i.id,i.name,i.quantity,i.checked,i.fulfillment_state,i.batch_id,i.revision from px_shopping_item i join px_shopping_todo_link l on l.item_id=i.id where l.todo_id=? and i.del_flag='0' order by i.id", todoId);
        return Map.of("items", items, "completed", Boolean.TRUE.equals(todo.getStatus()) || Integer.valueOf(2).equals(todo.getKanbanStatus()),
                "canEdit", permissions.canWrite(todo.getCreateBy(), "todo") && permissions.canWrite(todo.getCreateBy(), "shopping"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlink(Long todoId, Long itemId) {
        requireTodo(todoId, true);
        db.update("delete from px_shopping_todo_link where todo_id=? and item_id=?", todoId, itemId);
    }
}
