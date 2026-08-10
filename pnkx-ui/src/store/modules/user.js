import {login, logout, getInfo} from '@/api/login'
import {getToken, setToken, removeToken} from '@/utils/auth'

const user = {
    state: {
        id: '',
        token: getToken(),
        name: '',
        nickName: '',
        avatar: '',
        // ip地址信息
        location: {},
        roles: [],
        permissions: []
    },

    mutations: {
        SET_ID: (state, id) => {
            state.id = id
        },
        SET_TOKEN: (state, token) => {
            state.token = token
        },
        SET_NAME: (state, name) => {
            state.name = name
        },
        SET_NICK_NAME: (state, nickName) => {
            state.nickName = nickName
        },
        SET_AVATAR: (state, avatar) => {
            state.avatar = avatar
        },
        SET_LOCATION: (state, location) => {
            state.location = location
        },
        SET_ROLES: (state, roles) => {
            state.roles = roles
        },
        SET_PERMISSIONS: (state, permissions) => {
            state.permissions = permissions
        }
    },

    actions: {
        // 登录
        Login({commit}, userInfo) {
            const userName = userInfo.userName.trim()
            const password = userInfo.password
            const code = userInfo.code
            const uuid = userInfo.uuid
            return new Promise((resolve, reject) => {
                login(userName, password, code, uuid).then(res => {
                    setToken(res.token)
                    commit('SET_TOKEN', res.token)
                    resolve()
                }).catch(error => {
                    reject(error)
                })
            })
        },

        // 获取用户信息
        GetInfo({commit, state}) {
            return new Promise((resolve, reject) => {
                getInfo(state.token).then(res => {
                    const user = res.user;
                    let header = import.meta.env.VUE_APP_BASE_API + user.avatar;
                    if (user.avatar.indexOf('http') !== -1) {
                        header = user.avatar
                    }
                    const avatar = user.avatar === "" ? 'https://ftp.pnkx.top:8/ftp/avatar/%E7%94%A8%E6%88%B7-15006732580-%E5%A4%B4%E5%83%8F-4.png' : header;
                    if (res.roles && res.roles.length > 0) { // 验证返回的roles是否是一个非空数组
                        commit('SET_ROLES', res.roles)
                        commit('SET_PERMISSIONS', res.permissions)
                    } else {
                        commit('SET_ROLES', ['ROLE_DEFAULT'])
                    }
                    commit('SET_NAME', user.userName)
                    commit('SET_NICK_NAME', user.nickName)
                    commit('SET_ID', user.userId)
                    commit('SET_AVATAR', avatar)
                    commit('SET_LOCATION', user.location)
                    resolve(res)
                }).catch(error => {
                    reject(error)
                })
            })
        },

        // 退出系统
        LogOut({commit, state}) {
            return new Promise((resolve, reject) => {
                logout(state.token).then(() => {
                    commit('SET_TOKEN', '')
                    commit('SET_ROLES', [])
                    commit('SET_PERMISSIONS', [])
                    removeToken()
                    resolve()
                }).catch(error => {
                    reject(error)
                })
            })
        },

        // 前端 登出
        FedLogOut({commit}) {
            return new Promise(resolve => {
                commit('SET_TOKEN', '')
                removeToken()
                resolve()
            })
        }
    }
}

export default user
