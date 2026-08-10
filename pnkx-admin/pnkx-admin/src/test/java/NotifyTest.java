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
        FeishuISysNotify.sendNotification("https://open.feishu.cn/open-apis/bot/v2/hook/ae03114d-aa0d-4348-b12e-9bd7e2911399", "\uD83D\uDC49 叮咚！「Pei你看雪博客」您的待办提醒: 测试待办标题", "");
    }
}
