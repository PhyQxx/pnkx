package com.pnkx.web.service.operation;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxBookkeepingRecord;
import com.pnkx.life.service.IPxBookkeepingRecordService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * AI 记账：解析草稿规范化后进入持久化计划，金额、账户、分类在预览和执行时分别校验；
 * 生成结果属于财务记录，不提供自动撤销。
 */
@Service
public class BookkeepingCreateTool {
  private final JdbcTemplate db;
  private final IPxBookkeepingRecordService records;

  public BookkeepingCreateTool(JdbcTemplate db, IPxBookkeepingRecordService records) {
    this.db = db;
    this.records = records;
  }

  /** 把解析结果收敛为计划输入：剔除装配对象，保留编号与展示名，金额立即校验。 */
  public void prepare(JSONObject args, String owner) {
    JSONObject input = new JSONObject(true);
    input.put("money", args.getString("money"));
    input.put("type", args.getString("type"));
    input.put("typeId", args.getLong("typeId"));
    input.put("accountId", args.getLong("accountId"));
    String remark = args.getString("remark");
    if (remark != null && remark.length() > 255) remark = remark.substring(0, 255);
    input.put("remark", remark);
    input.put("payTime", args.getString("payTime"));
    if (args.getString("secondaryType") != null) input.put("typeName", args.getString("secondaryType"));
    if (args.getString("primaryType") != null) input.put("primaryTypeName", args.getString("primaryType"));
    if (args.getString("account") != null) input.put("accountName", args.getString("account"));
    money(input);
    args.clear();
    args.putAll(input);
  }

  private BigDecimal money(JSONObject a) {
    try {
      BigDecimal value =
          new BigDecimal(String.valueOf(a.get("money")).trim()).setScale(2, RoundingMode.HALF_UP);
      if (value.signum() <= 0) throw new ServiceException("金额必须大于零");
      return value;
    } catch (NumberFormatException | NullPointerException e) {
      throw new ServiceException("AI 返回的金额无法解析：" + a.get("money"));
    }
  }

  private Map<String, Object> owned(String table, Long id, String owner, boolean lock) {
    if (id == null) throw new ServiceException("请选择有效的" + ("px_bookkeeping_account".equals(table) ? "账户" : "分类"));
    var rows =
        db.queryForList(
            "select * from " + table + " where id=? and create_by=? and del_flag=0"
                + (lock ? " for update" : ""),
            id,
            owner);
    if (rows.isEmpty())
      throw new ServiceException(
          ("px_bookkeeping_account".equals(table) ? "账户" : "分类") + "不存在或无权使用，请重新生成草稿");
    return rows.get(0);
  }

  public void validate(JSONObject a, String owner, boolean lock) {
    money(a);
    String type = a.getString("type");
    if (!"0".equals(type) && !"1".equals(type)) throw new ServiceException("收支类型无效");
    Long typeId = a.getLong("typeId");
    if (typeId == null || typeId == 0L) throw new ServiceException("请选择有效的记账分类");
    owned("px_bookkeeping_classification", typeId, owner, lock);
    owned("px_bookkeeping_account", a.getLong("accountId"), owner, lock);
    String remark = a.getString("remark");
    if (remark != null && remark.length() > 255) throw new ServiceException("备注不能超过255字");
    String payTime = a.getString("payTime");
    if (payTime == null || payTime.isBlank()) throw new ServiceException("缺少支付时间");
    LocalDateTime time;
    try {
      time =
          LocalDateTime.parse(
              payTime,
              DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                  .withResolverStyle(ResolverStyle.STRICT));
    } catch (Exception e) {
      throw new ServiceException("支付时间格式无效");
    }
    if (time.isAfter(LocalDateTime.now().plusMinutes(1)))
      throw new ServiceException("支付时间不能晚于当前时间");
  }

  public JSONObject run(JSONObject a, String key, String owner) {
    var record = new PxBookkeepingRecord();
    record.setAccount(a.getLong("accountId"));
    record.setType(a.getLong("typeId"));
    record.setMoney(money(a));
    record.setRemark(a.getString("remark"));
    record.setPayTime(
        java.sql.Timestamp.valueOf(
            LocalDateTime.parse(
                a.getString("payTime"),
                DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                    .withResolverStyle(ResolverStyle.STRICT))));
    record.setDelFlag(false);
    record.setClientUuid("plan:" + key);
    if (records.insertPxBookkeepingRecord(record) != 1)
      throw new ServiceException("记账保存失败");
    if (record.getId() == null) throw new ServiceException("记账保存未返回记录编号");
    var result = new JSONObject();
    result.put("sourceType", "bookkeeping");
    result.put("sourceId", record.getId());
    result.put("money", record.getMoney());
    result.put("affected", 1);
    return result;
  }
}
