import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
const source = await readFile(
  new URL("../utils/actionCenterState.js", import.meta.url),
  "utf8"
);
const { createLatestLoader, parsePinned } = await import(
  `data:text/javascript;base64,${Buffer.from(source).toString("base64")}`
);
const deferred = () => {
  let resolve, reject;
  const promise = new Promise((a, b) => {
    resolve = a;
    reject = b;
  });
  return { promise, resolve, reject };
};
function setup() {
  const pending = [],
    state = { data: null, error: null, loading: false };
  const loader = createLatestLoader({
    fetch: (input) => {
      const d = deferred();
      pending.push({ ...d, input });
      return d.promise;
    },
    onStart: () => {
      state.data = null;
      state.error = null;
      state.loading = true;
    },
    onData: (data) => (state.data = data),
    onError: (e) => (state.error = e.message),
    onSettled: () => (state.loading = false),
  });
  return { pending, state, loader };
}
test("both clients share concurrency and preference rules", async () =>
  assert.equal(
    source,
    await readFile(
      new URL("../../pnkx-ui/src/utils/actionCenterState.js", import.meta.url),
      "utf8"
    )
  ));
test("late private response cannot overwrite a newer group", async () => {
  const { loader, pending, state } = setup();
  const a = loader.run("private"),
    b = loader.run("group");
  pending[1].resolve("group data");
  await b;
  pending[0].resolve("private data");
  await a;
  assert.equal(state.data, "group data");
  assert.equal(state.loading, false);
});
test("old failure cannot clear loading or error of new request", async () => {
  const { loader, pending, state } = setup();
  const a = loader.run(1),
    b = loader.run(2);
  pending[0].reject(new Error("old failure"));
  await a;
  assert.equal(state.error, null);
  assert.equal(state.loading, true);
  pending[1].reject(new Error("new failure"));
  await b;
  assert.equal(state.error, "new failure");
  assert.equal(state.loading, false);
});
test("unmount invalidates outstanding response", async () => {
  const { loader, pending, state } = setup();
  const task = loader.run(1);
  loader.invalidate();
  pending[0].resolve("stale");
  await task;
  assert.equal(state.data, null);
});
test("refresh clears stale data before request and can recover after failure", async () => {
  const { loader, pending, state } = setup();
  const a = loader.run(1);
  pending[0].resolve("old");
  await a;
  const b = loader.run(2);
  assert.equal(state.data, null);
  pending[1].reject(new Error("offline"));
  await b;
  const c = loader.run(3);
  pending[2].resolve("new");
  await c;
  assert.equal(state.data, "new");
  assert.equal(state.error, null);
});
test("malformed preferences cannot break center or introduce unknown links", () => {
  const allowed = { todo: "待办", meal: "餐饮" };
  assert.deepEqual(parsePinned("bad", allowed), []);
  assert.deepEqual(parsePinned('{"todo":true}', allowed), []);
  assert.deepEqual(parsePinned('["todo","evil","todo","meal"]', allowed), [
    "todo",
    "meal",
  ]);
});
