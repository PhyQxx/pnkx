package com.pnkx.life.mapper;
import com.pnkx.life.service.workspace.OrganizeSuggestion;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface OrganizeMapper {
    void insert(OrganizeSuggestion suggestion);
    List<OrganizeSuggestion> list(@Param("owner") String owner,@Param("after") long after);
    OrganizeSuggestion lock(@Param("owner") String owner,@Param("id") Long id);
    void status(@Param("owner") String owner,@Param("id") Long id,@Param("status") String status);
}
