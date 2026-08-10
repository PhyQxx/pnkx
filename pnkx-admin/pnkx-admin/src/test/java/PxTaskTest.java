import com.pnkx.PnkxApplication;
import com.pnkx.quartz.task.PxTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;

/**
 * 待办提醒测试类
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PnkxApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PxTaskTest {

    @Resource
    private PxTask pxTask;

    @Test
    public void testToDoReminder() {
        System.out.println("开始测试待办提醒...");
        pxTask.toDoReminder();
        System.out.println("待办提醒测试结束...");
    }
}
