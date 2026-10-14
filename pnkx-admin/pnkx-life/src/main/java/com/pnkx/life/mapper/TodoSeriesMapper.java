package com.pnkx.life.mapper;
import com.pnkx.life.service.recurrence.TodoSeries;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface TodoSeriesMapper {
 List<TodoSeries> list(@Param("owner") String owner);
 List<Long> active(@Param("after") long after);
 TodoSeries lock(@Param("id") Long id);
 void insert(TodoSeries series);
 int update(TodoSeries series);
 void progress(TodoSeries series);
 void occurrence(@Param("series") Long series,@Param("date") String date,@Param("todo") Long todo);
 Long occurrenceId(@Param("series") Long series,@Param("date") String date);
 List<Long> unfinished(@Param("series") Long series,@Param("owner") String owner);
}
