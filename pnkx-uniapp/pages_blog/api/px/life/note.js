import request from '@/utils/request'

// 查询笔记列表
export function listNote(query) {
  return request({
    url: '/note/list',
    method: 'get',
    params: query
  })
}

// 查询笔记详细
export function getNote(id) {
  return request({
    url: '/note/' + id,
    method: 'get'
  })
}

// 查询笔记详细（白名单）
export function getNoteWhite(id) {
  return request({
    url: '/customer/note/' + id,
    method: 'get'
  })
}

// 新增笔记
export function addNote(data) {
  return request({
    url: '/note',
    method: 'post',
    data: data
  })
}

// 修改笔记
export function updateNote(data) {
  return request({
    url: '/note',
    method: 'put',
    data: data
  })
}

// 删除笔记
export function delNote(id) {
  return request({
    url: '/note/' + id,
    method: 'delete'
  })
}

// 导出笔记
export function exportNote(query) {
  return request({
    url: '/note/export',
    method: 'get',
    params: query
  })
}


// 查询笔记文件夹列表
export function listFolder(query) {
  return request({
    url: '/note/folder/list',
    method: 'get',
    params: query
  })
}

// 查询笔记文件夹树形列表
export function treeList(query) {
  return request({
    url: '/note/folder/treeList',
    method: 'get',
    params: query
  })
}


// 查询笔记文件夹详细
export function getFolder(id) {
  return request({
    url: '/note/folder/' + id,
    method: 'get'
  })
}

// 新增笔记文件夹
export function addFolder(data) {
  return request({
    url: '/note/folder',
    method: 'post',
    data: data
  })
}

// 修改笔记文件夹
export function updateFolder(data) {
  return request({
    url: '/note/folder',
    method: 'put',
    data: data
  })
}

// 删除笔记文件夹
export function delFolder(id) {
  return request({
    url: '/note/folder/' + id,
    method: 'delete'
  })
}
