// 日期格式化
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
            time = time.replace(new RegExp(/-/gm), '/')
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
    let date = new Date()
    let year = date.getFullYear()
    let month = date.getMonth() < 9 ? '0' + (date.getMonth() + 1) : (date.getMonth() + 1)
    let day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate()
    let hours = date.getHours() < 10 ? '0' + date.getHours() : date.getHours()
    let minutes = date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes()
    let seconds = date.getSeconds() < 10 ? '0' + date.getSeconds() : date.getSeconds()
    let now = year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds
    return now
}

/**
 * 完整计算时间差(天、小时、分钟、秒)
 * @param d1    时间点
 * @param d2
 * @returns {string}
 */
export function getTimeDifference(d1, d2) {
    //如果时间格式是正确的，那下面这一步转化时间格式就可以不用了
    let dateBegin = new Date(d1.replace(/-/g, '/'))//将-转化为/，使用new Date
    let dateEnd
    if (d2) {
        dateEnd = new Date(d2.replace(/-/g, '/'))
    } else {
        dateEnd = new Date()//获取当前时间
    }
    let dateDiff = dateEnd.getTime() - dateBegin.getTime()//时间差的毫秒数
    let dayDiff = Math.floor(dateDiff / (24 * 3600 * 1000))//计算出相差天数
    let leave1 = dateDiff % (24 * 3600 * 1000) //计算天数后剩余的毫秒数
    let hours = Math.floor(leave1 / (3600 * 1000))//计算出小时数
    //计算相差分钟数
    let leave2 = leave1 % (3600 * 1000) //计算小时数后剩余的毫秒数
    let minutes = Math.floor(leave2 / (60 * 1000))//计算相差分钟数
    //计算相差秒数
    let leave3 = leave2 % (60 * 1000) //计算分钟数后剩余的毫秒数
    let seconds = Math.round(leave3 / 1000)
    return dayDiff + '天' + hours + '小时' + minutes + '分钟' + seconds + '秒'
}

/**
 * 时间加减
 * @param num
 * @param date
 * @returns {string}
 */
export function dateChange(num = 1, date = false) {
    if (!date) {
        date = new Date()//没有传入值时,默认是当前日期
        date = date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate()
    }
    date += ' 00:00:00'//设置为当天凌晨12点
    date = Date.parse(new Date(date)) / 1000//转换为时间戳
    date += (86400) * num//修改后的时间戳
    const newDate = new Date(parseInt(date) * 1000)//转换为时间
    return newDate.getFullYear() + '-' + (newDate.getMonth() + 1) + '-' + newDate.getDate()
}

/**
 * 计算两个日期之间的天数
 * @param dateString1  开始日期 yyyy-MM-dd
 * @param dateString2  结束日期 yyyy-MM-dd
 * @returns {number} 如果日期相同 返回一天 开始日期大于结束日期，返回0
 */
export function getDaysBetween(dateString1, dateString2) {
    const startDate = Date.parse(dateString1)
    const endDate = Date.parse(dateString2)
    if (startDate > endDate) {
        return 0
    }
    if (startDate === endDate) {
        return 1
    }
    return (endDate - startDate) / (24 * 60 * 60 * 1000)
}

/**
 * 判断日期是否在范围内
 * @param startDate 开始时间
 * @param endDate 结束时间
 * @param date 时间点
 * @returns {boolean}
 */
export function isDateInRange(startDate, endDate, date) {
    // 将传入的日期字符串转换为 Date 对象
    startDate = new Date(startDate)
    endDate = new Date(endDate)
    date = new Date(date)

    // 检查日期是否在范围内
    return date >= startDate && date <= endDate
}
