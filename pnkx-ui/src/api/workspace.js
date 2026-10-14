import request from '@/utils/request'
import { getToken } from '@/utils/auth'
export const listResources = params => request({ url: '/workspace/resources', method: 'get', params: { ...params, readFlag: params.type === 'webArticle' && typeof params.readFlag === 'boolean' ? params.readFlag : undefined, starFlag: params.type === 'webArticle' && typeof params.starFlag === 'boolean' ? params.starFlag : undefined, tagIds: params.tagIds?.join(',') || undefined } })
export const listTags = () => request({ url: '/workspace/tags', method: 'get' })
export const saveTag = data => request({ url: '/workspace/tags', method: 'post', data })
export const deleteTag = id => request({ url: '/workspace/tags/' + id, method: 'delete' })
export const mergeTags = (id, targetId) => request({ url: '/workspace/tags/' + id + '/merge', method: 'post', data: { targetId } })
export const getResourceTags = row => request({ url: `/workspace/resources/${row.type}/${row.id}/tags`, method: 'get' })
export const setResourceTags = (row, tagIds) => request({ url: `/workspace/resources/${row.type}/${row.id}/tags`, method: 'put', data: { tagIds } })
export const batchTags = (resources, tagIds) => request({ url: '/workspace/resources/tags/batch', method: 'post', data: { resources: resources.map(({ type, id }) => ({ type, id })), tagIds } })
export const backfillTags = type => request({ url: '/workspace/tags/backfill/' + type, method: 'post' })
export const getResource = row => request({ url: `/workspace/resources/${row.type}/${row.id}`, method: 'get' })
export const getLinks = row => request({ url: `/workspace/resources/${row.type}/${row.id}/links`, method: 'get' })
export const getBacklinks = row => request({ url: `/workspace/resources/${row.type}/${row.id}/backlinks`, method: 'get' })
export const addLink = (row, target) => request({ url: `/workspace/resources/${row.type}/${row.id}/links`, method: 'post', data: { type: target.type, id: target.id } })
export const removeLink = (row, link) => request({ url: `/workspace/resources/${row.type}/${row.id}/links/${link.targetType}/${link.targetId}`, method: 'delete' })
export const visitResource = row => request({ url: `/workspace/resources/${row.type}/${row.id}/visit`, method: 'post' })
export const setInbox = (row, value) => request({ url: `/workspace/resources/${row.type}/${row.id}/inbox`, method: 'put', params: { value } })
export const convertArticle = (id, data) => request({ url: `/workspace/webArticles/${id}/convert`, method: 'post', data })

export const noteHistory = id => request({ url: `/workspace/notes/${id}/history`, method: 'get' })
export const noteVersion = (id, version) => request({ url: `/workspace/notes/${id}/history/${version}`, method: 'get' })
export const restoreVersion = (id, version, revision) => request({ url: `/workspace/notes/${id}/history/${version}/restore`, method: 'post', data: { revision } })
export const listTrash = params => request({ url: '/workspace/trash', method: 'get', params })
export const restoreTrash = row => request({ url: `/workspace/trash/${row.type}/${row.id}/restore`, method: 'post' })

export const scanOrganize = (type, after = 0) => request({ url: `/workspace/organize/scan/${type}`, method: 'post', params: { after } })
export const listOrganize = (after = 0) => request({ url: '/workspace/organize', method: 'get', params: { after } })
export const applyOrganize = (id, ignore = false) => request({ url: `/workspace/organize/${id}/apply`, method: 'post', params: { ignore } })
export const auditTags = (type, after = 0) => request({ url: `/workspace/tags/audit/${type}`, method: 'get', params: { after } })

export const listSeries = () => request({ url: '/workspace/todo-series', method: 'get' })
export const saveSeries = (data, scope = 'future') => request({ url: '/workspace/todo-series', method: 'post', data, params: { scope } })

export const listFiles = (parent = 0, after = 0) => request({ url: '/workspace/files', method: 'get', params: { parent, after } })
export const listFileFolders = () => request({ url: '/workspace/files/folders', method: 'get' })
export const getFile = id => request({ url: `/workspace/files/${id}`, method: 'get' })
export const createFolder = (parentId, displayName) => request({ url: '/workspace/files/folders', method: 'post', data: { parentId, displayName } })
export const editFile = (id, data) => request({ url: `/workspace/files/${id}`, method: 'put', data })
export const deleteFile = row => request({ url: `/workspace/files/${row.id}`, method: 'delete', params: { revision: row.revision } })

export const uploadPrivateFile = (parent, file) => { const data = new FormData(); data.append('file',file); return request({ url: '/workspace/files/upload', method: 'post', params: { parent }, data, timeout: 120000, headers: { 'Content-Type': 'multipart/form-data' } }) }
export const fileContent = (id, preview = false) => request({ url: `/workspace/files/${id}/content`, method: 'get', params: { preview }, responseType: 'blob' })

export const batchInbox = (resources, value) => request({ url: '/workspace/resources/inbox/batch', method: 'post', data: { resources: resources.map(({ type,id }) => ({ type,id })), value } })

const qaRequest = async options => {
  const token = getToken()
  const result = await request(options)
  if (getToken() !== token) throw new Error('账号已变化，请重新打开问答')
  return result
}
export const askWorkspace = data => qaRequest({ url: '/workspace/qa', method: 'post', data, timeout: 130000, offline: false })
export const qaHistory = () => qaRequest({ url: '/workspace/qa', method: 'get', offline: false })
export const qaDetail = id => qaRequest({ url: '/workspace/qa/' + id, method: 'get', offline: false })
export const cancelQa = id => qaRequest({ url: '/workspace/qa/' + id + '/cancel', method: 'post', offline: false })
export const applyQa = (id, data) => qaRequest({ url: '/workspace/qa/' + id + '/apply', method: 'post', data, offline: false })
