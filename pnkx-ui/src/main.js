import { computed, createApp, defineAsyncComponent } from 'vue'
import Cookies from 'js-cookie'
// Element Plus 按需引入：命名导出可被 tree-shaking，仅打包项目实际使用的组件
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import {
    ElAlert,
    ElAvatar,
    ElBadge,
    ElBreadcrumb,
    ElBreadcrumbItem,
    ElButton,
    ElButtonGroup,
    ElCalendar,
    ElCard,
    ElCascader,
    ElCascaderPanel,
    ElCheckbox,
    ElCheckboxGroup,
    ElCol,
    ElCollapse,
    ElCollapseItem,
    ElColorPicker,
    ElDatePicker,
    ElDescriptions,
    ElDescriptionsItem,
    ElDialog,
    ElDivider,
    ElDrawer,
    ElDropdown,
    ElDropdownItem,
    ElDropdownMenu,
    ElEmpty,
    ElForm,
    ElFormItem,
    ElIcon,
    ElImage,
    ElInput,
    ElInputNumber,
    ElLink,
    ElMenu,
    ElMenuItem,
    ElOption,
    ElOptionGroup,
    ElPageHeader,
    ElPagination,
    ElPopover,
    ElProgress,
    ElRadio,
    ElRadioButton,
    ElRadioGroup,
    ElRate,
    ElRow,
    ElScrollbar,
    ElSelect,
    ElSlider,
    ElSubMenu,
    ElSwitch,
    ElTabPane,
    ElTable,
    ElTableColumn,
    ElTabs,
    ElTag,
    ElTimePicker,
    ElTimeline,
    ElTimelineItem,
    ElTooltip,
    ElTransfer,
    ElTree,
    ElTreeSelect,
    ElUpload,
    ElLoadingDirective, ElInfiniteScroll, ElMessage, ElMessageBox, ElNotification, configProviderContextKey, localeContextKey
} from 'element-plus'
// 按组件引入样式（各条目自动携带其依赖与主题基础样式）
import 'element-plus/es/components/alert/style/css'
import 'element-plus/es/components/avatar/style/css'
import 'element-plus/es/components/badge/style/css'
import 'element-plus/es/components/breadcrumb/style/css'
import 'element-plus/es/components/breadcrumb-item/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/button-group/style/css'
import 'element-plus/es/components/calendar/style/css'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/cascader/style/css'
import 'element-plus/es/components/cascader-panel/style/css'
import 'element-plus/es/components/checkbox/style/css'
import 'element-plus/es/components/checkbox-group/style/css'
import 'element-plus/es/components/col/style/css'
import 'element-plus/es/components/collapse/style/css'
import 'element-plus/es/components/collapse-item/style/css'
import 'element-plus/es/components/color-picker/style/css'
import 'element-plus/es/components/date-picker/style/css'
import 'element-plus/es/components/descriptions/style/css'
import 'element-plus/es/components/descriptions-item/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/divider/style/css'
import 'element-plus/es/components/drawer/style/css'
import 'element-plus/es/components/dropdown/style/css'
import 'element-plus/es/components/dropdown-item/style/css'
import 'element-plus/es/components/dropdown-menu/style/css'
import 'element-plus/es/components/empty/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/form-item/style/css'
import 'element-plus/es/components/icon/style/css'
import 'element-plus/es/components/image/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/input-number/style/css'
import 'element-plus/es/components/link/style/css'
import 'element-plus/es/components/menu/style/css'
import 'element-plus/es/components/menu-item/style/css'
import 'element-plus/es/components/option/style/css'
import 'element-plus/es/components/option-group/style/css'
import 'element-plus/es/components/page-header/style/css'
import 'element-plus/es/components/pagination/style/css'
import 'element-plus/es/components/popover/style/css'
import 'element-plus/es/components/progress/style/css'
import 'element-plus/es/components/radio/style/css'
import 'element-plus/es/components/radio-button/style/css'
import 'element-plus/es/components/radio-group/style/css'
import 'element-plus/es/components/rate/style/css'
import 'element-plus/es/components/row/style/css'
import 'element-plus/es/components/scrollbar/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/slider/style/css'
import 'element-plus/es/components/sub-menu/style/css'
import 'element-plus/es/components/switch/style/css'
import 'element-plus/es/components/tab-pane/style/css'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'
import 'element-plus/es/components/tabs/style/css'
import 'element-plus/es/components/tag/style/css'
import 'element-plus/es/components/time-picker/style/css'
import 'element-plus/es/components/timeline/style/css'
import 'element-plus/es/components/timeline-item/style/css'
import 'element-plus/es/components/tooltip/style/css'
import 'element-plus/es/components/transfer/style/css'
import 'element-plus/es/components/tree/style/css'
import 'element-plus/es/components/tree-select/style/css'
import 'element-plus/es/components/upload/style/css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { registerLegacyElementIconAliases, startLegacyElementIconObserver } from '@/utils/element-icon-compat'
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
import Pagination from '@/components/Pagination/index.vue'
import ToMessage from '@/components/ToMessage/index.vue'
import { compressImage } from '@/utils/compressImage'
import { getUserProfile } from '@/api/system/user'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import RightToolbar from '@/components/RightToolbar/index.vue'
import noData from '@/components/NoData/index.vue'
import ModelImage from '@/components/ModelImage/index.vue'
import AdminMessageBoard from '@/components/MessageBoard/admin.vue'
import * as filters from '@/utils/filters'
import TencentCaptcha from '@/assets/js/TencentCaptcha'

