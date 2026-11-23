package com.pnkx.web.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 工具使用打点 Mapper（注解 SQL，表结构见 V1.9.1__tool_use_log.sql）
 *
 * @author PHY
 */
@Mapper
public interface SysToolUseLogMapper {

    /**
     * 记录一次工具使用
     *
     * @param toolId   工具标识（sys.tools.list 的 id）
     * @param toolName 工具名称（冗余，便于统计直读）
     * @param ip       来源 IP
     * @param source   客户端来源（mp=微信小程序 / h5 / app）
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_tool_use_log (tool_id, tool_name, ip, source, create_time) "
            + "VALUES (#{toolId}, #{toolName}, #{ip}, #{source}, NOW())")
    int insertUseLog(@Param("toolId") String toolId,
                     @Param("toolName") String toolName,
                     @Param("ip") String ip,
                     @Param("source") String source);

    /**
     * 按工具聚合使用统计
     *
     * @return [{toolId, toolName, useCount, lastUsedTime}]
     */
    @Select("SELECT tool_id AS toolId, MAX(tool_name) AS toolName, COUNT(1) AS useCount, "
            + "MAX(create_time) AS lastUsedTime "
            + "FROM sys_tool_use_log "
            + "GROUP BY tool_id "
            + "ORDER BY useCount DESC, MAX(create_time) DESC")
    List<Map<String, Object>> selectUseStats();

    /**
     * 按工具聚合使用统计（带时间范围筛选，管理端统计视图用）
     *
     * @param beginTime 开始日期（含，格式 yyyy-MM-dd）
     * @param endTime   结束日期（含）
     * @return [{toolId, toolName, useCount, lastUsedTime}]
     */
    @Select("<script>SELECT tool_id AS toolId, MAX(tool_name) AS toolName, COUNT(1) AS useCount, "
            + "MAX(create_time) AS lastUsedTime "
            + "FROM sys_tool_use_log "
            + "<where>"
            + "<if test='beginTime != null and beginTime != \"\"'> AND create_time &gt;= #{beginTime}</if>"
            + "<if test='endTime != null and endTime != \"\"'> AND create_time &lt;= CONCAT(#{endTime}, ' 23:59:59')</if>"
            + "</where> "
            + "GROUP BY tool_id "
            + "ORDER BY useCount DESC, MAX(create_time) DESC</script>")
    List<Map<String, Object>> selectUseStatsFiltered(@Param("beginTime") String beginTime,
                                                     @Param("endTime") String endTime);

    /**
     * 使用明细（管理端分页，startPage() 拦截；倒序）
     *
     * @param toolId    工具标识（可选）
     * @param beginTime 开始日期（可选）
     * @param endTime   结束日期（可选）
     * @return [{id, toolId, toolName, ip, source, createTime}]
     */
    @Select("<script>SELECT id, tool_id AS toolId, tool_name AS toolName, ip, source, create_time AS createTime "
            + "FROM sys_tool_use_log "
            + "<where>"
            + "<if test='toolId != null and toolId != \"\"'> AND tool_id = #{toolId}</if>"
            + "<if test='beginTime != null and beginTime != \"\"'> AND create_time &gt;= #{beginTime}</if>"
            + "<if test='endTime != null and endTime != \"\"'> AND create_time &lt;= CONCAT(#{endTime}, ' 23:59:59')</if>"
            + "</where> "
            + "ORDER BY id DESC</script>")
    List<Map<String, Object>> selectUseLogList(@Param("toolId") String toolId,
                                               @Param("beginTime") String beginTime,
                                               @Param("endTime") String endTime);

    /**
     * 指定工具的使用次数（排序展示用）
     *
     * @param toolId 工具标识
     * @return 次数
     */
    @Select("SELECT COUNT(1) FROM sys_tool_use_log WHERE tool_id = #{toolId}")
    long countByToolId(@Param("toolId") String toolId);
}
