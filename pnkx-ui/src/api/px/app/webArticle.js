import request from '@/utils/request'
import axios from 'axios'
import { getToken } from '@/utils/auth'

export function listArticles(query) {
    return request({url: '/webArticle/list', method: 'get', params: query})
}

export function getArticle(id) {
    return request({url: '/webArticle/' + id, method: 'get'})
}

export function addArticle(data) {
    return request({url: '/webArticle', method: 'post', data})
}

export function updateArticle(data) {
    return request({url: '/webArticle', method: 'put', data})
}

export function deleteArticles(ids) {
    return request({url: '/webArticle/' + ids, method: 'delete'})
}

export function listTags() {
    return request({url: '/webArticle/tags', method: 'get'})
}

export function markRead(id, readFlag) {
    return request({url: `/webArticle/read/${id}/${readFlag}`, method: 'put'})
}

export function markStar(id, starFlag) {
    return request({url: `/webArticle/star/${id}/${starFlag}`, method: 'put'})
}

export function refetchArticle(id) {
    return request({url: '/webArticle/refetch/' + id, method: 'put'})
}

export function reanalyzeArticle(id) {
    return request({url: '/webArticle/reanalyze/' + id, method: 'put'})
}

export function listFeeds(query) {
    return request({url: '/webArticle/feed/list', method: 'get', params: query})
}

export function addFeed(data) {
    return request({url: '/webArticle/feed', method: 'post', data})
}

export function updateFeed(data) {
    return request({url: '/webArticle/feed', method: 'put', data})
}

export function deleteFeeds(ids) {
    return request({url: '/webArticle/feed/' + ids, method: 'delete'})
}

export function fetchFeedNow(id) {
    return request({url: '/webArticle/feed/fetch/' + id, method: 'put'})
}

// 运营统计（总览/分类分布/近30天趋势）
export function getStats() {
    return request({url: '/webArticle/stats', method: 'get'})
}

// 标签统计（标签名 + 文章数）
export function getTagStats() {
    return request({url: '/webArticle/tags/stats', method: 'get'})
}

// 标签重命名（新名已存在时合并）
export function renameTag(oldTag, newTag) {
    return request({url: '/webArticle/tags/rename', method: 'put', data: {oldTag, newTag}})
}

// 删除标签（从所有文章移除）
export function deleteTag(tag) {
    return request({url: '/webArticle/tags/' + encodeURIComponent(tag), method: 'delete'})
}

// 导出全部收藏为 Markdown（blob 下载）
export function exportMarkdown() {
    return axios({
        url: import.meta.env.VUE_APP_BASE_API + '/webArticle/export/markdown',
        method: 'get',
        responseType: 'blob',
        headers: {'Authorization': 'Bearer ' + getToken()},
        timeout: 120000
    }).then(response => downloadBlob(response.data, `网址收藏_${timestamp()}.md`, 'text/markdown;charset=utf-8'))
}

// 导出 RSS 订阅为 OPML（blob 下载）
export function exportOpml() {
    return axios({
        url: import.meta.env.VUE_APP_BASE_API + '/webArticle/feed/export/opml',
        method: 'get',
        responseType: 'blob',
        headers: {'Authorization': 'Bearer ' + getToken()},
        timeout: 60000
    }).then(response => downloadBlob(response.data, `pnkx_rss_${timestamp()}.opml`, 'text/xml;charset=utf-8'))
}

function timestamp() {
    return new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14)
}

function downloadBlob(data, fileName, mimeType) {
    const blob = new Blob([data], {type: mimeType})
    const link = document.createElement('a')
    const blobUrl = URL.createObjectURL(blob)
    link.href = blobUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(blobUrl)
}
