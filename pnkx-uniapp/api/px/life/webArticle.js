import request from '@/utils/request'

/*
 * 网址收藏 - 接口
 * 后端控制器 @RequestMapping("/webArticle")（PxWebArticleController）
 */

// 查询收藏列表（分页，支持 keyword/tag/category/readFlag/starFlag 筛选）
export function listArticle(query) {
	return request({
		url: '/webArticle/list',
		method: 'get',
		params: query
	})
}

// 查询收藏详情（含正文快照）
export function getArticle(id) {
	return request({
		url: '/webArticle/' + id,
		method: 'get'
	})
}

// 收藏网址（抓取与AI分析异步进行）
export function addArticle(data) {
	return request({
		url: '/webArticle',
		method: 'post',
		data
	})
}

// 人工修正（标题/摘要/标签/分类/备注）
export function updateArticle(data) {
	return request({
		url: '/webArticle',
		method: 'put',
		data
	})
}

// 当前用户全部标签
export function listTags() {
	return request({
		url: '/webArticle/tags',
		method: 'get'
	})
}

// ===== RSS 订阅管理 =====

// 订阅列表
export function listFeeds(query) {
	return request({
		url: '/webArticle/feed/list',
		method: 'get',
		params: query
	})
}

// 新增订阅
export function addFeed(data) {
	return request({
		url: '/webArticle/feed',
		method: 'post',
		data
	})
}

// 修改订阅（名称/启用状态等）
export function updateFeed(data) {
	return request({
		url: '/webArticle/feed',
		method: 'put',
		data
	})
}

// 删除订阅
export function deleteFeed(ids) {
	return request({
		url: '/webArticle/feed/' + ids,
		method: 'delete'
	})
}

// 立即抓取订阅
export function fetchFeedNow(id) {
	return request({
		url: '/webArticle/feed/fetch/' + id,
		method: 'put'
	})
}

// 标记已读/未读
export function markRead(id, readFlag) {
	return request({
		url: `/webArticle/read/${id}/${readFlag}`,
		method: 'put'
	})
}

// 星标/取消星标
export function markStar(id, starFlag) {
	return request({
		url: `/webArticle/star/${id}/${starFlag}`,
		method: 'put'
	})
}

// 删除收藏
export function delArticle(ids) {
	return request({
		url: '/webArticle/' + ids,
		method: 'delete'
	})
}
