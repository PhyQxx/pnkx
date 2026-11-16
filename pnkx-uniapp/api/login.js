import request from '@/utils/request'

// 登录方法
export function login(userName, password, code, uuid) {
  const data = {
    userName,
    password,
    code,
    uuid
  }
  return request({
    'url': '/login',
    headers: {
      isToken: false
    },
    'method': 'post',
    'data': data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    'url': '/getInfo',
    'method': 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    'url': '/logout',
    'method': 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    'url': '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}

// 微信登录（wx.login 的 code 换 token）
export function wxLogin(code) {
  return request({
    url: '/wx/login',
    headers: { isToken: false },
    method: 'post',
    data: { code }
  })
}

// 微信手机号授权登录
export function wxPhoneLogin(loginCode, phoneCode) {
  return request({
    url: '/wx/phoneLogin',
    headers: { isToken: false },
    method: 'post',
    data: { loginCode, phoneCode }
  })
}
