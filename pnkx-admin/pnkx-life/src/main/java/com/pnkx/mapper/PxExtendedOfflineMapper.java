package com.pnkx.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** Incremental reader for the extended offline modules; table names are server-side whitelisted. */
@Mapper
public interface PxExtendedOfflineMapper {
    List<Map<String, Object>> selectIncremental(@Param("table") String table,
                                                @Param("userId") String userId,
                                                @Param("since") String since,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);
}
