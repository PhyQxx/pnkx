import request from '@/utils/request'
import upload from '@/utils/upload'

/*
 * 我的书城 - 书籍/章节接口
 * 后端控制器 @RequestMapping("/myBook")
 */

// ========== 书籍 ==========

// 查询书籍列表（分页）
export function listBook(query) {
	return request({
		url: '/myBook/list',
		method: 'get',
		params: query
	})
}

// 查询书籍详细
export function getBook(id) {
	return request({
		url: '/myBook/' + id,
		method: 'get'
	})
}

// 新增书籍
export function addBook(data) {
	return request({
		url: '/myBook',
		method: 'post',
		data: data
	})
}

// 修改书籍
export function updateBook(data) {
	return request({
		url: '/myBook',
		method: 'put',
		data: data
	})
}

// 删除书籍
export function delBook(ids) {
	return request({
		url: '/myBook/' + (Array.isArray(ids) ? ids.join(',') : ids),
		method: 'delete'
	})
}

// ========== 章节 ==========

// 查询章节列表（分页）
export function listChapter(query) {
	return request({
		url: '/myBook/chapter/list',
		method: 'get',
		params: query
	})
}

// 查询章节详细
export function getChapter(id) {
	return request({
		url: '/myBook/chapter/' + id,
		method: 'get'
	})
}

// 查询阅读器数据：返回 { chapter, previous, next }
export function getReader(id) {
	return request({
		url: '/myBook/chapter/' + id + '/reader',
		method: 'get'
	})
}

// 新增章节
export function addChapter(data) {
	return request({
		url: '/myBook/chapter',
		method: 'post',
		data: data
	})
}

// 修改章节
export function updateChapter(data) {
	return request({
		url: '/myBook/chapter',
		method: 'put',
		data: data
	})
}

// 删除章节
export function delChapter(ids) {
	return request({
		url: '/myBook/chapter/' + (Array.isArray(ids) ? ids.join(',') : ids),
		method: 'delete'
	})
}

// ========== 阅读进度 ==========

// 保存阅读进度（记录最近阅读章节）
export function saveProgress(chapterId) {
	return request({
		url: '/myBook/progress/' + chapterId,
		method: 'put'
	})
}

// ========== TXT 导入 / 导出 ==========

// 导入 TXT 为一本书（multipart 上传）
export function importTxt(filePath, formData) {
	return upload({
		url: '/myBook/txt/import',
		filePath: filePath,
		name: 'file',
		formData: formData || {}
	})
}
