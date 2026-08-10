import request from '@/utils/request'
import axios from 'axios'
import { getToken } from '@/utils/auth'

export function listBooks(query) {
    return request({url: '/myBook/list', method: 'get', params: query})
}

export function getBook(id) {
    return request({url: '/myBook/' + id, method: 'get'})
}

export function addBook(data) {
    return request({url: '/myBook', method: 'post', data})
}

export function updateBook(data) {
    return request({url: '/myBook', method: 'put', data})
}

export function deleteBooks(ids) {
    return request({url: '/myBook/' + ids, method: 'delete'})
}

export function listChapters(query) {
    return request({url: '/myBook/chapter/list', method: 'get', params: query})
}

export function getChapter(id) {
    return request({url: '/myBook/chapter/' + id, method: 'get'})
}

export function getReaderData(id) {
    return request({url: '/myBook/chapter/' + id + '/reader', method: 'get'})
}

export function updateReadingProgress(chapterId) {
    return request({url: '/myBook/progress/' + chapterId, method: 'put'})
}

export function addChapter(data) {
    return request({url: '/myBook/chapter', method: 'post', data})
}

export function addChapters(data) {
    return request({url: '/myBook/chapter/batch', method: 'post', data})
}

export function updateChapter(data) {
    return request({url: '/myBook/chapter', method: 'put', data})
}

export function deleteChapters(ids) {
    return request({url: '/myBook/chapter/' + ids, method: 'delete'})
}

export function previewBookTxt(file) {
    const data = new FormData()
    data.append('file', file)
    return request({
        url: '/myBook/txt/preview',
        method: 'post',
        data,
        headers: {'Content-Type': 'multipart/form-data'},
        timeout: 120000
    })
}

export function importBookTxt(file, book) {
    const data = new FormData()
    data.append('file', file)
    data.append('title', book.title || '')
    data.append('author', book.author || '')
    data.append('status', book.status || 'reading')
    return request({
        url: '/myBook/txt/import',
        method: 'post',
        data,
        headers: {'Content-Type': 'multipart/form-data'},
        timeout: 120000
    })
}

export function importBooksTxt(items) {
    const data = new FormData()
    items.forEach(item => {
        data.append('files', item.file)
        data.append('titles', item.title || '')
        data.append('authors', item.author || '')
        data.append('statuses', item.status || 'reading')
    })
    return request({
        url: '/myBook/txt/import/batch',
        method: 'post',
        data,
        headers: {'Content-Type': 'multipart/form-data'},
        timeout: 300000
    })
}

export function exportBookTxt(book) {
    return axios({
        url: import.meta.env.VUE_APP_BASE_API + '/myBook/txt/export/' + book.id,
        method: 'get',
        responseType: 'blob',
        headers: {'Authorization': 'Bearer ' + getToken()},
        timeout: 120000
    }).then(response => downloadBlob(response.data, `${safeFileName(book.title || 'book')}.txt`, 'text/plain;charset=utf-8'))
}

export function exportBooksTxt(books) {
    return axios({
        url: import.meta.env.VUE_APP_BASE_API + '/myBook/txt/export/batch',
        method: 'post',
        data: books.map(book => book.id),
        responseType: 'blob',
        headers: {
            'Authorization': 'Bearer ' + getToken(),
            'Content-Type': 'application/json;charset=utf-8'
        },
        timeout: 300000
    }).then(response => {
        const stamp = new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14)
        downloadBlob(response.data, `我的书城_${stamp}.zip`, 'application/zip')
    })
}

function safeFileName(name) {
    return name.replace(/[\\/:*?"<>|]/g, '_')
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
