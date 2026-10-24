package com.pnkx.life.mapper;

import com.pnkx.life.domain.po.PxChargeInstance;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PxChargeInstanceMapper {
  @Select(
      "select request_hash,result_json from px_action_receipt where owner_id=#{owner} and"
          + " request_id=#{request}")
  java.util.Map<String, Object> receipt(
      @Param("owner") String owner, @Param("request") String request);

  @Insert(
      "insert into px_action_receipt(owner_id,request_id,request_hash,result_json)"
          + " values(#{owner},#{request},#{hash},#{result})")
  int receiptSave(
      @Param("owner") String owner,
      @Param("request") String request,
      @Param("hash") String hash,
      @Param("result") String result);

  @Update(
      "update px_charge_instance i join px_bookkeeping_record r on r.id=i.record_id set"
          + " i.actual_amount=r.money,i.account_id=r.account,i.classification_id=r.type,i.status=if(r.del_flag=1,'voided','posted'),i.revision=i.revision+1,i.update_time=now()"
          + " where i.owner_id=#{owner} and (not(i.actual_amount <=> r.money) or not(i.account_id"
          + " <=> r.account) or not(i.classification_id <=> r.type) or"
          + " i.status<>if(r.del_flag=1,'voided','posted'))")
  int reconcile(String owner);

  String COLUMNS =
      "id, owner_id ownerId, source_type sourceType, source_id sourceId, source_name sourceName,"
          + " due_date dueDate, expected_amount expectedAmount, actual_amount actualAmount,"
          + " account_id accountId, classification_id classificationId, other_account_id"
          + " otherAccountId, status, record_id recordId, revision, request_id requestId,"
          + " request_hash requestHash, reason";

  @Select(
      "select "
          + COLUMNS
          + " from px_charge_instance where owner_id=#{owner} order by due_date desc,id desc limit"
          + " #{limit} offset #{offset}")
  List<PxChargeInstance> list(
      @Param("owner") String owner, @Param("offset") int offset, @Param("limit") int limit);

  @Select(
      "select "
          + COLUMNS
          + " from px_charge_instance where owner_id=#{owner} and (#{type} is null or"
          + " source_type=#{type}) and (#{source} is null or source_id=#{source}) and (#{state} is"
          + " null or status=#{state}) order by due_date desc,id desc limit #{limit} offset"
          + " #{offset}")
  List<PxChargeInstance> filtered(
      @Param("owner") String owner,
      @Param("type") String type,
      @Param("source") Long source,
      @Param("state") String state,
      @Param("offset") int offset,
      @Param("limit") int limit);

  @Select(
      "select request_id,create_time,result_json from px_action_receipt where owner_id=#{owner} and"
          + " JSON_UNQUOTE(JSON_EXTRACT(result_json,'$.id'))=cast(#{id} as char) and"
          + " JSON_UNQUOTE(JSON_EXTRACT(result_json,'$.sourceType')) in"
          + " ('subscription','recurring') order by create_time")
  List<java.util.Map<String, Object>> history(@Param("owner") String owner, @Param("id") Long id);

  @Select(
      "select "
          + COLUMNS
          + " from px_charge_instance where id=#{id} and owner_id=#{owner} for update")
  PxChargeInstance lock(@Param("id") Long id, @Param("owner") String owner);

  @Select(
      "select "
          + COLUMNS
          + " from px_charge_instance where owner_id=#{owner} and source_type=#{type} and"
          + " source_id=#{source} and due_date=#{due}")
  PxChargeInstance period(
      @Param("owner") String owner,
      @Param("type") String type,
      @Param("source") Long source,
      @Param("due") String due);

  @Insert(
      "insert into"
          + " px_charge_instance(owner_id,source_type,source_id,source_name,due_date,expected_amount,account_id,classification_id,other_account_id,status,revision)"
          + " values(#{ownerId},#{sourceType},#{sourceId},#{sourceName},#{dueDate},#{expectedAmount},#{accountId},#{classificationId},#{otherAccountId},'pending',0)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(PxChargeInstance row);

  @Update(
      "update px_charge_instance set"
          + " actual_amount=#{actualAmount},account_id=#{accountId},classification_id=#{classificationId},status=#{status},record_id=#{recordId},revision=revision+1,request_id=#{requestId},request_hash=#{requestHash},reason=#{reason},update_time=now()"
          + " where id=#{id} and owner_id=#{ownerId} and revision=#{revision}")
  int update(PxChargeInstance row);
}
