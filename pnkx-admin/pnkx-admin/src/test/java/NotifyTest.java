import com.pnkx.common.notify.FeishuISysNotify;
import org.junit.Test;

/**
 * NotifyTest
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2024/4/30 17:21
 * @description 通知测试类
 */
public class NotifyTest {

    @Test
    public void notifyTest() {
        FeishuISysNotify.sendNotification("***REMOVED-FEISHU-WEBHOOK***", "\uD83D\uDC49 叮咚！「Pei你看雪博客」您的待办提醒: 测试待办标题", "");
    }
}