// 重型编辑器/裁剪组件按需异步加载，避免打入首屏入口 chunk
const XMarkDown = defineAsyncComponent(() => import('@/components/XMarkDown/index.vue'))
const CherryMarkdownEditor = defineAsyncComponent(() => import('@/components/CherryMarkdownEditor/index.vue'))
const EditPicture = defineAsyncComponent(() => import('@/components/EditPicture/index.vue'))
const Editor = defineAsyncComponent(() => import('@/components/Editor/index.vue'))

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

// 插件注册（Element Plus 按需版）
const elementComponents = [
    ElAlert,
    ElAvatar,
    ElBadge,
    ElBreadcrumb,
    ElBreadcrumbItem,
    ElButton,
    ElButtonGroup,
    ElCalendar,
    ElCard,
    ElCascader,
    ElCascaderPanel,
    ElCheckbox,
    ElCheckboxGroup,
    ElCol,
    ElCollapse,
    ElCollapseItem,
    ElColorPicker,
    ElDatePicker,
    ElDescriptions,
    ElDescriptionsItem,
    ElDialog,
    ElDivider,
    ElDrawer,
    ElDropdown,
    ElDropdownItem,
    ElDropdownMenu,
    ElEmpty,
    ElForm,
    ElFormItem,
    ElIcon,
    ElImage,
    ElInput,
    ElInputNumber,
    ElLink,
    ElMenu,
    ElMenuItem,
    ElOption,
    ElOptionGroup,
    ElPageHeader,
    ElPagination,
    ElPopover,
    ElProgress,
    ElRadio,
    ElRadioButton,
    ElRadioGroup,
    ElRate,
    ElRow,
    ElScrollbar,
    ElSelect,
    ElSlider,
    ElSubMenu,
    ElSwitch,
    ElTabPane,
    ElTable,
    ElTableColumn,
    ElTabs,
    ElTag,
    ElTimePicker,
    ElTimeline,
    ElTimelineItem,
    ElTooltip,
    ElTransfer,
    ElTree,
    ElTreeSelect,
    ElUpload
]
elementComponents.forEach(component => app.component(component.name, component))
app.directive('loading', ElLoadingDirective)
app.directive('infinite-scroll', ElInfiniteScroll)
// 全局 locale/size：等价于原 app.use(ElementPlus, { size, locale })，
// localeContextKey 供 Message/MessageBox 等命令式 API 的语言回退
app.provide(configProviderContextKey, computed(() => ({ locale: zhCn, size: Cookies.get('size') || 'default' })))
app.provide(localeContextKey, computed(() => zhCn))
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
