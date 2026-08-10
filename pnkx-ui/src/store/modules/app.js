import Cookies from 'js-cookie'
import { getTheme, setTheme } from '@/utils/auth'

const state = {
    sidebar: {
        opened: Cookies.get('sidebarStatus') ? !!+Cookies.get('sidebarStatus') : true,
        withoutAnimation: false
    },
    device: 'desktop',
    size: Cookies.get('size') || 'medium',
    theme: getTheme() || 'light',
    // 当前选中的一级菜单路径
    activeTopMenuPath: ''
}

const mutations = {
    TOGGLE_SIDEBAR: state => {
        state.sidebar.opened = !state.sidebar.opened
        state.sidebar.withoutAnimation = false
        if (state.sidebar.opened) {
            Cookies.set('sidebarStatus', 1)
        } else {
            Cookies.set('sidebarStatus', 0)
        }
    },
    CLOSE_SIDEBAR: (state, withoutAnimation) => {
        Cookies.set('sidebarStatus', 0)
        state.sidebar.opened = false
        state.sidebar.withoutAnimation = withoutAnimation
    },
    TOGGLE_DEVICE: (state, device) => {
        state.device = device
    },
    SET_SIZE: (state, size) => {
        state.size = size
        Cookies.set('size', size)
    },
    SET_THEME: (state, theme) => {
        state.theme = theme
        setTheme(theme)
        // 应用主题到 HTML 元素
        document.documentElement.setAttribute('data-theme', theme)
        // 添加过渡类
        document.documentElement.classList.add('theme-transitioning')
        setTimeout(() => {
            document.documentElement.classList.remove('theme-transitioning')
        }, 300)
    },
    // 设置当前一级菜单
    SET_TOP_MENU: (state, path) => {
        state.activeTopMenuPath = path
    }
}

const actions = {
    toggleSideBar({commit}) {
        commit('TOGGLE_SIDEBAR')
    },
    closeSideBar({commit}, {withoutAnimation}) {
        commit('CLOSE_SIDEBAR', withoutAnimation)
    },
    toggleDevice({commit}, device) {
        commit('TOGGLE_DEVICE', device)
    },
    setSize({commit}, size) {
        commit('SET_SIZE', size)
    },
    toggleTheme({commit, state}) {
        const newTheme = state.theme === 'light' ? 'dark' : 'light'
        commit('SET_THEME', newTheme)
    },
    initTheme({commit, state}) {
        // 初始化时应用主题
        document.documentElement.setAttribute('data-theme', state.theme)
    },
    // 切换一级菜单
    switchTopMenu({commit}, path) {
        commit('SET_TOP_MENU', path)
    }
}

export default {
    namespaced: true,
    state,
    mutations,
    actions
}
