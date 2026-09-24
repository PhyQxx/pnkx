package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Spring 驱动的 AI 工具注册表，统一意图发现、调用和能力展示。 */
@Component
public class AiToolRegistry {
    private final List<IntentHandler> handlers;
    private final Map<String, IntentHandler> handlerMap;

    public AiToolRegistry(List<IntentHandler> handlers) {
        this.handlers = List.copyOf(handlers);
        this.handlerMap = Collections.unmodifiableMap(handlers.stream().collect(Collectors.toMap(
                IntentHandler::intentName, Function.identity(), (left, right) -> left, LinkedHashMap::new)));
    }

    public List<IntentHandler> handlers() { return handlers; }

    public IntentHandler get(String name) { return handlerMap.get(name); }

    public JSONArray descriptors() {
        JSONArray result = new JSONArray();
        handlers.forEach(handler -> {
            JSONObject item = new JSONObject();
            item.put("name", handler.intentName());
            item.put("description", handler.promptDescription());
            item.put("requiresConfirmation", handler instanceof ConfirmableIntentHandler);
            result.add(item);
        });
        return result;
    }
}
