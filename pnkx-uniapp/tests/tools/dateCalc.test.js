/**
 * 日期计算引擎单测
 * 运行：node tests/tools/dateCalc.test.js
 */
const fs = require('fs')
const path = require('path')
const assert = require('assert')

// dateCalc.js 以 ESM import 引入 uni-calendar 农历库，测试环境手动装配依赖
const calSrc = fs.readFileSync(
  path.resolve(__dirname, '../../uni_modules/uni-calendar/components/uni-calendar/calendar.js'), 'utf8')
  .replace(/export default calendar\s*$/, '')
const calendar = new Function(`${calSrc}\nreturn calendar`)()

const engineSrc = fs.readFileSync(path.resolve(__dirname, '../..', 'utils/tools/dateCalc.js'), 'utf8')
  .replace(/^import .*$/gm, '')
  .replace(/export\s+/g, '')
const { parseDate, formatDate, toHolidayMap, isWorkday, diffDays, diffNatural,
  countWorkdays, addWorkdays, addDays, weekdayCn, solarToLunar } =
  new Function('calendar', `${engineSrc}\nreturn { parseDate, formatDate, toHolidayMap, isWorkday, diffDays, diffNatural, countWorkdays, addWorkdays, addDays, weekdayCn, solarToLunar }`)(calendar)

function test(name, fn) {
  try {
    fn()
    console.log(`ok - ${name}`)
  } catch (error) {
    console.error(`not ok - ${name}`)
    console.error(error.message)
    process.exitCode = 1
  }
}

// 2026 春节示例节假日表：2/16-2/22 休假，2/14（周六）与 2/28（周六）调休补班
const HOLIDAY_MAP = toHolidayMap({
  2026: [
    { date: '2026-02-14', type: 'workday', name: '春节调休补班' },
    { date: '2026-02-16', type: 'holiday', name: '春节' },
    { date: '2026-02-17', type: 'holiday', name: '春节' },
    { date: '2026-02-18', type: 'holiday', name: '春节' },
    { date: '2026-02-19', type: 'holiday', name: '春节' },
    { date: '2026-02-20', type: 'holiday', name: '春节' },
    { date: '2026-02-21', type: 'holiday', name: '春节' },
    { date: '2026-02-22', type: 'holiday', name: '春节' },
    { date: '2026-02-28', type: 'workday', name: '春节调休补班' }
  ]
})

test('diffDays：同月/跨月/跨年/负数', () => {
  assert.strictEqual(diffDays('2026-01-01', '2026-01-31'), 30)
  assert.strictEqual(diffDays('2026-01-01', '2026-02-01'), 31)
  assert.strictEqual(diffDays('2025-01-01', '2026-01-01'), 365)
  assert.strictEqual(diffDays('2026-03-01', '2026-02-01'), -28)
})

test('diffNatural：整年整月与零头', () => {
  const r1 = diffNatural('2025-01-15', '2026-03-18')
  assert.deepStrictEqual([r1.years, r1.months, r1.days], [1, 2, 3])
  assert.strictEqual(r1.text, '1 年 零 2 个月 零 3 天')

  const r2 = diffNatural('2026-02-17', '2026-02-17')
  assert.strictEqual(r2.text, '0 天')
})

test('diffNatural：1/31 → 3/1 借位场景 = 1 个月 1 天', () => {
  const r = diffNatural('2025-01-31', '2025-03-01')
  assert.deepStrictEqual([r.years, r.months, r.days], [0, 1, 1])
})

test('diffNatural：闰年 2 月锚点', () => {
  // 2024-01-31 + 1 个月 clamp 到 2/29，剩余 1 天 → 1 个月 1 天
  const r = diffNatural('2024-01-31', '2024-03-01')
  assert.deepStrictEqual([r.years, r.months, r.days], [0, 1, 1])
})

test('isWorkday：周末/节假日/调休补班', () => {
  assert.strictEqual(isWorkday('2026-02-13', HOLIDAY_MAP), true)  // 周五
  assert.strictEqual(isWorkday('2026-02-14', HOLIDAY_MAP), true)  // 周六但调休补班
  assert.strictEqual(isWorkday('2026-02-15', HOLIDAY_MAP), false) // 周日
  assert.strictEqual(isWorkday('2026-02-17', HOLIDAY_MAP), false) // 春节初二
  assert.strictEqual(isWorkday('2026-02-23', HOLIDAY_MAP), true)  // 周一恢复上班
  assert.strictEqual(isWorkday('2026-02-14'), false)              // 无节假日表时周六非工作日
})

test('countWorkdays：跨春节区间（含调休补班日 2/14）', () => {
  // 2/13(五) 到 2/24(二)：2/13、2/14(补班)、2/23、2/24 共 4 个工作日
  assert.strictEqual(countWorkdays('2026-02-13', '2026-02-24', HOLIDAY_MAP), 4)
  // 双端包含：同一天
  assert.strictEqual(countWorkdays('2026-02-13', '2026-02-13', HOLIDAY_MAP), 1)
})

test('addWorkdays：跨春节跳休 + 调休补班计入', () => {
  // 2/13(五) +1 = 2/14（周六但调休补班）
  assert.strictEqual(addWorkdays('2026-02-13', 1, HOLIDAY_MAP), '2026-02-14')
  // +2 跳过春节假期落在 2/23（周一）
  assert.strictEqual(addWorkdays('2026-02-13', 2, HOLIDAY_MAP), '2026-02-23')
  assert.strictEqual(addWorkdays('2026-02-13', 3, HOLIDAY_MAP), '2026-02-24')
  // 2/27(五) +1 = 2/28（周六但调休补班）
  assert.strictEqual(addWorkdays('2026-02-27', 1, HOLIDAY_MAP), '2026-02-28')
  assert.strictEqual(addWorkdays('2026-02-13', 0, HOLIDAY_MAP), '2026-02-13')
})

test('addDays：跨月/负数/非闰年', () => {
  assert.strictEqual(addDays('2026-02-28', 1), '2026-03-01')   // 2026 非闰年
  assert.strictEqual(addDays('2024-02-28', 1), '2024-02-29')   // 2024 闰年
  assert.strictEqual(addDays('2026-03-01', -1), '2026-02-28')
})

test('weekdayCn', () => {
  assert.strictEqual(weekdayCn('2026-02-17'), '星期二')
  assert.strictEqual(weekdayCn('2026-11-20'), '星期五')
})

test('solarToLunar：春节/中秋/干支生肖', () => {
  const chunjie = solarToLunar('2026-02-17')
  assert.strictEqual(chunjie.text, '正月初一')
  assert.strictEqual(chunjie.animal, '马')
  assert.strictEqual(chunjie.gzYear, '丙午')

  const zhongqiu = solarToLunar('2026-09-25')
  assert.strictEqual(zhongqiu.text, '八月十五')
})

test('toHolidayMap：配置展平与缺失容错', () => {
  assert.strictEqual(HOLIDAY_MAP['2026-02-17'].type, 'holiday')
  assert.deepStrictEqual(toHolidayMap(null), {})
  assert.deepStrictEqual(toHolidayMap({ 2026: null }), {})
})

test('非法输入抛错', () => {
  assert.throws(() => parseDate('2026/01/01'), /格式/)
  assert.throws(() => countWorkdays('2026-02-20', '2026-02-10', HOLIDAY_MAP), /早于/)
  assert.throws(() => addWorkdays('2026-02-13', -1, HOLIDAY_MAP), /非负整数/)
})

console.log('')
