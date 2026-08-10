import { createApp } from 'vue'
import Cookies from 'js-cookie'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { registerLegacyElementIconAliases, startLegacyElementIconObserver } from '@/utils/element-icon-compat'
import ElementPlusX from 'vue-element-plus-x'
import 'vue-element-plus-x/styles/index.css'
import '@/assets/styles/public.css'
import '@/assets/styles/font-face.css'
import './assets/styles/element-variables.scss'
import '@/assets/styles/index.scss' // global css
import '@/assets/styles/pnkx.scss' // pnkx css
import App from './App.vue'
import store from './store'
import router from './router'
import directive from './directive' // directive
import svgIcons from './assets/icons'
import 'virtual:svg-icons-register'
import './permission' // permission control
import { getDicts, listData } from '@/api/system/dict/data'
import { getConfigKey } from '@/api/system/config'
import {
    addDateRange,
    blobToBase64,
    choiceDic,
    copyText,
    debounce,
    downArrow,
    download,
    goToMessagePosition,
    handleTree,
    resetForm,
    scrollAnimation,
    selectDictLabel,
    selectDictLabels,
    throttle,
    translationDic
} from '@/utils/pnkx'
import { getSession, removeSession, setSession } from '@/utils/session'
import { getLocal, removeLocal, setLocal } from '@/utils/local'
import { dateChange, getDaysBetween, getNow, getTimeDifference, isDateInRange, parseTime } from '@/utils/time'
import { arraySum, sortAscByKey, sortDesByKey } from '@/utils/array'
import EditPicture from '@/components/EditPicture/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import ToMessage from '@/components/ToMessage/index.vue'
import { compressImage } from '@/utils/compressImage'
import { getUserProfile } from '@/api/system/user'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import '@/assets/js/prism'
import '@/assets/styles/prism.css'
import RightToolbar from '@/components/RightToolbar/index.vue'
import noData from '@/components/NoData/index.vue'
import ModelImage from '@/components/ModelImage/index.vue'
import XMarkDown from '@/components/XMarkDown/index.vue'
import CherryMarkdownEditor from '@/components/CherryMarkdownEditor/index.vue'
import AdminMessageBoard from '@/components/MessageBoard/admin.vue'
import Editor from '@/components/Editor/index.vue'
import * as filters from '@/utils/filters'
import TencentCaptcha from '@/assets/js/TencentCaptcha'
import 'video.js/dist/video-js.css'

const app = createApp(App)

// 全局方法挂载
const globalProperties = {
    $isDateInRange: isDateInRange,
    $copyText: copyText,
    $goToMessagePosition: goToMessagePosition,
    $getDaysBetween: getDaysBetween,
    $dateChange: dateChange,
    $sortDesByKey: sortDesByKey,
    $sortAscByKey: sortAscByKey,
    $moment: dayjs,
    $setSession: setSession,
    $getSession: getSession,
    $removeSession: removeSession,
    arraySum: arraySum,
    setLocal: setLocal,
    getLocal: getLocal,
    removeLocal: removeLocal,
    choiceDic: choiceDic,
    getDictList: listData,
    downArrow: downArrow,
    $throttle: throttle,
    $debounce: debounce,
    getNow: getNow,
    getUserProfile: getUserProfile,
    translationDic: translationDic,
    compressImage: compressImage,
    blobToBase64: blobToBase64,
    getTimeDifference: getTimeDifference,
    scrollAnimation: scrollAnimation,
    getDicts: getDicts,
    getConfigKey: getConfigKey,
    parseTime: parseTime,
    resetForm: resetForm,
    addDateRange: addDateRange,
    selectDictLabel: selectDictLabel,
    selectDictLabels: selectDictLabels,
    download: download,
    handleTree: handleTree,
    msgSuccess: function (msg) {
        ElNotification({ showClose: true, message: msg, type: "success" })
    },
    msgError: function (msg) {
        ElNotification({ showClose: true, message: msg, type: "error" })
    },
    msgInfo: function (msg) {
        ElNotification.info(msg)
    },
    // 若依风格的 $modal：confirm/alert/loading 等，供 this.$modal.xxx 调用
    $modal: {
        confirm(content, title = '系统提示', options = {}) {
            return ElMessageBox.confirm(content, title, {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
                ...options
            })
        },
        alert(content, title = '系统提示', options = {}) {
            return ElMessageBox.alert(content, title, {
                confirmButtonText: '确定',
                ...options
            })
        },
        msgSuccess(msg) {
            ElNotification({ showClose: true, message: msg, type: "success" })
        },
        msgError(msg) {
            ElNotification({ showClose: true, message: msg, type: "error" })
        },
        msgInfo(msg) {
            ElNotification.info(msg)
        }
    },
    // Element Plus 的 $prompt（Vue3 下不会自动挂到全局，需手动挂）
    $prompt(message, title, options) {
        return ElMessageBox.prompt(message, title, options)
    },
    // Element Plus 的 $message（Vue3 下不会自动挂到全局，需手动挂）
    $message: ElMessage
}

// 注册filters为全局属性
Object.keys(filters).forEach(key => {
    globalProperties[key] = filters[key]
})

Object.keys(globalProperties).forEach(key => {
    app.config.globalProperties[key] = globalProperties[key]
})

// 事件总线
import mitt from 'mitt'
app.config.globalProperties.$bus = mitt()

// 全局组件
app.component("ModelImage", ModelImage)
app.component("AdminMessageBoard", AdminMessageBoard)
app.component("noData", noData)
app.component("XMarkDown", XMarkDown)
app.component("CherryMarkdownEditor", CherryMarkdownEditor)
app.component("EditPicture", EditPicture)
app.component("Editor", Editor)
app.component('Pagination', Pagination)
app.component('RightToolbar', RightToolbar)
app.component('ToMessage', ToMessage)

// 插件注册
app.use(ElementPlus, {
    size: Cookies.get('size') || 'default',
    locale: zhCn
})
app.use(ElementPlusX)
app.use(store)
app.use(router)
app.use(directive)

// 全局注册 Element Plus 图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
registerLegacyElementIconAliases(app, ElementPlusIconsVue)
app.use(svgIcons)

// 百度统计
const _hmt = window._hmt || []
window._hmt = _hmt
;(function () {
    const hm = document.createElement("script")
    hm.src = "https://hm.baidu.com/hm.js?97673e147d34801825a986063a8c4138"
    const s = document.getElementsByTagName("script")[0]
    s.parentNode.insertBefore(hm, s)
})()

startLegacyElementIconObserver(ElementPlusIconsVue)
app.mount('#app')
