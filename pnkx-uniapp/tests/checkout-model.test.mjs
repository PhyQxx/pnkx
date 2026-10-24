import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
const src = await readFile(
  new URL("../utils/checkoutModel.js", import.meta.url),
  "utf8"
);
const m = await import(
  `data:text/javascript;base64,${Buffer.from(src).toString("base64")}`
);
const items = [
  { id: 1, fulfillmentState: "purchased" },
  { id: 2, fulfillmentState: "available" },
  { id: 3, fulfillmentState: "purchased", batchId: 9 },
];
test("checkout contracts and controller match across clients", async () => {
  assert.equal(
    src,
    await readFile(
      new URL("../../pnkx-ui/src/utils/checkoutModel.js", import.meta.url),
      "utf8"
    )
  );
  assert.equal(
    await readFile(
      new URL("../utils/useShoppingCheckout.js", import.meta.url),
      "utf8"
    ),
    await readFile(
      new URL(
        "../../pnkx-ui/src/utils/useShoppingCheckout.js",
        import.meta.url
      ),
      "utf8"
    )
  );
});
test("only purchased unsettled items can be selected", () => {
  assert.deepEqual(
    m.eligibleItems(items).map((i) => i.id),
    [1]
  );
  assert.equal(m.checkoutError({ purchasedItemIds: [1] }, null, items), "");
  assert.match(m.checkoutError({ purchasedItemIds: [2] }, null, items), /变化/);
  assert.match(
    m.checkoutError({ purchasedItemIds: [] }, null, items),
    /请选择/
  );
});
test("optional expense never sends stale private finance fields", () =>
  assert.deepEqual(
    m.checkoutPayload(
      {
        purchasedItemIds: [1],
        recordExpense: false,
        accountId: 8,
        totalAmount: 10,
      },
      null
    ),
    { recordExpense: false, purchasedItemIds: [1] }
  ));
test("expense requires valid money, accounts and calendar date", () => {
  const f = {
    purchasedItemIds: [1],
    recordExpense: true,
    totalAmount: "12.50",
    accountId: 1,
    classificationId: 1,
    paidAt: "2026-10-24",
  };
  assert.equal(m.checkoutError(f, null, items, "2026-10-24"), "");
  for (const amount of ["", "-1", "1.234", "Infinity", "1e3"])
    assert.notEqual(
      m.checkoutError({ ...f, totalAmount: amount }, null, items, "2026-10-24"),
      ""
    );
  assert.notEqual(
    m.checkoutError({ ...f, paidAt: "2026-02-30" }, null, items, "2026-10-24"),
    ""
  );
  assert.notEqual(
    m.checkoutError({ ...f, paidAt: "2026-10-25" }, null, items, "2026-10-24"),
    ""
  );
  assert.notEqual(
    m.checkoutError({ ...f, accountId: null }, null, items, "2026-10-24"),
    ""
  );
});
test("identical retry reuses key even after changing input and reverting", () => {
  let n = 0;
  const key = m.requestKeyCache(() => String(++n));
  const a = m.checkoutPayload({ purchasedItemIds: [2, 1] }, null),
    b = m.checkoutPayload({ purchasedItemIds: [1] }, null);
  assert.equal(key(a), "1");
  assert.equal(key(b), "2");
  assert.equal(key(m.checkoutPayload({ purchasedItemIds: [1, 2] }, null)), "1");
});
test("business date uses Shanghai even across UTC midnight", () =>
  assert.equal(
    m.businessToday(new Date("2026-10-24T17:00:00Z")),
    "2026-10-25"
  ));
