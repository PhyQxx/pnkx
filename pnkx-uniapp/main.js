import { createSSRApp } from 'vue'
import App from './App.vue'
import store from './store'
import plugins from './plugins'
import './permission'
import moment from 'moment'
import { getDicts } from "@/api/system/dict/data"
import * as filters from '@/utils/filters'
import SvgIcon from '@/components/SvgIcon/index.vue'
import NoData from '@/components/NoData/index.vue'
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

// uni-ui 组件全局注册（防止正式包 tree-shaking 剔除）
import uniDatetimePicker from '@/uni_modules/uni-datetime-picker/components/uni-datetime-picker/uni-datetime-picker.vue'
import uniDataCheckbox from '@/uni_modules/uni-data-checkbox/components/uni-data-checkbox/uni-data-checkbox.vue'
import uniDataSelect from '@/uni_modules/uni-data-select/components/uni-data-select/uni-data-select.vue'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import uniForms from '@/uni_modules/uni-forms/components/uni-forms/uni-forms.vue'
import uniFormsItem from '@/uni_modules/uni-forms/components/uni-forms-item/uni-forms-item.vue'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'
import uniPopupDialog from '@/uni_modules/uni-popup/components/uni-popup-dialog/uni-popup-dialog.vue'
import uniPopupMessage from '@/uni_modules/uni-popup/components/uni-popup-message/uni-popup-message.vue'
import uniPopupShare from '@/uni_modules/uni-popup/components/uni-popup-share/uni-popup-share.vue'
import uniTransition from '@/uni_modules/uni-transition/components/uni-transition/uni-transition.vue'
import uniSearchBar from '@/uni_modules/uni-search-bar/components/uni-search-bar/uni-search-bar.vue'
import uniCalendar from '@/uni_modules/uni-calendar/components/uni-calendar/uni-calendar.vue'
import uniList from '@/uni_modules/uni-list/components/uni-list/uni-list.vue'
import uniListItem from '@/uni_modules/uni-list/components/uni-list-item/uni-list-item.vue'
import uniSection from '@/uni_modules/uni-section/components/uni-section/uni-section.vue'
import uniCard from '@/uni_modules/uni-card/components/uni-card/uni-card.vue'
import uniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import uniRate from '@/uni_modules/uni-rate/components/uni-rate/uni-rate.vue'
import uniLoadMore from '@/uni_modules/uni-load-more/components/uni-load-more/uni-load-more.vue'
import uniSwipeAction from '@/uni_modules/uni-swipe-action/components/uni-swipe-action/uni-swipe-action.vue'
import uniSwipeActionItem from '@/uni_modules/uni-swipe-action/components/uni-swipe-action-item/uni-swipe-action-item.vue'
import uniGrid from '@/uni_modules/uni-grid/components/uni-grid/uni-grid.vue'
import uniGridItem from '@/uni_modules/uni-grid/components/uni-grid-item/uni-grid-item.vue'
import uniLink from '@/uni_modules/uni-link/components/uni-link/uni-link.vue'
import uniCollapse from '@/uni_modules/uni-collapse/components/uni-collapse/uni-collapse.vue'
import uniCollapseItem from '@/uni_modules/uni-collapse/components/uni-collapse-item/uni-collapse-item.vue'
import uniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'

moment.locale('zh-cn')

export function createApp() {
  const app = createSSRApp(App)

  app.use(store)
  app.use(plugins)

  // 全局属性
  app.config.globalProperties.$moment = moment
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

  // 全局组件
  app.component("SvgIcon", SvgIcon)
  app.component("NoData", NoData)
  app.component("uni-datetime-picker", uniDatetimePicker)
  app.component("uni-data-checkbox", uniDataCheckbox)
  app.component("uni-data-select", uniDataSelect)
  app.component("uni-icons", uniIcons)
  app.component("uni-easyinput", uniEasyinput)
  app.component("uni-forms", uniForms)
  app.component("uni-forms-item", uniFormsItem)
  app.component("uni-popup", uniPopup)
  app.component("uni-popup-dialog", uniPopupDialog)
  app.component("uni-popup-message", uniPopupMessage)
  app.component("uni-popup-share", uniPopupShare)
  app.component("uni-transition", uniTransition)
  app.component("uni-search-bar", uniSearchBar)
  app.component("uni-calendar", uniCalendar)
  app.component("uni-list", uniList)
  app.component("uni-list-item", uniListItem)
  app.component("uni-section", uniSection)
  app.component("uni-card", uniCard)
  app.component("uni-tag", uniTag)
  app.component("uni-rate", uniRate)
  app.component("uni-load-more", uniLoadMore)
  app.component("uni-swipe-action", uniSwipeAction)
  app.component("uni-swipe-action-item", uniSwipeActionItem)
  app.component("uni-grid", uniGrid)
  app.component("uni-grid-item", uniGridItem)
  app.component("uni-link", uniLink)
  app.component("uni-collapse", uniCollapse)
  app.component("uni-collapse-item", uniCollapseItem)
  app.component("uni-number-box", uniNumberBox)

  return {
    app
  }
}
