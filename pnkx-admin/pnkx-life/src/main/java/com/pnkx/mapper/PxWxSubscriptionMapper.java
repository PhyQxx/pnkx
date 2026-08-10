package com.pnkx.mapper;

import com.pnkx.domain.po.PxWxSubscription;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PxWxSubscriptionMapper {
    @Insert("INSERT INTO px_wx_subscription(user_id, template_type, accepted, create_time, update_time) "
            + "VALUES(#{userId}, #{templateType}, #{accepted}, NOW(), NOW()) ON DUPLICATE KEY UPDATE accepted=VALUES(accepted), update_time=NOW()")
    int upsert(PxWxSubscription subscription);

    @Select("SELECT id, user_id userId, template_type templateType, accepted, update_time updateTime "
            + "FROM px_wx_subscription WHERE user_id=#{userId}")
    List<PxWxSubscription> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT id, user_id userId, template_type templateType, accepted, update_time updateTime "
            + "FROM px_wx_subscription WHERE accepted=1")
    List<PxWxSubscription> selectEnabled();

    @Update("UPDATE px_wx_subscription SET accepted=0, update_time=NOW() WHERE id=#{id}")
    int consume(@Param("id") Long id);

    @Insert("INSERT INTO px_wx_message_log(user_id, template_type, success, response_message, send_time) "
            + "VALUES(#{userId}, #{templateType}, #{success}, #{message}, NOW())")
    int insertLog(@Param("userId") Long userId, @Param("templateType") String templateType,
                  @Param("success") boolean success, @Param("message") String message);
}
