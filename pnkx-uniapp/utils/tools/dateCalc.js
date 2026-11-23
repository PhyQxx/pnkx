/**
 * 日期计算引擎（纯函数，与 UI 解耦，可直接单测）
 *
 * - 日期格式统一 'YYYY-MM-DD'
 * - 工作日口径：周一至周五，减去法定节假日（type=holiday），加上调休补班（type=workday）
 * - 农历转换复用 uni_modules/uni-calendar 内置算法（覆盖 1900-2100）
 */

import calendar from '@/uni_modules/uni-calendar/components/uni-calendar/calendar.js'

/** 星期中文（0=周日） */
const WEEK_CN = ['日', '一', '二', '三', '四', '五', '六']

/**
 * 解析 'YYYY-MM-DD' 为本地日期
 * 手动拆解避免 new Date(string) 的 UTC 偏移问题
 * @param {string} dateStr 日期
 * @returns {Date}
 */
export function parseDate(dateStr) {
  const match = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(String(dateStr || '').trim())
  if (!match) {
    throw new Error('日期格式错误，应为 YYYY-MM-DD：' + dateStr)
  }
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
  if (isNaN(date.getTime())) {
    throw new Error('无效日期：' + dateStr)
  }
  return date
}

/**
 * 日期格式化为 'YYYY-MM-DD'
 * @param {Date} date 日期
 * @returns {string}
 */
export function formatDate(date) {
  const m = date.getMonth() + 1
  const d = date.getDate()
  return date.getFullYear() + '-' + (m < 10 ? '0' + m : m) + '-' + (d < 10 ? '0' + d : d)
}

/**
 * 当月天数
 * @param {number} y 年
 * @param {number} m 月
 * @returns {number}
 */
function daysInMonth(y, m) {
  return new Date(y, m, 0).getDate()
}

/**
 * 节假日配置（后台 sys.holiday.data）展平为 { 'YYYY-MM-DD': {type, name} } 快查表
 * 配置结构：{ "2026": [ { date, type: 'holiday'|'workday', name } ], ... }
 * @param {Object} config 后台配置对象
 * @returns {Object}
 */
export function toHolidayMap(config) {
  const map = {}
  if (!config) return map
  Object.keys(config).forEach(year => {
    const list = config[year]
    if (Array.isArray(list)) {
      list.forEach(item => {
        if (item && item.date) {
          map[item.date] = { type: item.type, name: item.name || '' }
        }
      })
    }
  })
  return map
}

/**
 * 是否工作日
 * @param {string} dateStr 日期
 * @param {Object} holidayMap 节假日快查表
 * @returns {boolean}
 */
export function isWorkday(dateStr, holidayMap = {}) {
  const override = holidayMap[dateStr]
  if (override) {
    return override.type === 'workday'
  }
  const dow = parseDate(dateStr).getDay()
  return dow >= 1 && dow <= 5
}

/**
 * 日期间隔天数（to − from，支持负数）
 * @param {string} fromStr 开始日期
 * @param {string} toStr 结束日期
 * @returns {number}
 */
export function diffDays(fromStr, toStr) {
  return Math.round((parseDate(toStr) - parseDate(fromStr)) / 86400000)
}

/**
 * 月份推算（带日 clamp）：from 加 n 个月后同日；目标月无该日则取月末
 * 如 1 月 31 日 + 1 个月 = 2 月 28 日
 * @param {Date} from 起始日期
 * @param {number} n 月数（可为负）
 * @returns {Date}
 */
function addMonthsClamp(from, n) {
  const y = from.getFullYear()
  const m = from.getMonth() + n
  const d = from.getDate()
  const targetY = y + Math.floor(m / 12)
  const targetM = ((m % 12) + 12) % 12
  const clampedD = Math.min(d, daysInMonth(targetY, targetM + 1))
  return new Date(targetY, targetM, clampedD)
}

/**
 * 自然年月日差（人类口径，如「1 年 2 个月零 3 天」）
 * 算法：先按整月推进锚点（日不足则取月末 clamp），再以剩余天数补齐
 * @param {string} fromStr 开始日期（须早于结束日期）
 * @param {string} toStr 结束日期
 * @returns {{years:number, months:number, days:number, text:string}}
 */
