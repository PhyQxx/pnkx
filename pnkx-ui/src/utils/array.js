
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
