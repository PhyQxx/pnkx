/**
 * 日期格式化
 * @param {Object} time
 * @param {Object} pattern
 */
export function parseTime(time, pattern) {
  if (arguments.length === 0 || !time) {
    return null
  }
  const format = pattern || '{y}-{m}-{d} {h}:{i}:{s}'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if ((typeof time === 'string') && (/^[0-9]+$/.test(time))) {
      time = parseInt(time)
    } else if (typeof time === 'string') {
      time = time.replace(new RegExp(/-/gm), '/');
    }
    if ((typeof time === 'number') && (time.toString().length === 10)) {
      time = time * 1000
    }
    date = new Date(time)
  }
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  }
  const time_str = format.replace(/{(y|m|d|h|i|s|a)+}/g, (result, key) => {
    let value = formatObj[key]
    // Note: getDay() returns 0 on Sunday
    if (key === 'a') {
      return ['日', '一', '二', '三', '四', '五', '六'][value]
    }
    if (result.length > 0 && value < 10) {
      value = '0' + value
    }
    return value || 0
  })
  return time_str
}


/**
 * 获取当前时间
 * @returns {string}
 */
export function getNow() {
  let date = new Date();
  let year = date.getFullYear();
  let month = date.getMonth() < 9 ? "0" + (date.getMonth() + 1) : (date.getMonth() + 1);
  let day = date.getDate() < 10 ? "0" + date.getDate() : date.getDate();
  let hours = date.getHours() < 10 ? "0" + date.getHours() : date.getHours();
  let minutes = date.getMinutes() < 10 ? "0" + date.getMinutes() : date.getMinutes();
  let seconds = date.getSeconds() < 10 ? '0' + date.getSeconds() : date.getSeconds();
  let now = year + "-" + month + "-" + day + " " + hours + ":" + minutes + ':' + seconds;
  return now
}

/**
 * 数组求和
 * @param arr 数组
 * @param key key
 * @returns {number} 和
 */
export function arraySum(arr, key) {
  if (!arr) {
    return 0
  }
  let res = 0;
  arr.forEach(item => {
    res += Number(item[key])
  })
  return res
}

/**
 * 完整计算时间差(天、小时、分钟、秒)
 * @param d1    时间点
 * @param d2
 * @returns {string}
 */
export function getTimeDifference(d1, d2) {
  //如果时间格式是正确的，那下面这一步转化时间格式就可以不用了
  let dateBegin = new Date(d1.replace(/-/g, "/"));//将-转化为/，使用new Date
  let dateEnd;
  if (d2) {
    dateEnd = new Date(d2.replace(/-/g, "/"))
  } else {
    dateEnd = new Date();//获取当前时间
  }
  let dateDiff = dateEnd.getTime() - dateBegin.getTime();//时间差的毫秒数
  let dayDiff = Math.floor(dateDiff / (24 * 3600 * 1000));//计算出相差天数
  let leave1 = dateDiff % (24 * 3600 * 1000) //计算天数后剩余的毫秒数
  let hours = Math.floor(leave1 / (3600 * 1000))//计算出小时数
  //计算相差分钟数
  let leave2 = leave1 % (3600 * 1000) //计算小时数后剩余的毫秒数
  let minutes = Math.floor(leave2 / (60 * 1000))//计算相差分钟数
  //计算相差秒数
  let leave3 = leave2 % (60 * 1000) //计算分钟数后剩余的毫秒数
  let seconds = Math.round(leave3 / 1000)
  return dayDiff + "天" + hours + "小时" + minutes + "分钟" + seconds + "秒"
}

/**
 * 格式化日期为 YYYY-MM-DD
 * @param {Date|string|number} date
 * @returns {string}
 */
export function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * 格式化日期为 YYYY年M月D日
 * @param {Date|string|number} date
 * @param {boolean} showWeek 是否显示星期
 * @returns {string}
 */
export function formatDateFull(date, showWeek = false) {
  if (!date) return ''
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  let result = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
  if (showWeek) {
    const weekList = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    result += ` ${weekList[d.getDay()]}`
  }
  return result
}

/**
 * 排序从大到小
 * @param ary
 * @param key
 * @returns {*}
 */
export function sortDesByKey(ary, key) {
  return ary.sort((a, b) => {
    let x = a[key];
    let y = b[key];
    return ((x < y) ? 1 : (x > y) ? -1 : 0)
  })
}

/**
 * 排序从小到大
 * @param ary
 * @param key
 * @returns {*}
 */
export function sortAscByKey(ary, key) {
  return ary.sort((a, b) => {
    let x = a[key];
    let y = b[key];
    return ((x < y) ? -1 : (x > y) ? 1 : 0)
  })
}


/**
 * 防抖
 * @param fn    函数
 * @param delay  等待时间
 * @returns {function(...[*]=)}
 */
export function debounce(fn, delay = 1000) {
  let timer = null;
  return function (...args) {
    if (timer) {
      clearTimeout(timer)
    }
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}


/**
 * 节流
 * @param fn  函数
 * @param delay  等待时间
 * @returns {function(...[*]=)}
 */
export function throttle(fn, delay) {
  let valid = true;
  return function (...args) {
    if (!valid) {
      return false
    }
    valid = false
    setTimeout(() => {
      fn.apply(this, args)
      valid = true;
    }, delay)
  }
}
