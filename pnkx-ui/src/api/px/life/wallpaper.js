import request from '@/utils/request'

// 查询壁纸文件夹列表
export function listWallpaperFolder(query) {
    return request({
        url: '/wallpaper/folder/list',
        method: 'get',
        params: query
    })
}

// 查询壁纸文件夹分页列表（文件夹管理页表格用，轻量查询无 CTE）
export function listWallpaperFolderPage(query) {
    return request({
        url: '/wallpaper/folder/pageList',
        method: 'get',
        params: query
    })
}

// 查询壁纸文件夹树形列表（含父子层级，供下拉选择）
export function treeWallpaperFolder(query) {
    return request({
        url: '/wallpaper/folder/list',
        method: 'get',
        params: query
    })
}

// 查询壁纸文件夹详细
export function getWallpaperFolder(id) {
    return request({
        url: '/wallpaper/folder/' + id,
        method: 'get'
    })
}

// 新增壁纸文件夹
export function addWallpaperFolder(data) {
    return request({
        url: '/wallpaper/folder',
        method: 'post',
        data: data
    })
}

// 修改壁纸文件夹
export function updateWallpaperFolder(data) {
    return request({
        url: '/wallpaper/folder',
        method: 'put',
        data: data
    })
}

// 删除壁纸文件夹
export function delWallpaperFolder(id) {
    return request({
        url: '/wallpaper/folder/' + id,
        method: 'delete'
    })
}

// 导出壁纸文件夹列表
export function exportWallpaperFolder(query) {
    return request({
        url: '/wallpaper/folder/export',
        method: 'get',
        params: query
    })
}

// 查询壁纸列表
export function listWallpaper(query) {
    return request({
        url: '/wallpaper/list',
        method: 'get',
        params: query
    })
}

// 查询壁纸详细
export function getWallpaper(id) {
    return request({
        url: '/wallpaper/' + id,
        method: 'get'
    })
}

// 新增壁纸
export function addWallpaper(data) {
    return request({
        url: '/wallpaper',
        method: 'post',
        data: data
    })
}

// 修改壁纸
export function updateWallpaper(data) {
    return request({
        url: '/wallpaper',
        method: 'put',
        data: data
    })
}

// 删除壁纸
export function delWallpaper(id) {
    return request({
        url: '/wallpaper/' + id,
        method: 'delete'
    })
}

// 导出壁纸列表
export function exportWallpaper(query) {
    return request({
        url: '/wallpaper/export',
        method: 'get',
        params: query
    })
}

// 管理端：查询所有点赞记录（分页，支持按用户筛选）
export function listAllLikes(query) {
    return request({
        url: '/wallpaper/records/likes',
        method: 'get',
        params: query
    })
}

// 管理端：查询所有下载记录（分页，支持按用户筛选）
export function listAllDownloads(query) {
    return request({
        url: '/wallpaper/records/downloads',
        method: 'get',
        params: query
    })
}

// 管理端：操作记录中出现过的用户（用于用户筛选下拉）
export function listRecordUsers(type) {
    return request({
        url: '/wallpaper/records/users',
        method: 'get',
        params: {type}
    })
}

// 管理端：操作记录统计（按日期趋势）
export function statsRecordsByDate(query) {
    return request({
        url: '/wallpaper/records/statsByDate',
        method: 'get',
        params: query
    })
}

// 管理端：操作记录统计（按文件夹分布）
export function statsRecordsByFolder() {
    return request({
        url: '/wallpaper/records/statsByFolder',
        method: 'get'
    })
}

// 管理端：下载记录按用户统计（用户汇总）
export function downloadStatsByUser(query) {
    return request({
        url: '/wallpaper/records/downloadStatsByUser',
        method: 'get',
        params: query
    })
}

// 管理端：下载记录按用户+日期统计明细
export function downloadStatsByUserDate(query) {
    return request({
        url: '/wallpaper/records/downloadStatsByUserDate',
        method: 'get',
        params: query
    })
}
