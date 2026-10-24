export function eligibleItems(items) {
    return items.filter((i) => i.fulfillmentState === 'purchased' && !i.batchId);
}
export function businessToday(now = new Date()) {
    const parts = new Intl.DateTimeFormat('en-CA', {
        timeZone: 'Asia/Shanghai',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    }).formatToParts(now);
    const get = (t) => parts.find((p) => p.type === t).value;
    return `${get('year')}-${get('month')}-${get('day')}`;
}
export function checkoutError(form, batchId, items, today = businessToday()) {
    const ids = form.purchasedItemIds || [];
    if (!batchId) {
        if (!ids.length || ids.length > 200) return '请选择 1 至 200 项本次采购的商品';
        const eligible = new Set(eligibleItems(items).map((i) => String(i.id)));
        if (ids.some((id) => !eligible.has(String(id)))) return '部分商品状态已变化，请重新打开结算并核对';
    }
    if (form.recordExpense || batchId) {
        const amount = String(form.totalAmount ?? '').trim();
        if (!/^\d+(\.\d{1,2})?$/.test(amount) || !Number.isFinite(Number(amount)))
            return '请输入实际总额，最多两位小数';
        if (!form.accountId) return '请选择扣费账户';
        if (!form.classificationId) return '请选择支出分类';
        const date = form.paidAt;
        if (
            !/^\d{4}-\d{2}-\d{2}$/.test(date || '') ||
            Number.isNaN(Date.parse(date)) ||
            new Date(date).toISOString().slice(0, 10) !== date
        )
            return '请选择有效的扣费日期';
        if (date > today) return '扣费日期不能晚于今天';
    }
    return '';
}
export function checkoutPayload(form, batchId) {
    const payload = { recordExpense: !!(form.recordExpense || batchId) };
    if (!batchId)
        payload.purchasedItemIds = [...(form.purchasedItemIds || [])].sort((a, b) =>
            String(a).localeCompare(String(b))
        );
    if (payload.recordExpense) {
        payload.totalAmount = String(form.totalAmount).trim();
        payload.accountId = form.accountId;
        payload.classificationId = form.classificationId;
        payload.paidAt = form.paidAt;
    }
    return payload;
}
// Keep the same key for identical retries, including switching away and back after a failure.
export function requestKeyCache(makeKey) {
    const keys = new Map();
    return (payload) => {
        const fingerprint = JSON.stringify(payload);
        if (!keys.has(fingerprint)) keys.set(fingerprint, makeKey());
        return keys.get(fingerprint);
    };
}
