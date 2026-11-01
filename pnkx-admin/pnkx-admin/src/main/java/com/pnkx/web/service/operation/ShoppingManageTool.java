package com.pnkx.web.service.operation;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.PxShoppingItem;
import com.pnkx.life.mapper.PxShoppingItemMapper;
import com.pnkx.life.service.impl.ShoppingActionService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShoppingManageTool {
    private final JdbcTemplate db;
    private final DataPermissionService permissions;
    private final PxShoppingItemMapper items;
    private final ShoppingActionService actions;
    public ShoppingManageTool(JdbcTemplate db, DataPermissionService permissions,
                              PxShoppingItemMapper items, ShoppingActionService actions) {
        this.db=db; this.permissions=permissions; this.items=items; this.actions=actions;
    }
    public List<Map<String,Object>> find(JSONObject input) {
        StringBuilder sql=new StringBuilder("select i.id,i.name,i.list_id listId,l.name listName,i.fulfillment_state state,i.claimed_by claimedBy,i.revision from px_shopping_item i join px_shopping_list l on l.id=i.list_id where i.del_flag='0' and l.del_flag='0'");
        List<Object> args=new ArrayList<>();
        var visible=permissions.getVisibleUserIds("shopping");
        if (visible!=null) {
            if (visible.isEmpty()) return List.of();
            sql.append(" and l.create_by in (").append(String.join(",",Collections.nCopies(visible.size(),"?"))).append(")");
            args.addAll(visible);
        }
        if (input.getLong("itemId")!=null) { sql.append(" and i.id=?"); args.add(input.getLong("itemId")); }
        if (input.getLong("listId")!=null) { sql.append(" and l.id=?"); args.add(input.getLong("listId")); }
        String keyword=input.getString("keyword");
        if (keyword!=null && !keyword.isBlank()) { sql.append(" and instr(i.name,?)>0"); args.add(keyword.trim()); }
        sql.append(" order by i.id limit 21");
        return db.queryForList(sql.toString(),args.toArray());
    }
    public void prepare(JSONObject draft, String owner) {
        if (draft.getLong("itemId")==null && (draft.getString("keyword")==null || draft.getString("keyword").isBlank()))
            throw new ServiceException("请提供商品名称或编号");
        var matches=find(draft);
        if (matches.size()!=1) throw new ServiceException(matches.isEmpty()?"没有找到可操作的商品":"存在多条匹配商品，请先查询并指定商品编号");
        var row=matches.get(0);
        draft.put("itemId",row.get("id"));
        draft.put("expectedRevision",row.get("revision"));
        draft.put("target",new JSONObject(row));
        validate(draft,owner,false);
    }
    public void validate(JSONObject input,String owner,boolean lock) {
        String action=input.getString("action");
        if (action==null || !Set.of("claim","release","fulfillment").contains(action)) throw new ServiceException("不支持的购物操作");
        Long id=input.getLong("itemId");
        if (id==null || input.getLong("expectedRevision")==null) throw new ServiceException("缺少商品或版本，请重新预览");
        if(lock) db.queryForList("select id from px_shopping_item where id=? for update",id);
        actions.checkAccess(id);
        var item=items.selectPxShoppingItemById(id);
        if(item==null || !"0".equals(item.getDelFlag())) throw new ServiceException("商品不存在或已删除");
        if(!Objects.equals(item.getRevision(),input.getLong("expectedRevision"))) throw new ServiceException("商品已变化，请重新预览");
        if(item.getBatchId()!=null) throw new ServiceException("已结算商品不能修改");
        if("fulfillment".equals(action)) {
            String state=input.getString("state");
            if(state==null || !Set.of("needed","available","purchased").contains(state)) throw new ServiceException("请选择有效采购状态");
        } else {
            if(!"needed".equals(item.getFulfillmentState())) throw new ServiceException("请先将商品改为待购，再认领或释放认领");
            if("claim".equals(action) && item.getClaimedBy()!=null && !owner.equals(item.getClaimedBy())) throw new ServiceException("商品已被其他成员认领");
            if("release".equals(action) && item.getClaimedBy()!=null && !owner.equals(item.getClaimedBy())) {
                var rows=db.queryForList("select id from px_shopping_list where id=? and create_by=?",item.getListId(),owner);
                if(rows.isEmpty()) throw new ServiceException("只能本人或清单所有者释放认领");
            }
        }
    }
    public JSONObject run(JSONObject input,String owner) {
        validate(input,owner,true);
        PxShoppingItem item=actions.act(input.getLong("itemId"),input.getString("action"),input,owner);
        return new JSONObject().fluentPut("sourceType","shopping_mutation").fluentPut("sourceId",item.getId())
                .fluentPut("state",item.getFulfillmentState()).fluentPut("claimedBy",item.getClaimedBy());
    }
}
