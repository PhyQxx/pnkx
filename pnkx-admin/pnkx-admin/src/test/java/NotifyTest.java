import com.pnkx.common.notify.FeishuISysNotify;
import org.junit.Test;

/**
 * NotifyTest
 * 手工冒烟脚本（会真实外呼飞书机器人），默认禁用；
 * 需要运行时设置 FEISHU_WEBHOOK 环境变量并临时移除 @Disabled。
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2024/4/30 17:21
 * @description 通知测试类
 */
public class NotifyTest {

    @Test
    @org.junit.Ignore("手工冒烟脚本：需 FEISHU_WEBHOOK 环境变量，勿在 CI 自动执行")
    public void notifyTest() {
        String webhook = System.getenv("FEISHU_WEBHOOK");
        if (webhook == null || webhook.isBlank()) {
            throw new IllegalStateException("请设置 FEISHU_WEBHOOK 环境变量");
        }
        FeishuISysNotify.sendNotification(webhook, "\uD83D\uDC49 叮咚！「Pei你看雪博客」您的待办提醒: 测试待办标题", "");
    }
}
