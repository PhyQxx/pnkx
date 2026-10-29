package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Explicit per-resource grants; links never grant write access. */
@Service
public class NoteShareService {
    private final JdbcTemplate db;
    public NoteShareService(JdbcTemplate db) { this.db = db; }

    private String table(String type) {
        if ("note".equals(type)) return "px_note";
        if ("folder".equals(type)) return "px_note_folder";
        throw new ServiceException("不支持的共享类型");
    }

    Map<String, Object> resource(String type, Long id) {
        String fields = "folder".equals(type) ? "name title,parent_id parent" : "title,folder parent";
        var rows = db.queryForList("select id,create_by owner," + fields + " from " + table(type)
                + " where id=? and del_flag=0", id);
        if (rows.isEmpty()) throw new ServiceException("内容不存在或已删除");
        return rows.get(0);
    }

    public void requireOwner(String type, Long id, String actor) {
        if (actor == null || !actor.equals(String.valueOf(resource(type, id).get("owner"))))
            throw new ServiceException("只有所有者可以执行此操作");
    }

    // Walk upwards using current ancestry: moving a resource out immediately removes inherited access.
    boolean contains(Map<String, Object> grant, String type, Long id) {
        String owner = String.valueOf(grant.get("owner_id"));
        Set<Long> visited = new HashSet<>();
        while (id != null && id > 0 && visited.size() < 256) {
            Map<String, Object> row;
            try { row = resource(type, id); } catch (ServiceException e) { return false; }
            if (!owner.equals(String.valueOf(row.get("owner")))) return false;
            if (type.equals(grant.get("resource_type"))
                    && id.toString().equals(String.valueOf(grant.get("resource_id")))) return true;
            if (!"folder".equals(grant.get("resource_type"))) return false;
            if ("folder".equals(type) && !visited.add(id)) return false;
            Object parent = row.get("parent");
            id = parent == null ? null : Long.valueOf(parent.toString());
            type = "folder";
        }
        return false;
    }

    public void requireAccess(String type, Long id, String actor, boolean write) {
        if (actor == null) throw new ServiceException("请先登录");
        if (actor.equals(String.valueOf(resource(type, id).get("owner")))) return;
        var grants = db.queryForList("select * from px_note_share where recipient_id=? and revoked=0"
                + " and (expires_at is null or expires_at>now())" + (write ? " and permission='edit'" : ""), actor);
        for (var grant : grants) if (contains(grant, type, id)) return;
        throw new ServiceException(write ? "没有编辑权限" : "没有访问权限");
    }

    public List<Map<String, Object>> managed(String type, Long id, String actor) {
        requireOwner(type, id, actor);
        return db.queryForList("select s.id,s.token,s.permission,s.expires_at,s.recipient_id,u.user_name recipient_name"
                + " from px_note_share s left join sys_user u on u.user_id=s.recipient_id"
                + " where s.resource_type=? and s.resource_id=? and s.owner_id=? and s.revoked=0 order by s.id desc", type,id,actor);
    }

    @Transactional
    public List<Map<String, Object>> create(String type, Long id, String actor, String username, String permission, Integer days) {
        requireOwner(type,id,actor);
        if (permission == null || !Set.of("read", "edit").contains(permission)) throw new ServiceException("权限无效");
        if (days == null || days < 0 || days > 365) throw new ServiceException("有效期应为0至365天，0表示永久");
        String recipient = null;
        String token = null;
        if (username == null || username.isBlank()) {
            if (!"read".equals(permission)) throw new ServiceException("分享链接仅支持只读");
            token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        } else {
            var users = db.queryForList("select user_id from sys_user where user_name=? and status='0' and del_flag='0'", username.trim());
            if (users.isEmpty()) throw new ServiceException("用户不存在或已停用");
            recipient = String.valueOf(users.get(0).get("user_id"));
            if (actor.equals(recipient)) throw new ServiceException("无需共享给自己");
            db.update("update px_note_share set revoked=1 where resource_type=? and resource_id=? and owner_id=? and recipient_id=?",type,id,actor,recipient);
        }
        db.update("insert into px_note_share(resource_type,resource_id,owner_id,recipient_id,token,permission,expires_at)"
                + " values(?,?,?,?,?,?,?)",type,id,actor,recipient,token,permission,
                days == 0 ? null : new java.sql.Timestamp(System.currentTimeMillis() + days * 86400000L));
        return managed(type,id,actor);
    }

    public void revoke(Long id, String actor) {
        if (db.update("update px_note_share set revoked=1 where id=? and owner_id=?",id,actor) == 0)
            throw new ServiceException("共享不存在或无权取消");
    }

    public List<Map<String, Object>> received(String actor) {
        var grants = db.queryForList("select id,resource_type,resource_id,owner_id,permission from px_note_share"
                + " where recipient_id=? and revoked=0 and (expires_at is null or expires_at>now()) order by id desc",actor);
        var result = new ArrayList<Map<String,Object>>();
        for (var g : grants) {
            try {
                var row = resource(g.get("resource_type").toString(),Long.valueOf(g.get("resource_id").toString()));
                if (!String.valueOf(row.get("owner")).equals(String.valueOf(g.get("owner_id")))) continue;
                g.put("title",row.get("title"));
                result.add(g);
            } catch (ServiceException ignored) { /* deleted resource */ }
        }
        return result;
    }

    public Map<String,Object> browse(Long shareId, String token, String actor, String type, Long id) {
        boolean publicLink = token != null;
        var grants = publicLink
                ? db.queryForList("select * from px_note_share where token=? and recipient_id is null and revoked=0 and (expires_at is null or expires_at>now())",token)
                : db.queryForList("select * from px_note_share where id=? and recipient_id=? and revoked=0 and (expires_at is null or expires_at>now())",shareId,actor);
        if (grants.isEmpty()) throw new ServiceException("共享已失效或无权访问");
        var grant = grants.get(0);
        if (id == null) { type = grant.get("resource_type").toString(); id = Long.valueOf(grant.get("resource_id").toString()); }
        if (!contains(grant,type,id)) throw new ServiceException("内容不在共享范围内");
        var result = new HashMap<String,Object>(resource(type,id));
        result.remove("owner"); result.remove("parent");
        result.put("type",type);
        result.put("editable",!publicLink && "edit".equals(grant.get("permission")));
        if ("note".equals(type)) {
            result.putAll(db.queryForMap("select content,rich_text richText from px_note where id=?",id));
        } else {
            var children = db.queryForList("select id,name title,'folder' type from px_note_folder where parent_id=? and create_by=? and del_flag=0 order by name",id,grant.get("owner_id"));
            children.addAll(db.queryForList("select id,title,'note' type from px_note where folder=? and create_by=? and del_flag=0 order by update_time desc",id,grant.get("owner_id")));
            result.put("children",children);
        }
        return result;
    }
}
