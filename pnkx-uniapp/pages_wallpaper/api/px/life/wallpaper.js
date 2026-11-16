import request from '@/utils/request'
import config from '@/config'

// 查询壁纸列表（免登录）
export function listWallpaper(query) {
  return request({
    url: '/client/wallpaper/list',
    method: 'get',
    params: query
  })
}

/**
 * 构造壁纸单张代理下载地址
 * H5 直接下载跨域图片会触发 CORS，统一走后端代理
 */
export function getWallpaperDownloadUrl(id) {
  return config.baseUrl + '/client/wallpaper/download/' + id
}

/**
 * 构造壁纸 ZIP 打包下载地址
 * @param {Object} options
 * @param {Array<number>} [options.ids]     壁纸 id 列表（与 folderId 二选一）
 * @param {number}        [options.folderId] 文件夹 id（打包整个文件夹含子文件夹）
 * @param {string}        [options.name]     zip 文件名（不含扩展名）
 */
export function getWallpaperZipUrl({ ids, folderId, name } = {}) {
  const params = []
  if (folderId != null) {
    params.push('folderId=' + encodeURIComponent(folderId))
  } else if (ids && ids.length) {
    params.push('ids=' + encodeURIComponent(ids.join(',')))
  }
  if (name) {
    params.push('name=' + encodeURIComponent(name))
  }
  return config.baseUrl + '/client/wallpaper/download/zip' + (params.length ? '?' + params.join('&') : '')
}

// 点赞壁纸（需登录，切换态）
export function likeWallpaper(id) {
  return request({
    url: '/client/wallpaper/like/' + id,
    method: 'get'
  })
}

// 查询今日下载状态（剩余次数、限额、分享奖励、提醒文案）
export function getDownloadStatus() {
  return request({
    url: '/client/wallpaper/downloadStatus',
    method: 'get'
  })
}

// 分享小程序奖励下载次数（需登录，分享成功回调中调用）
export function shareReward() {
  return request({
    url: '/client/wallpaper/shareReward',
    method: 'post'
  })
}

// 我的点赞记录（分页）
export function listMyLikes(query) {
  return request({
    url: '/client/wallpaper/myLikes',
    method: 'get',
    params: query
  })
}

// 我的下载记录（分页）
export function listMyDownloads(query) {
  return request({
    url: '/client/wallpaper/myDownloads',
    method: 'get',
    params: query
  })
}

// 查询壁纸详细（免登录）
export function getWallpaper(id) {
  return request({
    url: '/client/wallpaper/' + id,
    method: 'get'
  })
}

// 查询壁纸文件夹列表（免登录）
export function listWallpaperFolder(query) {
  return request({
    url: '/client/wallpaper/folder/list',
    method: 'get',
    params: query
  })
}

// 查询壁纸文件夹详细（免登录）
export function getWallpaperFolder(id) {
  return request({
    url: '/client/wallpaper/folder/' + id,
    method: 'get'
  })
}
