import { getOfflineAccount } from '@/utils/offlineAccount'
import { withNoteWriteLock } from '@/utils/noteOffline'
/**
 * 同步调度器
 *
 * 职责：
 * - 定时轮询（30s）检查 pending 任务并同步
 * - 网络恢复 / App 回前台时立即触发同步
 * - 上行：推送 pending 任务到服务端
 * - 下行：从服务端拉取增量变更到本地
 */
import { isOnline, setOnNetworkRestore } from '@/utils/network'
// #ifdef APP-PLUS
import offlineQueue from '@/utils/offlineQueue'
import sqliteDB from '@/utils/sqliteDB'
import request from '@/utils/request'
// #endif

const SYNC_INTERVAL = 30_000   // 30秒轮询
const MAX_RETRY = 3             // 最大重试次数
const BATCH_SIZE = 20            // 每批同步任务数
const IDEMPOTENT_BATCH_TABLES = new Set([
  'px_diary', 'px_todo', 'px_bookkeeping_record', 'px_note', 'px_commemoration_day'
])

let syncTimer = null
let isSyncing = false

/** 增量同步模块配置 */
const SYNC_MODULES = [
  { table: 'px_diary', syncUrl: '/offline/sync/diary' },
  { table: 'px_todo', syncUrl: '/offline/sync/todo' },
  { table: 'px_bookkeeping_record', syncUrl: '/offline/sync/record' },
  { table: 'px_bookkeeping_classification', syncUrl: '/offline/sync/classification' },
  { table: 'px_bookkeeping_account', syncUrl: '/offline/sync/account' },
  { table: 'px_note', syncUrl: '/offline/sync/note' },
  { table: 'px_commemoration_day', syncUrl: '/offline/sync/commemorationDay' },
  { table: 'px_card', syncUrl: '/offline/sync/card' },
  { table: 'px_shopping_list', syncUrl: '/offline/sync/extended/shoppingList' },
  { table: 'px_shopping_item', syncUrl: '/offline/sync/extended/shoppingItem' },
  { table: 'px_recipe', syncUrl: '/offline/sync/extended/recipe' },
  { table: 'px_meal_plan', syncUrl: '/offline/sync/extended/mealPlan' },
  { table: 'px_subscription', syncUrl: '/offline/sync/extended/subscription' },
  { table: 'px_menstruation_record', syncUrl: '/offline/sync/extended/menstruation' },
  { table: 'px_bookkeeping_budget', syncUrl: '/offline/sync/extended/budget' },
  { table: 'px_bookkeeping_recurring', syncUrl: '/offline/sync/extended/recurring' },
  { table: 'px_book', syncUrl: '/offline/sync/extended/book' }
]

