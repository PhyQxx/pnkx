package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.life.domain.po.PxWebArticle;
import com.pnkx.life.service.IPxWebArticleService;
import com.pnkx.life.service.webarticle.WebArticleUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 网址剪藏意图处理器
 * <p>
 * 用户在 AI 对话中发送网页链接（或明确表达收藏网址的意愿）时，
 * 自动收藏到「网址收藏」并触发后台抓取与 AI 整理。
 * 剪藏按链接去重、幂等且不产生破坏性影响，故无需二次确认。
 *
 * @author PHY
 */
@Component
public class WebClipHandler implements IntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebClipHandler.class);

    /**
     * 判定为「刚收藏过」的时间窗口（毫秒），用于区分新建与重复收藏的提示
     */
    private static final long JUST_CLIPPED_MS = 5 * 60 * 1000L;

    @Resource
    private IPxWebArticleService webArticleService;

    @Override
    public String intentName() {
        return "web_clip";
    }

    @Override
    public String promptDescription() {
        return "用户明确想收藏/剪藏网页，或仅发送网页链接。明确要求生成或导入菜谱时必须选择 recipe。slots: {\"url\": \"用户提到的网址\"}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) throws IOException {
        String url = intentData.getString("url");
        if (url == null || url.isBlank()) {
            url = WebArticleUtils.extractUrl(question);
        }
        if (url == null) {
            return false;
        }

        PxWebArticle article = webArticleService.addArticleByUrl(url, null, "2");
        boolean justClipped = article.getCreateTime() != null
                && System.currentTimeMillis() - article.getCreateTime().getTime() < JUST_CLIPPED_MS;
        StringBuilder msg = new StringBuilder();
        msg.append("**已收藏网页**\n\n");
        msg.append("- 链接：").append(article.getUrl()).append("\n");
        if (justClipped) {
            msg.append("\n正文抓取与 AI 摘要正在后台进行，稍后可在「我的应用 → 网址收藏」查看整理结果。");
        } else {
            msg.append("\n这条链接之前已经收藏过了，可直接在「我的应用 → 网址收藏」中查看。");
        }
        IntentHandler.writeSse(out, msg.toString());
        IntentHandler.writeSse(out, "[DONE]");
        logger.info("AI对话剪藏成功, url: {}, id: {}", article.getUrl(), article.getId());
        return true;
    }
}