export function diffNatural(fromStr, toStr) {
  const from = parseDate(fromStr)
  const to = parseDate(toStr)
  if (to < from) {
    throw new Error('开始日期须早于或等于结束日期')
  }
  // 粗算总月数：锚点超过 to 则回退一个月
  let months = (to.getFullYear() - from.getFullYear()) * 12 + (to.getMonth() - from.getMonth())
  if (months > 0 && addMonthsClamp(from, months) > to) {
    months -= 1
  }
  if (months < 0) months = 0
  const anchor = addMonthsClamp(from, months)
  const days = Math.round((to - anchor) / 86400000)
  const years = Math.floor(months / 12)
  months = months % 12
  // 拼接中文文本
  const parts = []
  if (years > 0) parts.push(years + ' 年')
  if (months > 0) parts.push(months + ' 个月')
  if (days > 0 || parts.length === 0) parts.push(days + ' 天')
  const text = parts.length > 1 ? parts.slice(0, -1).join(' 零 ') + ' 零 ' + parts[parts.length - 1] : parts[0]
  return { years, months, days, text }
}

/**
 * 统计工作日天数（双端包含）
 * @param {string} fromStr 开始日期（须早于等于结束日期）
 * @param {string} toStr 结束日期
 * @param {Object} holidayMap 节假日快查表
 * @returns {number}
 */
export function countWorkdays(fromStr, toStr, holidayMap = {}) {
  const from = parseDate(fromStr)
  const to = parseDate(toStr)
  if (to < from) {
    throw new Error('开始日期须早于或等于结束日期')
  }
  let count = 0
  const cursor = new Date(from)
  while (cursor <= to) {
    if (isWorkday(formatDate(cursor), holidayMap)) {
      count += 1
    }
    cursor.setDate(cursor.getDate() + 1)
  }
  return count
}

/**
 * 工作日推算：从 fromStr 起（不含当天）数第 n 个工作日的日期；n=0 返回当天
 * @param {string} fromStr 起始日期
 * @param {number} n 工作日数（>=0）
 * @param {Object} holidayMap 节假日快查表
 * @returns {string} 'YYYY-MM-DD'
 */
export function addWorkdays(fromStr, n, holidayMap = {}) {
  if (!(n >= 0) || !Number.isInteger(n)) {
    throw new Error('工作日数必须为非负整数')
  }
  const cursor = parseDate(fromStr)
  if (n === 0) {
    return formatDate(cursor)
  }
  let counted = 0
  while (counted < n) {
    cursor.setDate(cursor.getDate() + 1)
    if (isWorkday(formatDate(cursor), holidayMap)) {
      counted += 1
    }
  }
  return formatDate(cursor)
}

/**
 * 自然日推算（n 可正可负）
 * @param {string} fromStr 起始日期
 * @param {number} n 天数（正=往后，负=往前）
 * @returns {string} 'YYYY-MM-DD'
 */
export function addDays(fromStr, n) {
  const cursor = parseDate(fromStr)
  cursor.setDate(cursor.getDate() + n)
  return formatDate(cursor)
}

/**
 * 星期中文
 * @param {string} dateStr 日期
 * @returns {string} 如 星期一
 */
export function weekdayCn(dateStr) {
  return '星期' + WEEK_CN[parseDate(dateStr).getDay()]
}

/**
 * 公历转农历（复用 uni-calendar 内置算法，区间 1900-2100）
 * @param {string} dateStr 日期
 * @returns {{text:string, monthCn:string, dayCn:string, animal:string, gzYear:string, gzMonth:string, gzDay:string, weekCn:string, term:string, astro:string, isLeap:boolean, lunarYear:number}}
 */
export function solarToLunar(dateStr) {
  const date = parseDate(dateStr)
  const lunar = calendar.solar2lunar(date.getFullYear(), date.getMonth() + 1, date.getDate())
  if (!lunar || lunar === -1) {
    throw new Error('该日期超出农历转换范围（1900-2100）')
  }
  return {
    text: (lunar.isLeap ? '闰' : '') + lunar.IMonthCn + lunar.IDayCn,
    monthCn: lunar.IMonthCn,
    dayCn: lunar.IDayCn,
    lunarYear: lunar.lYear,
    animal: lunar.Animal,
    gzYear: lunar.gzYear,
    gzMonth: lunar.gzMonth,
    gzDay: lunar.gzDay,
    weekCn: lunar.ncWeek,
    term: lunar.isTerm ? lunar.Term : '',
    astro: lunar.astro,
    isLeap: !!lunar.isLeap
  }
}
