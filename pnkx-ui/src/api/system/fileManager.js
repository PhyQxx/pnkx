import request from '@/utils/request'

export function listFiles(path) {
  return request({
    url: '/system/file-manager/list',
    method: 'get',
    params: { path }
  })
}

export function readFile(path) {
  return request({
    url: '/system/file-manager/read',
    method: 'get',
    params: { path }
  })
}

export function writeFile(data) {
  return request({
    url: '/system/file-manager/write',
    method: 'put',
    data
  })
}

export function createFile(data) {
  return request({
    url: '/system/file-manager/create',
    method: 'post',
    data
  })
}

export function deleteFile(path) {
  return request({
    url: '/system/file-manager/',
    method: 'delete',
    params: { path }
  })
}

export function moveFile(data) {
  return request({
    url: '/system/file-manager/move',
    method: 'put',
    data
  })
}

export function mkdirFile(data) {
  return request({
    url: '/system/file-manager/mkdir',
    method: 'post',
    data
  })
}

export function searchFiles(q) {
  return request({
    url: '/system/file-manager/search',
    method: 'get',
    params: { q }
  })
}
