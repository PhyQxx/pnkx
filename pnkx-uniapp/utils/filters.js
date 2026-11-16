import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

// 配置 dayjs
dayjs.locale('zh-cn')
dayjs.extend(relativeTime)

// 时间常量（毫秒）
const TIME_CONSTANTS = {
  MILLISECOND: 1,
  SECOND: 1000,
  MINUTE: 60 * 1000,
  HOUR: 60 * 60 * 1000,
  DAY: 24 * 60 * 60 * 1000,
  WEEK: 7 * 24 * 60 * 60 * 1000,
}

/**
 * 时间格式过滤器
 * @param {string|Date} valueTime 时间值
 * @returns {string} 格式化后的时间字符串
 */
export function timeFilter(valueTime) {
  if (!valueTime) return ''

  const date = new Date(valueTime.toString().replace(/-/g, '/'))
  if (isNaN(date.getTime())) return valueTime

  const now = new Date()
  const diffTime = Math.abs(now.getTime() - date.getTime())

  // 超过7天显示具体日期
  if (diffTime > TIME_CONSTANTS.WEEK) {
    return dayjs(date).format('MM-DD HH:mm')
  }

  // 使用 dayjs relativeTime 插件
  return dayjs(date).fromNow()
}

/**
 * 钱数过滤器
 * @param {number|string} valueMoney 金额值
 * @returns {string} 格式化后的金额字符串（千分位）
 */
export function moneyFilter(valueMoney) {
  // 处理空值
  if (valueMoney === undefined || valueMoney === null || valueMoney === '' || valueMoney === 'null') {
    return '0.00'
  }

  // 转换为数字
  let num = typeof valueMoney === 'string' ? parseFloat(valueMoney) : valueMoney
  if (isNaN(num)) return '0.00'

  // 处理负数
  const isNegative = num < 0
  num = Math.abs(num)

  // 格式化为两位小数
  const formatted = num.toFixed(2)

  // 分割整数和小数部分
  const [integerPart, decimalPart] = formatted.split('.')

  // 添加千分位分隔符
  const formattedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')

  return (isNegative ? '-' : '') + formattedInteger + '.' + decimalPart
}

// 文字截断常量
const ELLIPSIS_CONFIG = {
  MAX_LENGTH: 8,
  PREFIX_LENGTH: 4,
  SUFFIX_LENGTH: 2,
  SEPARATOR: '···',
}

/**
 * 文字超出样式过滤器
 * @param {string} value 文本值
 * @returns {string} 截断后的文本
 */
export function ellipsisFilter(value) {
  if (!value || typeof value !== 'string') return value || ''

  if (value.length > ELLIPSIS_CONFIG.MAX_LENGTH) {
    const prefix = value.substring(0, ELLIPSIS_CONFIG.PREFIX_LENGTH)
    const suffix = value.substring(value.length - ELLIPSIS_CONFIG.SUFFIX_LENGTH)
    return prefix + ELLIPSIS_CONFIG.SEPARATOR + suffix
  }

  return value
}
