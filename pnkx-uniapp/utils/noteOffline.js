// Serialize note queue edits with upload claims in this app process.
let pending = Promise.resolve()
export function withNoteWriteLock(action) {
  const result = pending.then(action)
  pending = result.catch(() => {})
  return result
}

export async function writeOfflineNote(config, db, queue, uuid) {
  return withNoteWriteLock(async () => {
    const method = (config.method || 'post').toLowerCase()
    const input = { ...(config.data || {}) }
    const localId = input._localId || input._local_id
    const id = input.id || config.url.split('/').pop()
    let row
    if (method !== 'post') {
      const where = localId ? 'id = ' + db._escapeValue(localId) :
        '_server_id = ' + db._escapeValue(id)
      const rows = await db.selectSql('SELECT * FROM px_note WHERE ' + where + ' LIMIT 1')
      row = rows[0]
      if (!row) throw new Error('没有可编辑的本地笔记，请联网加载后重试')
    }
    const tasks = row ? await db.selectSql("SELECT * FROM _sync_queue WHERE table_name = 'px_note' AND status != 'synced' ORDER BY created_at") : []
    const previous = tasks.find(task => {
      const payload = JSON.parse(task.payload || '{}')
      return payload._noteLocalId === row?.id ||
        (row?._server_id && String(payload.id) === String(row._server_id)) ||
        task.id === row?.client_uuid
    })
    // A retried POST may already have committed. Never change its immutable payload.
    if (previous && (previous.status !== 'pending' || previous.retry_count > 0)) {
      throw new Error('这篇笔记有待处理的同步任务，当前草稿已保留，请先完成同步')
    }
    const operationId = previous?.id || uuid()
    const oldPayload = previous ? JSON.parse(previous.payload || '{}') : {}
    const effectiveMethod = previous?.method?.toLowerCase() || method
    if (method === 'delete' && previous) throw new Error('请先同步笔记，再执行删除')
    const payload = {
      ...input, id: row?._server_id || undefined,
      revision: previous ? oldPayload.revision : input.revision,
      clientUuid: row?.client_uuid || operationId,
      _noteLocalId: row?.id
    }
    delete payload._localId
    delete payload._local_id
    // Persist the operation first. Failed cache writes can be recovered from the queue.
    if (previous) await queue.replacePayload(operationId, payload)
    else await queue.enqueue({ id: operationId, tableName: 'px_note', method: effectiveMethod,
      url: config.url, payload, createdAt: new Date().toISOString() })
    const record = {
      ...input, revision: payload.revision,
      client_uuid: row?.client_uuid || operationId,
      _server_id: row?._server_id || null, _sync_status: 0,
      create_time: row?.create_time || new Date().toISOString(),
      update_time: new Date().toISOString()
    }
    delete record.id
    if (method === 'delete') record.del_flag = 1
    if (row) await db.update('px_note', record, { id: row.id })
    else {
      await db.insert('px_note', record)
      const rows = await db.selectSql('SELECT * FROM px_note WHERE client_uuid = ' + db._escapeValue(operationId))
      row = rows[0]
    }
    return { code: 200, msg: 'offline_queued', taskId: operationId,
      data: { ...input, revision: payload.revision, id: row?._server_id || row?.id, _localId: row?.id } }
  })
}
