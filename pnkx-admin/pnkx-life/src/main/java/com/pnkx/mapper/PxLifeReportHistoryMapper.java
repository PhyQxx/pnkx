package com.pnkx.mapper;

import com.pnkx.domain.po.PxLifeReportHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PxLifeReportHistoryMapper {
    @Insert("INSERT INTO px_life_report_history(user_id, period, report_type, source, content, create_time) "
            + "VALUES(#{userId}, #{period}, #{reportType}, #{source}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PxLifeReportHistory history);

    @Select("SELECT id, user_id userId, period, report_type reportType, source, content, create_time createTime "
            + "FROM px_life_report_history WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<PxLifeReportHistory> selectRecent(@Param("userId") String userId, @Param("limit") int limit);
}
