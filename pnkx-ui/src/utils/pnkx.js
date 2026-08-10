import {ElNotification} from 'element-plus'
/**
 * 通用js方法封装处理
 * Copyright (c) 2020 pnkx
 */

const baseURL = import.meta.env.VUE_APP_BASE_API

// 表单重置
export function resetForm(refName) {
    if (this.$refs[refName]) {
        this.$refs[refName].resetFields();
    }
}

// 添加日期范围
export function addDateRange(params, dateRange, propName) {
    var search = params;
    search.params = {};
    if (null != dateRange && '' != dateRange) {
        if (typeof (propName) === "undefined") {
            search.params["beginTime"] = dateRange[0];
            search.params["endTime"] = dateRange[1];
        } else {
            search.params["begin" + propName] = dateRange[0];
            search.params["end" + propName] = dateRange[1];
        }
    }
    return search;
}

// 回显数据字典
export function selectDictLabel(datas, value) {
    var actions = [];
    Object.keys(datas).some((key) => {
        if (datas[key].dictValue == ('' + value)) {
            actions.push(datas[key].dictLabel);
            return true;
        }
    })
    return actions.join('');
}

// 回显数据字典（字符串数组）
export function selectDictLabels(datas, value, separator) {
    var actions = [];
    var currentSeparator = undefined === separator ? "," : separator;
    var temp = value.split(currentSeparator);
    Object.keys(value.split(currentSeparator)).some((val) => {
        Object.keys(datas).some((key) => {
            if (datas[key].dictValue == ('' + temp[val])) {
                actions.push(datas[key].dictLabel + currentSeparator);
            }
        })
    })
    return actions.join('').substring(0, actions.join('').length - 1);
}

// 通用下载方法
export function download(fileName) {
    window.location.href = baseURL + "/common/download?fileName=" + encodeURI(fileName) + "&delete=" + true;
}

// 字符串格式化(%s )
export function sprintf(str) {
    var args = arguments, flag = true, i = 1;
    str = str.replace(/%s/g, function () {
        var arg = args[i++];
        if (typeof arg === 'undefined') {
            flag = false;
            return '';
        }
        return arg;
    });
    return flag ? str : '';
}

// 转换字符串，undefined,null等转化为""
export function praseStrEmpty(str) {
    if (!str || str == "undefined" || str == "null") {
        return "";
    }
    return str;
}

/**
 * 构造树型结构数据
 * @param {*} data 数据源
 * @param {*} id id字段 默认 'id'
 * @param {*} parentId 父节点字段 默认 'parentId'
 * @param {*} children 孩子节点字段 默认 'children'
 * @param {*} rootId 根Id 默认 0
 */
export function handleTree(data, id, parentId, children, rootId) {
    id = id || 'id'
    parentId = parentId || 'parentId'
    children = children || 'children'
    rootId = rootId || Math.min.apply(Math, data.map(item => {
        return item[parentId]
    })) || 0
    //对源数据深度克隆
    const cloneData = JSON.parse(JSON.stringify(data))
    //使用 Map 构建 O(n) 树
    const map = new Map()
    for (const item of cloneData) {
        item[children] = []
        map.set(item[id], item)
    }
    const treeData = []
    for (const item of cloneData) {
        const parent = map.get(item[parentId])
        if (parent) {
            parent[children].push(item)
        }
        if (item[parentId] === rootId) {
            treeData.push(item)
        }
    }
    return treeData.length > 0 ? treeData : data;
}

/**
 * 滚动动画
 * @param currentY  开始位置
 * @param targetY   结束位置
 */
export function scrollAnimation(currentY, targetY) {
    // 获取当前位置方法
    // const currentY = document.documentElement.scrollTop || document.body.scrollTop
    // 计算需要移动的距离
    let needScrollTop = targetY - currentY;
    let _currentY = currentY;
    setTimeout(() => {
        // 一次调用滑动帧数，每次调用会不一样
        const dist = Math.ceil(needScrollTop / 10);
        _currentY += dist;
        window.scrollTo(_currentY, currentY);
        // 如果移动幅度小于十个像素，直接移动，否则递归调用，实现动画效果
        if (needScrollTop > 10 || needScrollTop < -10) {
            scrollAnimation(_currentY, targetY)
        } else {
            window.scrollTo(_currentY, targetY)
        }
    }, 10)
};

/**
 * blob转base64
 * @param blob  blob对象
 * @returns {Promise<unknown>}
 */
export function blobToBase64(blob) {
    return new Promise((resolve, reject) => {
        const fileReader = new FileReader();
        fileReader.onload = (e) => {
            resolve(e.target.result);
        };
        // readAsDataURL
        fileReader.readAsDataURL(blob);
        fileReader.onerror = () => {
            this.$message.error("图片上传出错，请稍后再试")
        };
    });
};

/**
 * 翻译字典项
 * @param value 字典键值
 * @param list  字典列表
 * @returns {string}
 */
export function translationDic(value, list) {
    let label = '';
    list.forEach(item => {
        if (item.dictValue === value) {
            label = item.dictLabel;
        }
    });
    return label
};

/**
 * 选择字典项
 * @param value 字典键值
 * @param list  字典列表
 * @returns {string}
 */
export function choiceDic(value, list) {
    let res = {};
    list.forEach(item => {
        if (item.dictValue === value) {
            res = item;
        }
    });
    return res
};

/**
 * 防抖
 * @param fn    函数
 * @param delay  等待时间
 * @returns {function(...[*]=)}
 */
export function debounce(fn, delay = 1000){
    let timer = null;
    return function (...args) {
        if(timer){
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
export function throttle(fn, delay){
    let valid = true;
    return function (...args) {
        if(!valid){
            return false
        }
        valid = false
        setTimeout(() => {
            fn.apply(this, args)
            valid = true;
        }, delay)
    }
}

/**
 * 向下的箭头
 * @returns {function(...[*]=)}
 */
export function downArrow(dom) {
    let start = document.documentElement.scrollTop;
    let end = document.getElementsByClassName(dom)[0].offsetTop;
    scrollAnimation(start, end)
}

/**
 * 移到留言位置
 */
export function goToMessagePosition(event) {
    const start = event.target.offsetTop
    const end = document.getElementById("messageBoard").offsetTop + document.getElementById("messageBoard").offsetHeight;
    scrollAnimation(start, end - 100)
}

/**
 * 复制到粘贴板
 */
export function copyText(text) {
    const input = document.createElement('input');
    document.body.appendChild(input);
    input.setAttribute('value', text);
    input.select();
    if (document.execCommand('copy')) {
        document.execCommand('copy');
        ElNotification({type: "success", message: '复制分享链接成功。'});
    }
    document.body.removeChild(input);
}
