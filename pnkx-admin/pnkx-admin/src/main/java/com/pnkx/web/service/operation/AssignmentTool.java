package com.pnkx.web.service.operation;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.service.impl.AssignmentService;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class AssignmentTool {
    private final AssignmentService assignments;
    public AssignmentTool(AssignmentService assignments) { this.assignments = assignments; }
    private void target(JSONObject input) {
        String type = input.getString("sourceType");
        if (type == null || !Set.of("todo", "shopping").contains(type)
                || input.getLong("sourceId") == null || input.getLong("sourceId") <= 0)
            throw new ServiceException("请提供事项类型（待办或购物商品）及明确编号");
    }
    public JSONObject detail(JSONObject input, String actor) {
        target(input);
        Integer page = input.getInteger("page");
        return assignments.detail(input.getString("sourceType"), input.getLong("sourceId"), actor, page == null ? 1 : page);
    }
    public void prepare(JSONObject draft, String actor) {
        var current = detail(draft, actor);
        if (!current.getBooleanValue("canTransfer")) throw new ServiceException("只有所有者或共享空间管理员可以转派");
        draft.put("expectedAssignee", current.getString("assignee") == null ? "" : current.getString("assignee"));
        validate(draft, actor, false);
    }
    public void validate(JSONObject draft, String actor, boolean lock) {
        target(draft);
        assignments.validateTransfer(draft.getString("sourceType"), draft.getLong("sourceId"), actor, transferInput(draft), lock);
    }
    private JSONObject transferInput(JSONObject draft) {
        // Keep an explicit empty marker through JSON persistence, then restore null for the domain service.
        JSONObject input = new JSONObject(new java.util.HashMap<>(draft));
        if ("".equals(input.getString("expectedAssignee"))) input.put("expectedAssignee", null);
        return input;
    }
    public JSONObject run(JSONObject draft, String actor) {
        target(draft);
        var detail = assignments.transfer(draft.getString("sourceType"), draft.getLong("sourceId"), actor, transferInput(draft));
        return new JSONObject().fluentPut("sourceType", "assignment_mutation")
                .fluentPut("sourceId", draft.getLong("sourceId")).fluentPut("entityType", draft.getString("sourceType"))
                .fluentPut("assignee", detail.getString("assignee"));
    }
}
