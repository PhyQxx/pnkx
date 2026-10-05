/**
 * 纪念日倒计时纯函数——从 index.vue 拆出，供 DaySidebar / DayDetailPanel 共用。
 * nowTime 为父组件每秒刷新的当前时间字符串（保持原有刷新机制）。
 */

/**
 * 获取提示语句（"还有" / "已经"）
 */
export function getRepeatLabel(item, nowTime) {
    if (item.repeat) {
        return '还有'
    }
    if (new Date(item.date.replace(/-/g, '/')).getTime() > new Date(nowTime.replace(/-/g, '/')).getTime()) {
        return '还有'
    }
    return '已经'
}

/**
 * 获取倒计时单位分解（天/时/分/秒，按周年循环取最近一次）
 */
export function getCountdownUnits(item, nowTime) {
    let targetDate
    const now = new Date(nowTime.replace(/-/g, '/')).getTime()
    if (now < new Date(item.date.replace(/-/g, '/')).getTime()) {
        targetDate = item.date
    } else {
        const yearDate = new Date().getFullYear() + item.date.slice(4)
        if (now < new Date(yearDate.replace(/-/g, '/')).getTime()) {
            targetDate = yearDate
        } else {
            targetDate = (Number(new Date().getFullYear()) + 1) + item.date.slice(4)
        }
    }

    const dateBegin = new Date(nowTime.replace(/-/g, '/'))
    const dateEnd = new Date(targetDate.replace(/-/g, '/'))
    const dateDiff = Math.abs(dateEnd.getTime() - dateBegin.getTime())

    const days = Math.floor(dateDiff / (24 * 3600 * 1000))
    const leave1 = dateDiff % (24 * 3600 * 1000)
    const hours = Math.floor(leave1 / (3600 * 1000))
    const leave2 = leave1 % (3600 * 1000)
    const minutes = Math.floor(leave2 / (60 * 1000))
    const leave3 = leave2 % (60 * 1000)
    const seconds = Math.round(leave3 / 1000)

    return {days, hours, minutes, seconds}
}

/**
 * 获取简短倒计时（侧边栏用）
 */
export function getCountdownShort(item, nowTime) {
    const units = getCountdownUnits(item, nowTime)
    if (units.days > 0) return units.days + '天'
    if (units.hours > 0) return units.hours + '小时'
    return units.minutes + '分钟'
}
