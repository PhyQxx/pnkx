import { createSSRApp } from 'vue'
import App from './App.vue'
import store from './store'
import plugins from './plugins'
import './permission'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import { getDicts } from "@/api/system/dict/data"
import * as filters from '@/utils/filters'
import SvgIcon from '@/components/SvgIcon/index.vue'
import NoData from '@/components/NoData/index.vue'
import UserBadge from '@/components/UserBadge/index.vue'
import { getConfigKey } from "@/api/system/config"
import {
  arraySum,
  debounce,
  getNow,
  getTimeDifference,
  parseTime,
  sortAscByKey,
  sortDesByKey
} from './utils/pnkx.js'

// 配置 dayjs
dayjs.locale('zh-cn')
dayjs.extend(relativeTime)
dayjs.extend(customParseFormat)

export function createApp() {
  const app = createSSRApp(App)

  app.use(store)
  app.use(plugins)

  // 全局属性
  app.config.globalProperties.$dayjs = dayjs
  app.config.globalProperties.$store = store
  app.config.globalProperties.$getConfigKey = getConfigKey
  app.config.globalProperties.$parseTime = parseTime
  app.config.globalProperties.$getNow = getNow
  app.config.globalProperties.$arraySum = arraySum
  app.config.globalProperties.$getTimeDifference = getTimeDifference
  app.config.globalProperties.$sortDesByKey = sortDesByKey
  app.config.globalProperties.$sortAscByKey = sortAscByKey
  app.config.globalProperties.$debounce = debounce
  app.config.globalProperties.getDicts = getDicts

  // 注册filters为全局属性
  Object.keys(filters).forEach(key => {
    app.config.globalProperties[key] = filters[key]
  })

  // 自定义全局组件（非 uni-ui 的组件仍需手动注册）
  app.component("SvgIcon", SvgIcon)
  app.component("NoData", NoData)
  app.component("UserBadge", UserBadge)

  return {
    app
  }
}
