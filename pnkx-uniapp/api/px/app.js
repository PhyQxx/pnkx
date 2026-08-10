import request from '@/utils/request'

/**
 * 获取最新版app信息
 * @returns 最新版app信息
 */
export function getNewestAppInfo() {
  return request({
    url: '/open/getNewestAppInfo',
    method: 'get',
  })
}

/**
 * 获取最新版app信息
 * @returns 最新版app信息
 */
export function getAdminNewestAppInfo() {
  return request({
    url: '/app/getNewestAppInfo',
    method: 'get',
  })
}

/**
 * 检查App更新（基于sys_app_version表，支持WGT热更新）
 * @param {string} platform - 平台 (android/ios)
 * @param {number} versionCode - 当前版本编号
 * @returns 更新信息
 */
export function checkAppUpdate(platform, versionCode) {
  return request({
    url: '/open/checkUpdate',
    method: 'get',
    params: { platform, versionCode },
    header: { isToken: false }
  })
}
