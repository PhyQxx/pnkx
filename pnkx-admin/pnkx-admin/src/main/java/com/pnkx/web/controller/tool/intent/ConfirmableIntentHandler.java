package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

public interface ConfirmableIntentHandler extends IntentHandler {

    boolean confirm(JSONObject draft, OutputStream out) throws IOException;
}
