import type { LoginForm, UserInfo, UserForm, UserInfoForm } from '@/types/user'
import type { HttpOption } from '@/composables/useHttp'

enum Api {
  login = '/clientLogin',
  password = '/user/password',
  register = '/register',
  activation = '/activation/',
  sendResetEmail = '/sendResetEmail/',
  restPassword = '/restPassword/',
  userInfo = '/getInfo',
  updateUserInfo = '/system/user/profile',
  updateAvatar = '/system/user/profile/avatar',
}

/**
 * 用户登录
 * @param data 登录信息
 * @param option
 * @returns Token
 */
export function login(data: LoginForm, option?: HttpOption<string>) {
  return useHttp.post<any>(Api.login, data, option)
}

/**
 * 邮箱注册
 * @param data 注册信息
 * @param option
 */
export function register(data: UserForm, option?: HttpOption<null>) {
  return useHttp.post<any>(Api.register, data, option)
}

/**
 * 激活账号
 * @returns 激活账号结果
 */
export function activation(userName: string) {
  return useHttp.get<boolean>(Api.activation + userName)
}

/**
 * 发送重置邮件
 * @returns 发送结果
 */
export function sendResetEmail(userName: string) {
  return useHttp.get<boolean>(Api.sendResetEmail + userName)
}

/**
 * 重置密码
 * @returns 发送结果
 */
export function restPassword(userName: string) {
  return useHttp.get<boolean>(Api.restPassword + userName)
}

/**
 * 修改用户密码
 * @param data 用户密码
 */
export function changePassword(data: UserForm) {
  return useHttp.put<null>(Api.password, data)
}

/**
 * 获取登录用户信息
 * @returns 登录用户信息
 */
export function getUserInfo() {
  return useHttp.get<UserInfo>(Api.userInfo)
}

/**
 * 修改用户信息
 * @param data 用户信息
 */
export function updateUserInfo(data: UserInfoForm) {
  return useHttp.put<null>(Api.updateUserInfo, data)
}

/**
 * 修改用户头像
 * @param data 头像
 */
export function updateAvatar(data: FormData) {
  return useHttp.post<string>(Api.updateAvatar, data)
}
