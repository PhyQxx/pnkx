import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
const labels = {
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
const values = {
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
function display(v, key) {
    if (v == null) return '未设置';
    if (key.endsWith('Date') && typeof v === 'number') return new Date(v).toLocaleDateString();
    return values[v] || String(v);
}
export async function changeRule(sourceType, changes) {
    const r = await request({
        url: '/charge/rules/change-preview',
        method: 'post',
        data: { sourceType, sourceId: changes.id, changes }
    });
    const text =
        (r.data.changes || [])
            .map((c) => `${labels[c.field] || c.field}：${display(c.before, c.field)} → ${display(c.after, c.field)}`)
            .join('；') +
        '。' +
        r.data.notice +
        ((r.data.duplicates || []).length
            ? '另有同金额规则：' + r.data.duplicates.map((d) => d.name).join('、') + '，请核对是否重复并保留主规则。'
            : '');
    await ElMessageBox.confirm(text, '确认规则变更', { type: 'warning' });
    return request({ url: '/charge/rules/change-commit', method: 'post', data: { previewId: r.data.previewId } });
}

export async function backfillRule(sourceType, sourceId) {
    const r = await request({ url: '/charge/rules/backfill-preview', method: 'post', data: { sourceType, sourceId } });
    const text = r.data.periods.join('、') + '；每期预计 ' + r.data.amountPerPeriod + ' 元。' + r.data.notice;
    await ElMessageBox.confirm(text, '确认补齐账期', { type: 'warning' });
    return request({ url: '/charge/rules/change-commit', method: 'post', data: { previewId: r.data.previewId } });
}
