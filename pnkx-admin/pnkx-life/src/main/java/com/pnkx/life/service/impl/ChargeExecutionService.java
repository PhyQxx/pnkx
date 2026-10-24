package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ChargeExecutionService {
  private final JdbcTemplate db;

  public ChargeExecutionService(JdbcTemplate db) {
    this.db = db;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void failed(String owner, String type, Long source, LocalDate due) {
    db.update(
        "insert into px_charge_execution(owner_id,source_type,source_id,due_date,next_retry_at)"
            + " values(?,?,?,?,date_add(now(),interval 60 second)) on duplicate key update"
            + " status='failed',next_retry_at=date_add(now(),interval"
            + " LEAST(3600,60*POW(2,LEAST(attempt_count,6)))"
            + " second),attempt_count=attempt_count+1,update_time=now()",
        owner,
        type,
        source,
        due.toString());
  }

  public void resolved(String owner, String type, Long source, LocalDate due) {
    db.update(
        "update px_charge_execution set status='recovered',next_retry_at=null,update_time=now()"
            + " where owner_id=? and source_type=? and source_id=? and due_date=?",
        owner,
        type,
        source,
        due.toString());
  }

  public boolean ready(String owner, String type, Long source, LocalDate due) {
    return db.queryForObject(
            "select count(*) from px_charge_execution where owner_id=? and source_type=? and"
                + " source_id=? and due_date=? and status<>'recovered' and next_retry_at>now()",
            Integer.class,
            owner,
            type,
            source,
            due.toString())
        == 0;
  }

  public List<Map<String, Object>> list(String owner) {
    return db.queryForList(
        "select"
            + " e.id,e.source_type,e.source_id,e.due_date,e.error_code,e.attempt_count,e.update_time,e.next_retry_at,coalesce(s.name,r.name,'来源已移除')"
            + " source_name from px_charge_execution e left join px_subscription s on"
            + " e.source_type='subscription' and s.id=e.source_id and s.create_by=e.owner_id left"
            + " join px_bookkeeping_recurring r on e.source_type='recurring' and r.id=e.source_id"
            + " and r.create_by=e.owner_id where e.owner_id=? and e.status='failed' order by"
            + " e.update_time desc limit 30",
        owner);
  }

  public Map<String, Object> owned(Long id, String owner) {
    var rows =
        db.queryForList("select * from px_charge_execution where id=? and owner_id=?", id, owner);
    if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    return rows.get(0);
  }

  public void dismiss(Long id, String owner) {
    if (db.update(
            "update px_charge_execution set status='dismissed',update_time=now() where id=? and"
                + " owner_id=?",
            id,
            owner)
        != 1) throw new ServiceException("SOURCE_UNAVAILABLE");
  }
}
