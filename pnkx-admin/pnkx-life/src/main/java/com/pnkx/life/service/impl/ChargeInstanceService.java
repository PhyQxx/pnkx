package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargeInstanceService {
  private final PxChargeInstanceMapper instances;
  private final PxBookkeepingRecordMapper records;
  private final PxBookkeepingAccountMapper accounts;
  private final PxBookkeepingClassificationMapper classifications;

  public ChargeInstanceService(
      PxChargeInstanceMapper instances,
      PxBookkeepingRecordMapper records,
      PxBookkeepingAccountMapper accounts,
      PxBookkeepingClassificationMapper classifications) {
    this.instances = instances;
    this.records = records;
    this.accounts = accounts;
    this.classifications = classifications;
  }

  @Data
  public static class Action {
    private String requestId;
    private Long expectedRevision;
    private BigDecimal actualAmount;
    private Long accountId;
    private Long classificationId;
    private String paidAt;
    private String reason;
  }

  public static String mode(String mode, String fallback) {
    String value = mode == null ? fallback : mode;
    if (!Set.of("manual_confirm", "auto_record").contains(value))
      throw new ServiceException("INVALID_POSTING_MODE");
    return value;
  }

  public List<PxChargeInstance> list(String owner, int page, int size) {
    if (page < 1 || page > 10000 || size < 1 || size > 100) throw new ServiceException("分页参数无效");
    instances.reconcile(owner);
    return instances.list(owner, (page - 1) * size, size);
  }

  public List<PxChargeInstance> filtered(
      String owner, int page, int size, String type, Long source, String state) {
    if (page < 1 || page > 10000 || size < 1 || size > 100) throw new ServiceException("分页参数无效");
    if (type != null && type.isBlank()) type = null;
    if (state != null && state.isBlank()) state = null;
    if (type != null && !Set.of("subscription", "recurring").contains(type))
      throw new ServiceException("来源类型无效");
    if (state != null
        && !Set.of("pending", "payment_failed", "posted", "skipped", "cancelled", "voided")
            .contains(state)) throw new ServiceException("INVALID_STATE");
    instances.reconcile(owner);
    return instances.filtered(owner, type, source, state, (page - 1) * size, size);
  }

  @Transactional
  public Map<String, Object> detail(Long id, String owner) {
    instances.reconcile(owner);
    var row = instances.lock(id, owner);
    if (row == null) throw new ServiceException("SOURCE_UNAVAILABLE");
    return Map.of("instance", row, "history", instances.history(owner, id));
  }

  /** 调用者必须已锁定来源规则，事务覆盖实例创建与来源日期推进。 */
  @Transactional(rollbackFor = Exception.class)
  public PxChargeInstance generate(
      String owner,
      String type,
      Long source,
      String name,
      LocalDate due,
      BigDecimal amount,
      Long account,
      Long classification,
      Long otherAccount,
      String postingMode,
      String legacyKey) {
    PxChargeInstance row = instances.period(owner, type, source, due.toString());
    if (row != null) return row;
    row = new PxChargeInstance();
    row.setOwnerId(owner);
    row.setSourceType(type);
    row.setSourceId(source);
    row.setSourceName(name);
    row.setDueDate(due.toString());
    row.setExpectedAmount(amount);
    row.setAccountId(account);
    row.setClassificationId(classification);
    row.setOtherAccountId(otherAccount);
    row.setStatus("pending");
    row.setRevision(0L);
    instances.insert(row);
    PxBookkeepingRecord legacy = legacyKey == null ? null : records.selectByClientUuid(legacyKey);
    if (legacy != null) {
      if (!Objects.equals(owner, legacy.getCreateBy())) throw new ServiceException("账期记录归属冲突");
      row.setRecordId(legacy.getId());
      row.setActualAmount(legacy.getMoney());
      row.setStatus("posted");
      save(row);
    } else if ("auto_record".equals(mode(postingMode, "auto_record"))) {
      post(
          row,
          amount,
          account,
          classification,
          due,
          legacyKey == null ? "charge_" + row.getId() : legacyKey);
    }
    return row;
  }

  @Transactional(rollbackFor = Exception.class)
  public PxChargeInstance act(Long id, String owner, String operation, Action action) {
    if (action == null
        || action.getRequestId() == null
        || !action.getRequestId().matches("[A-Za-z0-9_-]{16,64}"))
      throw new ServiceException("REQUEST_ID_REQUIRED");
    if (!Set.of("confirm", "skip", "payment-failed", "cancel").contains(operation))
      throw new ServiceException("INVALID_ACTION");
    String hash =
        hash(
            id
                + "|"
                + operation
                + "|"
                + action.getActualAmount()
                + "|"
                + action.getAccountId()
                + "|"
                + action.getClassificationId()
                + "|"
                + action.getPaidAt()
                + "|"
                + action.getReason());
    PxChargeInstance row = instances.lock(id, owner);
    if (row == null) throw new ServiceException("SOURCE_UNAVAILABLE");
    var receipt = instances.receipt(owner, action.getRequestId());
    if (receipt != null) {
      if (!hash.equals(receipt.get("request_hash")))
        throw new ServiceException("IDEMPOTENCY_CONFLICT");
      return com.alibaba.fastjson.JSON.parseObject(
          (String) receipt.get("result_json"), PxChargeInstance.class);
    }
    if (action.getRequestId().equals(row.getRequestId())) {
      if (!hash.equals(row.getRequestHash())) throw new ServiceException("IDEMPOTENCY_CONFLICT");
      return row;
    }
    if (!Objects.equals(row.getRevision(), action.getExpectedRevision()))
      throw new ServiceException("REVISION_CONFLICT");
    if (!Set.of("pending", "payment_failed").contains(row.getStatus()))
      throw new ServiceException("INVALID_TRANSITION");
    row.setRequestId(action.getRequestId());
    row.setRequestHash(hash);
    if ("confirm".equals(operation)) {
      BigDecimal amount = action.getActualAmount();
      if (amount == null || amount.signum() < 0 || amount.scale() > 2 || amount.precision() > 18)
        throw new ServiceException("金额必须为非负两位小数");
      Long account = action.getAccountId(), classification = action.getClassificationId();
      PxBookkeepingAccount a =
          account == null ? null : accounts.selectPxBookkeepingAccountById(account);
      if (a == null || !owner.equals(a.getCreateBy())) throw new ServiceException("账户不存在或无权使用");
      if (!Long.valueOf(0).equals(row.getClassificationId())) {
        PxBookkeepingClassification c =
            classification == null
                ? null
                : classifications.selectPxBookkeepingClassificationById(classification);
        if (c == null || !owner.equals(c.getCreateBy())) throw new ServiceException("分类不存在或无权使用");
        if ("subscription".equals(row.getSourceType()) && !"1".equals(c.getTypeDifference()))
          throw new ServiceException("订阅必须选择支出分类");
      } else {
        classification = 0L;
        PxBookkeepingAccount other =
            accounts.selectPxBookkeepingAccountById(row.getOtherAccountId());
        if (other == null
            || !owner.equals(other.getCreateBy())
            || Objects.equals(account, other.getId())) throw new ServiceException("转入账户无效");
      }
      LocalDate paid;
      try {
        paid = LocalDate.parse(action.getPaidAt());
      } catch (Exception e) {
        throw new ServiceException("实际扣费日期无效");
      }
      if (paid.isAfter(LocalDate.now(SubscriptionSchedule.ZONE)))
        throw new ServiceException("实际扣费日期不能在未来");
      post(row, amount, account, classification, paid, "charge_" + id);
    } else {
      if (action.getReason() == null
          || action.getReason().isBlank()
          || action.getReason().length() > 500) throw new ServiceException("请填写不超过500字的原因");
      row.setReason(action.getReason());
      row.setStatus(
          switch (operation) {
            case "skip" -> "skipped";
            case "cancel" -> "cancelled";
            default -> "payment_failed";
          });
      save(row);
    }
    instances.receiptSave(
        owner, action.getRequestId(), hash, com.alibaba.fastjson.JSON.toJSONString(row));
    return row;
  }

  private void post(
      PxChargeInstance row,
      BigDecimal amount,
      Long account,
      Long classification,
      LocalDate paid,
      String key) {
    var ownedAccount = account == null ? null : accounts.selectPxBookkeepingAccountById(account);
    if (ownedAccount == null || !row.getOwnerId().equals(ownedAccount.getCreateBy()))
      throw new ServiceException("请为周期规则配置本人有效账户");
    if (Long.valueOf(0).equals(classification)) {
      var target = accounts.selectPxBookkeepingAccountById(row.getOtherAccountId());
      if (target == null
          || !row.getOwnerId().equals(target.getCreateBy())
          || Objects.equals(account, target.getId())) throw new ServiceException("转入账户无效");
    } else {
      var category =
          classification == null
              ? null
              : classifications.selectPxBookkeepingClassificationById(classification);
      if (category == null
          || !row.getOwnerId().equals(category.getCreateBy())
          || ("subscription".equals(row.getSourceType())
              && !"1".equals(category.getTypeDifference())))
        throw new ServiceException("请为周期规则配置本人有效分类");
    }
    PxBookkeepingRecord record = new PxBookkeepingRecord();
    record.setCreateBy(row.getOwnerId());
    record.setCreateTime(DateUtils.getNowDate());
    record.setAccount(account);
    record.setOtherAccount(row.getOtherAccountId());
    record.setType(classification);
    record.setMoney(amount);
    record.setPayTime(java.sql.Date.valueOf(paid));
    record.setDelFlag(false);
    record.setClientUuid(key);
    record.setVersion(key);
    record.setRemark("周期账单：" + row.getSourceName());
    if (records.insertPxBookkeepingRecord(record) != 1) throw new ServiceException("账单写入失败");
    row.setActualAmount(amount);
    row.setAccountId(account);
    row.setClassificationId(classification);
    row.setRecordId(record.getId());
    row.setStatus("posted");
    save(row);
  }

  private void save(PxChargeInstance row) {
    if (instances.update(row) != 1) throw new ServiceException("REVISION_CONFLICT");
    row.setRevision(row.getRevision() + 1);
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
