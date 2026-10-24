import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
const src = await readFile(
  new URL("../utils/actionCenterMutations.js", import.meta.url),
  "utf8"
);
const { createActionMutations } = await import(
  `data:text/javascript;base64,${Buffer.from(src).toString("base64")}`
);
const base = {
  refresh: async () => {},
  onBusy: () => {},
  onError: () => {},
  makeId: () => String(Math.random()),
};
test("clients use identical mutation guards", async () =>
  assert.equal(
    src,
    await readFile(
      new URL(
        "../../pnkx-ui/src/utils/actionCenterMutations.js",
        import.meta.url
      ),
      "utf8"
    )
  ));
test("simultaneous actions on same object are suppressed", async () => {
  let resolve,
    calls = 0;
  const runner = createActionMutations({
    ...base,
    request: () => {
      calls++;
      return new Promise((r) => (resolve = r));
    },
  });
  const task = runner.run("a", "read", () => ({}));
  assert.equal(await runner.run("a", "dismiss", () => ({})), false);
  assert.equal(calls, 1);
  resolve();
  assert.equal(await task, true);
});
test("failed retry keeps original id and snooze timestamp", async () => {
  const sent = [];
  let builds = 0;
  const runner = createActionMutations({
    ...base,
    request: async (p) => {
      sent.push(p);
      if (sent.length === 1) throw Error("offline");
    },
  });
  const build = ({ id, now }) => {
    builds++;
    return { id, until: now + 3600000 };
  };
  assert.equal(await runner.run("r", "snooze:1", build), false);
  assert.equal(await runner.run("r", "snooze:1", build), true);
  assert.equal(builds, 1);
  assert.deepEqual(sent[0], sent[1]);
  await runner.run("r", "snooze:2", build);
  assert.equal(builds, 2);
});
test("disposed operation does not refresh or report on unmounted view", async () => {
  let resolve,
    refreshes = 0;
  const runner = createActionMutations({
    ...base,
    request: () => new Promise((r) => (resolve = r)),
    refresh: () => refreshes++,
  });
  const task = runner.run("a", "x", () => ({}));
  runner.dispose();
  resolve();
  await task;
  assert.equal(refreshes, 0);
  assert.equal(await runner.run("a", "y", () => ({})), false);
});
test("old space failure cannot show an error in the new space", async () => {
  let reject;
  const errors = [];
  const runner = createActionMutations({
    ...base,
    request: () => new Promise((_, r) => (reject = r)),
    onError: (e) => errors.push(e),
  });
  const task = runner.run("a", "read", () => ({}));
  runner.contextChanged();
  reject(Error("offline"));
  await task;
  assert.deepEqual(errors, [""]);
});
