import request from '@/utils/request'

// 列出目录下的文件
export function listFiles(path) {
  return request({
    url: '/system/file-manager/list',
    method: 'get',
    params: { path }
  })
}

// 读取文件内容
export function readFile(path) {
  return request({
    url: '/system/file-manager/read',
    method: 'get',
    params: { path }
  })
}

// 保存文件内容
export function writeFile(data) {
  return request({
    url: '/system/file-manager/write',
    method: 'put',
    data
  })
}

// 新建文件
export function createFile(data) {
  return request({
    url: '/system/file-manager/create',
    method: 'post',
    data
  })
}

// 新建目录
export function mkdirFile(data) {
  return request({
    url: '/system/file-manager/mkdir',
    method: 'post',
    data
  })
}

// 删除文件
export function deleteFile(path) {
  return request({
    url: '/system/file-manager/',
    method: 'delete',
    params: { path }
  })
}

// 搜索文件
export function searchFiles(q) {
  return request({
    url: '/system/file-manager/search',
    method: 'get',
    params: { q }
  })
}
