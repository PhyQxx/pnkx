// Both clients share one shopping grouping contract; parity is checked by the Node tests.
// 按履约状态分组:待购 / 家里已有 / 已买到 / 已结算。
// 已结算(batchId)优先归类;旧数据缺少 fulfillmentState 时按勾选状态映射到待购/已买到。
export function groupItems(items) {
    const groups = { needed: [], available: [], purchased: [], settled: [] };
    for (const item of items || []) {
        if (item.batchId) groups.settled.push(item);
        else if (item.fulfillmentState === 'available') groups.available.push(item);
        else if (item.fulfillmentState === 'purchased' || (!item.fulfillmentState && item.checked))
            groups.purchased.push(item);
        else groups.needed.push(item);
    }
    return groups;
}
export const groupMeta = {
    needed: { label: '待购', tone: 'primary' },
    available: { label: '家里已有', tone: 'success' },
    purchased: { label: '已买到', tone: 'warning' },
    settled: { label: '已结算', tone: 'info' }
};
