package com.pnkx.quartz.task;
import com.pnkx.life.service.recurrence.TodoSeriesService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
@Component("todoSeriesTask")
public class TodoSeriesTask {
 @Resource private TodoSeriesService service;
 public void generate() { long after=0;while(true){var ids=service.active(after);for(Long id:ids){try{service.generate(id);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(TodoSeriesTask.class).error("Series generation failed: {}",id,ex);}}if(ids.size()<100)return;after=ids.getLast();} }
}
