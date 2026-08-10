package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * AI意图处理器接口（策略模式）
 * 新增意图只需实现此接口并加 @Component，Spring 自动注册，零改动现有代码。
 *
 * @author PHY
 */
public interface IntentHandler {

    /**
     * 意图名称，用于匹配AI返回的 intent 字段
     * 例如 "bookkeeping"、"analysis"、"chat"
     */
    String intentName();

    /**
     * 提供给AI意图分类器的描述行
     * 例如："- {\"intent\":\"bookkeeping\"} — 用户想记一笔账"
     */
    String promptDescription();

    /**
     * 处理该意图
     *
     * @param question   用户原始输入
     * @param intentData AI返回的完整意图JSON（可从中提取额外参数）
     * @param out        SSE输出流
     * @return true=已处理完毕，false=降级到普通对话
     */
    boolean handle(String question, JSONObject intentData, OutputStream out) throws IOException;

    /**
     * 写SSE数据（符合SSE标准：多行文本每行加 data: 前缀）
     */
    static void writeSse(OutputStream out, String data) throws IOException {
        if (data == null) return;
        String[] lines = data.split("\\r?\\n");
        for (String line : lines) {
            out.write(("data:" + line + "\n").getBytes(StandardCharsets.UTF_8));
        }
        out.write("\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
