import request from "@/utils/request";
import {
    CHANGE_PREVIEW_URL,
    BACKFILL_PREVIEW_URL,
    CHANGE_COMMIT_URL,
    formatValue,
    fieldLabels,
    presentRuleFlow,
} from "@/utils/ruleChangeFlow";

function fallbackText(preview, kind) {
  if (kind === "backfill") {
    return (
      preview.periods.join("、") + "；每期预计 " + preview.amountPerPeriod + " 元。" + preview.notice
    );
  }
  return (
    (preview.changes || [])
      .map(
        (c) =>
          `${fieldLabels[c.field] || c.field}：${formatValue(c.before, c.field)} → ${formatValue(
            c.after,
            c.field
          )}`
      )
      .join("；") + "." + preview.notice
  );
}

function confirmDialog(title, content) {
  return new Promise((resolve) =>
    uni.showModal({
      title,
      content,
      success: (r) => resolve(!!r.confirm),
      fail: () => resolve(false),
    })
  );
}

async function runTask(task, title) {
  // 展示器(三步流程组件)可用时交给组件;否则降级为单条确认
  const presented = await presentRuleFlow(task);
  if (presented && presented.status === "committed") return presented.result;
  if (presented && presented.status === "cancelled") throw new Error("已取消变更");
  const ok = await confirmDialog(title, fallbackText(task.preview, task.kind));
  if (!ok) throw new Error("已取消变更");
  return request({
    url: CHANGE_COMMIT_URL,
    method: "post",
    data: { previewId: task.preview.previewId },
  });
}

export async function changeRule(sourceType, changes) {
  const input = { sourceType, sourceId: changes.id, changes };
  const r = await request({ url: CHANGE_PREVIEW_URL, method: "post", data: input });
  return runTask(
    { kind: "change", sourceType, sourceId: changes.id, input, preview: r.data },
    "确认规则变更"
  );
}

export async function backfillRule(sourceType, sourceId) {
  const input = { sourceType, sourceId };
  const r = await request({ url: BACKFILL_PREVIEW_URL, method: "post", data: input });
  return runTask({ kind: "backfill", sourceType, sourceId, input, preview: r.data }, "确认补齐账期");
}
