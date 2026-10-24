package com.pnkx.web.service.intent;

import com.alibaba.fastjson.*;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.web.service.operation.OperationPlanService;
import java.io.*;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LocalIntentPlanService {
  private final OperationPlanService plans;

  public LocalIntentPlanService(OperationPlanService plans) {
    this.plans = plans;
  }

  public void prepare(JSONObject draft, String code, String title) {
    var step = new JSONObject();
    step.put("toolCode", code);
    step.put("input", JSON.parseObject(draft.toJSONString()));
    var steps = new JSONArray();
    steps.add(step);
    var input = new JSONObject();
    input.put("title", title);
    input.put("steps", steps);
    draft.put("operationPlanId", plans.preview(input, SecurityUtils.getUserId()).getString("id"));
  }

  public boolean confirm(JSONObject draft, OutputStream out) throws IOException {
    String id = draft.getString("operationPlanId");
    if (id == null) throw new ServiceException("旧草稿请重新生成预览后确认");
    var result = plans.execute(id, SecurityUtils.getUserId());
    draft.put("_result", result);
    IntentHandler.writeSse(
        out,
        "执行结果："
            + Map.of(
                    "succeeded",
                    "已完成",
                    "partially_succeeded",
                    "部分完成",
                    "failed",
                    "执行失败",
                    "running",
                    "正在执行",
                    "cancelled",
                    "已取消")
                .getOrDefault(result.getString("status"), result.getString("status"))
            + "。计划编号："
            + id
            + "，可在操作计划查看结果、重试或预览撤销。");
    IntentHandler.writeSse(out, "[DONE]");
    return true;
  }
}
