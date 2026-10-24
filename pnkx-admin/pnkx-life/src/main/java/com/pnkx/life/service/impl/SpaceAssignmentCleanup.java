package com.pnkx.life.service.impl;

import com.pnkx.common.event.SpaceMembershipChanged;
import com.pnkx.system.service.ISysDataGroupService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class SpaceAssignmentCleanup {
  @jakarta.annotation.Resource private AssignmentService assignments;
  private final JdbcTemplate db;
  private final ISysDataGroupService spaces;

  public SpaceAssignmentCleanup(JdbcTemplate db, ISysDataGroupService spaces) {
    this.db = db;
    this.spaces = spaces;
  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void changed(SpaceMembershipChanged event) {
    String user = event.userId().toString();
    for (var row :
        db.queryForList(
            "select i.id,l.create_by from px_shopping_item i join px_shopping_list l on"
                + " l.id=i.list_id where i.claimed_by=? and i.del_flag='0' and"
                + " i.fulfillment_state='needed' and i.batch_id is null",
            user)) {
      String owner = String.valueOf(row.get("create_by"));
      if (user.equals(owner)) continue;
      if (!canShare(event.userId(), owner, "shopping")) {
        int changed =
            db.update(
                "update px_shopping_item set claimed_by=null,claimed_at=null,revision=revision+1"
                    + " where id=? and claimed_by=? and fulfillment_state='needed' and batch_id is"
                    + " null",
                row.get("id"),
                user);
        if (changed > 0 && assignments != null)
          assignments.record(
              "shopping",
              ((Number) row.get("id")).longValue(),
              owner,
              user,
              null,
              "system",
              "membership_removed",
              "成员退出或权限变化，释放未完成认领");
      }
    }
    for (var row :
        db.queryForList(
            "select id,create_by,performer from px_to_do where FIND_IN_SET(?,performer)>0 and"
                + " status=0",
            user)) {
      String owner = String.valueOf(row.get("create_by"));
      if (user.equals(owner)) continue;
      if (!canShare(event.userId(), owner, "todo")) {
        String previous = String.valueOf(row.get("performer"));
        String target =
            java.util.Arrays.stream(previous.split(","))
                .filter(id -> !id.equals(user))
                .collect(java.util.stream.Collectors.joining(","));
        if (target.isBlank()) target = owner;
        int changed =
            db.update(
                "update px_to_do set performer=?,update_time=now() where id=? and performer=? and"
                    + " status=0",
                target,
                row.get("id"),
                previous);
        if (changed > 0 && assignments != null)
          assignments.record(
              "todo",
              ((Number) row.get("id")).longValue(),
              owner,
              previous,
              target,
              "system",
              "membership_removed",
              "成员退出或权限变化，移除失效执行者；无人负责时恢复为所有者");
      }
    }
  }

  private boolean canShare(Long user, String owner, String module) {
    try {
      return spaces.canShare(user, Long.valueOf(owner), module);
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