const syncScheduler = {
  /**
   * 启动定时同步
   */
  start() {
    if (syncTimer) return
    // #ifdef APP-PLUS
    syncTimer = setInterval(() => {
      if (isOnline.value && !isSyncing) {
        this.syncPendingTasks()
      }
    }, SYNC_INTERVAL)
    // 注册网络恢复回调
    setOnNetworkRestore(() => this.triggerSync('network_restore'))
    console.log('[SyncScheduler] 定时同步已启动 (间隔:', SYNC_INTERVAL / 1000, '秒)')
    // #endif
  },

  /**
   * 停止定时同步
   */
  stop() {
    if (syncTimer) {
      clearInterval(syncTimer)
      syncTimer = null
    }
  },

  /**
   * 外部触发同步（网络恢复 / App 回前台）
   */
  async triggerSync(reason = 'manual') {
    // #ifdef APP-PLUS
    if (!isOnline.value || isSyncing) return
    console.log('[SyncScheduler] 触发同步:', reason)
    await this.syncPendingTasks()
    await this.pullServerChanges()
    // #endif
  },

  /**
   * 重置指定表（或全部表）的同步游标，下次同步将全量拉取
   * @param {string[]} [tables] - 要重置的表名数组，为空则重置全部
   */
  async resetCursors(tables) {
    // #ifdef APP-PLUS
    try {
      if (tables && tables.length > 0) {
        for (const t of tables) {
          await sqliteDB.executeSql(`DELETE FROM _sync_cursor WHERE table_name = ${sqliteDB._escapeValue(t)}`)
        }
        console.log('[SyncScheduler] 游标已重置:', tables.join(', '))
      } else {
        await sqliteDB.executeSql('DELETE FROM _sync_cursor')
        console.log('[SyncScheduler] 全部游标已重置')
      }
    } catch (e) {
      console.warn('[SyncScheduler] 重置游标失败:', e)
    }
    // #endif
  },

  /**
   * 上行同步：推送 pending 任务到服务端
   */
  async syncPendingTasks() {
    // #ifdef APP-PLUS
    const account = getOfflineAccount(); if (!account) return
    if (isSyncing) return
    isSyncing = true

    try {
      const tasks = await offlineQueue.getPendingTasks(BATCH_SIZE)
      if (!tasks || tasks.length === 0) {
        isSyncing = false
        return
      }

      console.log(`[SyncScheduler] 开始上行同步: ${tasks.length} 条待处理任务`)

      for (const task of tasks) {
        if (!isOnline.value || getOfflineAccount() !== account) break  // 网络断开则停止

        try {
          if (task.table_name === 'px_note') {
            await withNoteWriteLock(async () => {
              const rows = await sqliteDB.selectSql('SELECT * FROM _sync_queue WHERE id = ' + sqliteDB._escapeValue(task.id))
              const latest = rows[0]
              if (!latest || latest.status !== 'pending') return
              await offlineQueue.markSyncing(task.id)
              await this._syncOneTask(latest)
            })
          } else {
            await offlineQueue.markSyncing(task.id)
            await this._syncOneTask(task)
          }
        } catch (e) {
          if (getOfflineAccount() !== account) break
          console.error('[SyncScheduler] 单条同步失败:', task.id, e)
          // 判断重试次数
          const retryCount = (task.retry_count || 0) + 1
          if (e.code === 409) {
            await offlineQueue.updateStatus(task.id, 'conflict', JSON.stringify({
              errorCode: e.errorCode, message: e.message, current: e.current
            }))
          } else if (retryCount >= MAX_RETRY) {
            await offlineQueue.updateStatus(task.id, 'conflict', e?.message || 'unknown')
          } else {
            await offlineQueue.incrementRetry(task.id)
          }
        }
      }

      // 清理已同步任务
      if (getOfflineAccount() === account) await offlineQueue.cleanSynced()
    } catch (e) {
      console.error('[SyncScheduler] 上行同步异常:', e)
    } finally {
      isSyncing = false
    }
    // #endif
  },

  /**
   * 同步单条任务到服务端
   */
  async _syncOneTask(task) {
    // #ifdef APP-PLUS
    const account = getOfflineAccount(); if (!account) throw new Error('请先登录')
    const payload = JSON.parse(task.payload || '{}')

    // 附加 clientUuid 用于服务端幂等去重
    if (!payload.clientUuid) payload.clientUuid = task.id

    let res
    let serverId = null
    let revision = null
    if (IDEMPOTENT_BATCH_TABLES.has(task.table_name)) {
      res = await request({
        url: '/offline/batch',
        method: 'post',
        data: {
          operations: [{
            tableName: task.table_name,
            method: String(task.method || '').toUpperCase(),
            payload,
            clientUuid: task.id
          }]
        },
        offline: false,
        _isSyncCall: true
      })
      if (getOfflineAccount() !== account) throw new Error('账号已变化')
      const result = res.data?.results?.[0]
      if (!result || !['success', 'skip'].includes(result.status)) {
        throw Object.assign(new Error(result?.errorMsg || '离线批量同步失败'), {
          code: result?.status === 'conflict' ? 409 : undefined,
          errorCode: result?.errorCode, current: result?.current
        })
      }
      serverId = result.id || null
      revision = result.revision
    } else {
      res = await request({
        url: task.url,
        method: task.method,
        data: payload,
        offline: false,
        _isSyncCall: true
      })
      serverId = res.data?.id || res.data || null
    }

    if (getOfflineAccount() !== account) throw new Error('账号已变化')
    // 同步成功 → 更新本地 _server_id + 状态
    if (res.code === 200) {
      if (task.table_name === 'px_note') {
        const where = payload._noteLocalId ?
          'id = ' + sqliteDB._escapeValue(payload._noteLocalId) :
          'client_uuid = ' + sqliteDB._escapeValue(payload.clientUuid || task.id)
        if (String(task.method).toUpperCase() === 'DELETE') {
          await sqliteDB.executeSql('DELETE FROM px_note WHERE ' + where)
        } else {
          if (!serverId || !revision) throw new Error('服务端未返回笔记修订号，请更新服务端后重试')
          await sqliteDB.update('px_note', { ...payload, revision, _server_id: serverId, _sync_status: 1 },
            payload._noteLocalId ? { id: payload._noteLocalId } : { client_uuid: payload.clientUuid || task.id }, true)
        }
        await offlineQueue.updateStatus(task.id, 'synced')
        return
      }
      await offlineQueue.updateStatus(task.id, 'synced')

      // 更新本地业务表的 _server_id 和 _sync_status
      if (task.table_name && serverId) {
        if (serverId && (typeof serverId === 'number' || (typeof serverId === 'string' && /^\d+$/.test(serverId)))) {
          const numServerId = Number(serverId)
          try {
            await sqliteDB.executeSql(
              `UPDATE ${task.table_name} SET \`_server_id\` = ${numServerId}, \`_sync_status\` = 1 WHERE \`client_uuid\` = ${sqliteDB._escapeValue(task.id)}`
            )
            // 清理重复数据：如果已存在相同 _server_id 的其他记录，删除多余的那条
            const duplicates = await sqliteDB.selectSql(
              `SELECT id FROM ${task.table_name} WHERE \`_server_id\` = ${numServerId} ORDER BY id ASC`
            )
            if (duplicates && duplicates.length > 1) {
              // 保留第一条，删除其余
              const keepId = duplicates[0].id
              const deleteIds = duplicates.slice(1).map(d => d.id)
              await sqliteDB.executeSql(
                `DELETE FROM ${task.table_name} WHERE id IN (${deleteIds.join(',')}) AND id != ${keepId}`
              )
              console.log(`[SyncScheduler] 清理重复记录: ${task.table_name}._server_id=${numServerId}, 删除 ${deleteIds.length} 条重复`)
            }
          } catch (e) {
            console.warn('[SyncScheduler] 更新 _server_id 失败:', e)
          }
        }
      }
    }
    // #endif
  },

  /**
   * 下行同步：从服务端并行拉取增量变更到本地
   */
  async pullServerChanges() {
    // #ifdef APP-PLUS
    if (!isOnline.value) return

    await Promise.all(SYNC_MODULES.map(mod => this._pullModule(mod)))
    // #endif
  },

  /**
   * 拉取单个模块的增量数据
   */
  async _pullNotes() {
    // #ifdef APP-PLUS
    const account = getOfflineAccount(); if (!account) return
    try {
      const stored = await sqliteDB.selectSql("SELECT last_cursor FROM _sync_cursor WHERE table_name = 'px_note'")
      let cursor = /^n2:[0-9]+:[0-9]+$/.test(stored?.[0]?.last_cursor || '') ? stored[0].last_cursor : '0'
      let resets = 0
      while (true) {
        const res = await request({ url: '/workspace/notes/changes', method: 'get', params: { cursor }, offline: false, _isSyncCall: true })
        if (getOfflineAccount() !== account) return
        if (res.code !== 200 || !Array.isArray(res.data?.items)) throw new Error('笔记同步响应无效')
        if (res.data.resetRequired) {
          if (++resets > 1) throw new Error('笔记游标无法重置')
          cursor = '0'
          continue
        }
        if (!/^n2:[0-9]+:[0-9]+$/.test(res.data.nextCursor || '')) throw new Error('笔记同步游标无效')
        await withNoteWriteLock(async () => {
          // Advance only after every upsert/tombstone succeeds. Replaying a partially applied page is safe.
          for (const item of res.data.items) {
            const deleted = item.delFlag === 1 || item.delFlag === '1' || item.delFlag === true
            if (deleted) {
              if (!/^[1-9][0-9]*$/.test(String(item.id)) || !/^[1-9][0-9]*$/.test(String(item.revision))) throw new Error('笔记墓碑无效')
              const matches = [`_server_id = ${sqliteDB._escapeValue(item.id)}`]
              if (item.clientUuid) matches.push(`client_uuid = ${sqliteDB._escapeValue(item.clientUuid)}`)
              await sqliteDB.executeSql(`DELETE FROM px_note WHERE (${matches.join(' OR ')}) AND _sync_status = 1 AND (revision IS NULL OR revision <= ${sqliteDB._escapeValue(item.revision)})`)
            } else {
              const result = await sqliteDB.upsertBatch('px_note', [{ ...item, _server_id: item.id, _sync_status: 1, _updated_at: item.updateTime }], true)
              if (result.failed) throw new Error('笔记写入失败，保留游标重试')
            }
          }
          await sqliteDB.executeSql(`INSERT OR REPLACE INTO _sync_cursor (table_name,last_cursor,last_sync_at) VALUES ('px_note',${sqliteDB._escapeValue(res.data.nextCursor)},${sqliteDB._escapeValue(new Date().toISOString())})`)
        })
        if (!res.data.hasMore) break
        if (cursor === res.data.nextCursor) throw new Error('笔记游标未推进')
        cursor = res.data.nextCursor
      }
    } catch (error) { console.warn('[SyncScheduler] 笔记同步失败，下次重试', error?.message || error) }
    // #endif
  },

  async _pullModule(mod) {
    // #ifdef APP-PLUS
    if (mod.table === 'px_note') return this._pullNotes()
    const account = getOfflineAccount(); if (!account) return
    try {
      // 获取该模块最后同步时间（datetime 游标）
      let since = '2020-01-01 00:00:00'
      try {
        const cursor = await sqliteDB.selectSql(
          `SELECT last_cursor FROM _sync_cursor WHERE table_name = ${sqliteDB._escapeValue(mod.table)}`
        )
        if (cursor && cursor.length > 0 && cursor[0].last_cursor) {
          const val = cursor[0].last_cursor
          if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(String(val))) {
            since = val
          } else {
            console.warn(`[SyncScheduler] 游标格式无效: ${mod.table} = ${val}，重置为默认值`)
            await sqliteDB.executeSql(
              `DELETE FROM _sync_cursor WHERE table_name = ${sqliteDB._escapeValue(mod.table)}`
            )
          }
        }
      } catch (e) {
        // _sync_cursor 表可能尚未创建
      }

      // 游标合理性校验：如果游标存在但本地表为空，说明之前 upsert 全部失败，需重置游标
      if (since !== '2020-01-01 00:00:00') {
        try {
          const localCount = await sqliteDB.count(mod.table)
          if (localCount === 0) {
            console.warn(`[SyncScheduler] ${mod.table} 游标存在(${since})但本地无数据，重置游标`)
            await sqliteDB.executeSql(`DELETE FROM _sync_cursor WHERE table_name = '${mod.table}'`)
            since = '2020-01-01 00:00:00'
          }
        } catch (e) {
          // 表可能尚未创建
        }
      }

      // 分页拉取
      let offset = 0
      let hasMore = true
      let totalItems = 0
      let lastNextSince = null
      let upsertFailed = false

      while (hasMore) {
        const res = await new Promise((resolve, reject) => {
          request({
            url: mod.syncUrl,
            method: 'get',
            params: { since, offset },
            offline: false,
            _isSyncCall: true
          }).then(resolve).catch(reject)
        })

        if (getOfflineAccount() !== account) return
        if (res.code === 200 && res.data?.items) {
          console.log(`[SyncScheduler] ${mod.table} 拉取到 ${res.data.items.length} 条记录`)

          // 软删除记录与正常记录分开处理
          const normalItems = []
          const deletedServerIds = []
          const deletedClientUuids = []
          for (const item of res.data.items) {
            const isDeleted = item.delFlag === true || item.delFlag === '1' || item.delFlag === 1 || item.del_flag === 1 || item.del_flag === '1'
            if (isDeleted) {
              deletedServerIds.push(item.id)
              if (item.clientUuid || item.client_uuid) {
                deletedClientUuids.push(item.clientUuid || item.client_uuid)
              }
            } else {
              normalItems.push(item)
            }
          }

          // 正常记录：合并到本地
          const items = normalItems.map(item => ({
            ...item,
            _sync_status: 1,
            _server_id: item.id,
            _updated_at: item.updateTime || item.update_time || new Date().toISOString()
          }))

          const result = await sqliteDB.upsertBatch(mod.table, items)
          if (result.failed > 0) {
            upsertFailed = true
            console.warn(`[SyncScheduler] upsert 部分失败: ${mod.table}, ${result.failed}/${result.total}`)
          }

          // 软删除记录：从本地 SQLite 中删除
          if (deletedServerIds.length > 0) {
            try {
              const ids = deletedServerIds.join(',')
              await sqliteDB.executeSql(
                `DELETE FROM ${mod.table} WHERE \`_server_id\` IN (${ids})${mod.table === 'px_note' ? ' AND _sync_status = 1' : ''}`
              )
              if (deletedClientUuids.length > 0) {
                const uuids = deletedClientUuids.map(u => sqliteDB._escapeValue(u)).join(',')
                await sqliteDB.executeSql(
                  `DELETE FROM ${mod.table} WHERE \`client_uuid\` IN (${uuids})${mod.table === 'px_note' ? ' AND _sync_status = 1' : ''}`
                )
              }
              console.log(`[SyncScheduler] 同步删除: ${mod.table}, ${deletedServerIds.length} 条`)
            } catch (e) {
              console.error(`[SyncScheduler] 同步删除失败: ${mod.table}`, e)
            }
          }

          totalItems += items.length

          // 清理同 _server_id 的重复记录
          try {
            const dupes = await sqliteDB.selectSql(
              `SELECT _server_id, COUNT(*) as cnt FROM ${mod.table} WHERE _server_id IS NOT NULL GROUP BY _server_id HAVING cnt > 1`
            )
            if (dupes && dupes.length > 0) {
              for (const d of dupes) {
                const rows = await sqliteDB.selectSql(
                  `SELECT id FROM ${mod.table} WHERE _server_id = ${d._server_id} ORDER BY id ASC`
                )
                if (rows && rows.length > 1) {
                  const keepId = rows[0].id
                  const deleteIds = rows.slice(1).map(r => r.id)
                  await sqliteDB.executeSql(
                    `DELETE FROM ${mod.table} WHERE id IN (${deleteIds.join(',')})${mod.table === 'px_note' ? ' AND _sync_status = 1' : ''}`
                  )
                  console.log(`[SyncScheduler] 清理重复: ${mod.table}._server_id=${d._server_id}, 删除 ${deleteIds.length} 条`)
                }
              }
            }
          } catch (e) {
            console.warn('[SyncScheduler] 清理重复记录失败:', e)
          }

          hasMore = res.data.hasMore === true
          // offset 基于服务端原始页大小推进；若整页都是软删除记录，按正常记录数推进会死循环。
          offset += res.data.items.length

          if (res.data.nextSince) {
            lastNextSince = res.data.nextSince
          }
        } else {
          hasMore = false
        }
      }

      // 全部分页完成后才更新游标
      if (lastNextSince && !upsertFailed) {
        await sqliteDB.executeSql(
          `INSERT OR REPLACE INTO _sync_cursor (table_name, last_cursor, last_sync_at) VALUES ('${mod.table}', '${lastNextSince}', '${new Date().toISOString()}')`
        )
      } else if (upsertFailed) {
        console.warn(`[SyncScheduler] ${mod.table} 存在 upsert 失败，游标不更新，下次将重新拉取`)
      }

      if (totalItems > 0) {
        console.log(`[SyncScheduler] 下行同步成功: ${mod.table}, ${totalItems} 条`)
      }
    } catch (e) {
      console.warn(`[SyncScheduler] 下行同步跳过: ${mod.table}`, e?.message || e)
    }
    // #endif
  }
}

export default syncScheduler
