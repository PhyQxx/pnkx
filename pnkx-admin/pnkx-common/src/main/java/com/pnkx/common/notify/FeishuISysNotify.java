package com.pnkx.common.notify;

import com.pnkx.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * FeishuISysNotify
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2024/4/30 17:04
 * @description 飞书通知
 */
public class FeishuISysNotify {

    protected static final Logger logger = LoggerFactory.getLogger(FeishuISysNotify.class);

    public static void sendNotification(String webhookUrl, String message, String link) {
        if (StringUtils.isEmpty(webhookUrl)) {
            logger.warn("飞书webhook地址为空，无法发送通知。");
            return;
        }
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = getHttpURLConnection(message, link, url);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                logger.info("飞书通知成功");
            } else {
                logger.error("飞书通知失败，响应码：{}", responseCode);
            }

        } catch (Exception e) {
            logger.error("发送飞书通知时发生错误：{}", e.getMessage());
        }
    }

    /**
     * 获取HttpURLConnection
     *
     * @param message 通知内容
     * @param link    超链接地址
     * @param url     webhook地址
     * @return HttpURLConnection
     * @throws IOException 异常
     */
    private static HttpURLConnection getHttpURLConnection(String message, String link, URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);

        StringBuilder jsonInputString = new StringBuilder();
        jsonInputString.append("{\n" +
                "  \"msg_type\": \"interactive\",\n" +
                "  \"card\": {\n" +
                "    \"elements\": [\n" +
                "      {\n" +
                "        \"tag\": \"div\",\n" +
                "        \"text\": {\n" +
                "          \"content\": \"" + message + "\",\n" +
                "          \"tag\": \"lark_md\"\n" +
                "        }\n" +
                "      }\n");
        if (StringUtils.isNotEmpty(link)) {
            jsonInputString.append(",      {\n" +
                    "        \"actions\": [\n" +
                    "          {\n" +
                    "            \"tag\": \"button\",\n" +
                    "            \"text\": {\n" +
                    "              \"content\": \"详情查看\",\n" +
                    "              \"tag\": \"lark_md\"\n" +
                    "            },\n" +
                    "            \"url\": \"https://admin.pnkx.top:8" + link + "\",\n" +
                    "            \"type\": \"default\",\n" +
                    "            \"value\": {\n" +
                    "              \n" +
                    "            }\n" +
                    "          }\n" +
                    "        ],\n" +
                    "        \"tag\": \"action\"\n" +
                    "      }\n");
        }
        jsonInputString.append("    ],\n" +
                "    \"header\": {\n" +
                "      \"title\": {\n" +
                "        \"content\": \"【Pei你看雪博客】系统通知\",\n" +
                "        \"tag\": \"plain_text\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
        logger.info("飞书通知内容：{}", jsonInputString);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }
}
