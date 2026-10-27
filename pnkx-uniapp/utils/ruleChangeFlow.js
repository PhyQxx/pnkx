// Both clients share one rule-change flow contract; parity is checked by the Node tests.
// 账期规则调整短流程(变化摘要 → 影响账期 → 确认)的共享格式化与展示器注册。
// 展示器由各端 ChargeRuleChangeFlow 组件挂载时注册;未注册时 API 走各自的降级确认。
export const CHANGE_PREVIEW_URL = '/charge/rules/change-preview';
export const BACKFILL_PREVIEW_URL = '/charge/rules/backfill-preview';
export const CHANGE_COMMIT_URL = '/charge/rules/change-commit';

export const fieldLabels = {
    name: '名称',
    amount: '金额',
    money: '金额',
    cycle: '周期',
    cycleInterval: '周期间隔',
    nextPaymentDate: '下次日期',
    frequency: '频率',
    dayNumber: '执行日',
    postingMode: '入账方式',
    lifecycle: '状态',
    trialEndDate: '试用结束',
    enabled: '启用',
    accountId: '账户',
    account: '账户',
    classificationId: '分类',
    type: '分类',
    otherAccount: '转入账户',
    remark: '备注'
};
const valueLabels = {
    active: '追踪中',
    paused: '暂停',
    cancelled: '终止追踪',
    manual_confirm: '到期确认',
    auto_record: '自动记账',
    monthly: '每月',
    month: '每月',
    weekly: '每周',
    week: '每周',
    yearly: '每年',
    daily: '每天'
};
export function formatValue(value, field) {
    if (value == null) return '未设置';
    if (field.endsWith('Date') && typeof value === 'number') return new Date(value).toLocaleDateString();
    return valueLabels[value] || String(value);
}
/** 字段差异渲染为 变化摘要 行 */
export function formatDiffs(changes) {
    return (changes || []).map((c) => ({
        field: c.field,
        label: fieldLabels[c.field] || c.field,
        before: formatValue(c.before, c.field),
        after: formatValue(c.after, c.field)
    }));
}
let presenter = null;
/** 组件挂载时注册展示器,返回注销函数;仅当前展示器可被注销 */
export function setRuleFlowPresenter(fn) {
    presenter = fn;
    return () => {
        if (presenter === fn) presenter = null;
    };
}
/** 有展示器时交给组件渲染完整三步流程;无展示器返回 null,由调用方降级为简单确认 */
export function presentRuleFlow(task) {
    return presenter ? presenter(task) : Promise.resolve(null);
}
