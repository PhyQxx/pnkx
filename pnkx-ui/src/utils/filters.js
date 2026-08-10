/**
 * 时间格式过滤器
 * @param valueTime
 * @returns {string}
 */
export function timeFilter(valueTime) {
    if (valueTime) {
        let date = new Date(valueTime.toString().replace(/-/g, '/'));
        if (isNaN(date.getTime())) return valueTime;
        let diffTime = Math.abs(new Date().getTime() - date.getTime());
        if (diffTime > 7 * 24 * 3600 * 1000) {
            let m = date.getMonth() + 1;
            m = m < 10 ? ('0' + m) : m;
            let d = date.getDate();
            d = d < 10 ? ('0' + d) : d;
            let h = date.getHours();
            h = h < 10 ? ('0' + h) : h;
            let minute = date.getMinutes();
            minute = minute < 10 ? ('0' + minute) : minute;
            return m + '-' + d + ' ' + h + ':' + minute
        } else if (diffTime >= 24 * 3600 * 1000) {
            let dayNum = Math.floor(diffTime / (24 * 60 * 60 * 1000));
            return dayNum + '天前'
        } else if (diffTime >= 3600 * 1000) {
            let dayNum = Math.floor(diffTime / (60 * 60 * 1000));
            return dayNum + '小时前'
        } else if (diffTime >= 60 * 1000) {
            let dayNum = Math.floor(diffTime / (60 * 1000));
            return dayNum + '分钟前'
        } else {
            return '刚刚'
        }
    }
}

/**
 * 钱数过滤器
 * @param valueMoney
 * @returns {string}
 */
export function moneyFilter(valueMoney) {
    let result = '', counter = 0;
    let negativeFlag = false;
    if (valueMoney === undefined || valueMoney === null || valueMoney === '' || valueMoney === 'null') {
        valueMoney = 0
    }
    if (typeof valueMoney === 'string') {
        if (valueMoney.slice(0, 1) === '-') {
            valueMoney = valueMoney.slice(1);
            negativeFlag = true;
        }
        valueMoney = parseFloat(valueMoney).toFixed(2).toString();
    }
    if (typeof valueMoney === 'number') {
        if (valueMoney < 0) {
            valueMoney = valueMoney * (-1);
            negativeFlag = true;
        }
        valueMoney = valueMoney.toFixed(2).toString();
    }
    let decimal = valueMoney.slice(valueMoney.indexOf('.'));
    valueMoney = valueMoney.slice(0, valueMoney.indexOf('.'));
    for (let i = valueMoney.length - 1; i >= 0; i--) {
        counter++;
        result = valueMoney.charAt(i) + result;
        if (!(counter % 3) && i !== 0) {
            result = ',' + result;
        }
    }
    if (negativeFlag) {
        result = '-' + result;
    }
    return result + decimal;
}

/**
 * 文字超出样式过滤器
 * @param value
 * @returns {string|*}
 */
export function ellipsisFilter(value) {
    if (!value) {
        return
    }
    let len = value.length;
    if (!value) return ''
    if (value.length > 8) {
        return value.substring(0, 4) + '···' + value.substring(len - 2, len)
    }
    return value
}
