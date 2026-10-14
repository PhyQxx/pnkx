package com.pnkx.life.mapper;
import com.pnkx.life.service.workspace.UserFile;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface UserFileMapper {
 UserFile get(@Param("owner") String owner,@Param("id") Long id);
 List<UserFile> list(@Param("owner") String owner,@Param("parent") Long parent,@Param("after") long after);
 List<UserFile> folders(@Param("owner") String owner);
 long used(@Param("owner") String owner);
 int nameCount(@Param("owner") String owner,@Param("parent") Long parent,@Param("name") String name,@Param("exclude") long exclude);
 int children(@Param("owner") String owner,@Param("parent") Long parent);
 void insert(UserFile row);
 int update(UserFile row);
}
