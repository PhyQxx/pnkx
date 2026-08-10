/**
 * 离线写操作队列
 *
 * 写操作入队 → SQLite _sync_queue 持久化 → 网络恢复后批量同步
 * 任务状态：pending → syncing → synced / conflict
 */
// #ifdef APP-PLUS
import sqliteDB from '@/utils/sqliteDB'
// #endif

const offlineQueue = {
  /**
   * 入队：写操作进 SQLite 队列表
   */
  async enqueue(task) {
    // #ifdef APP-PLUS
    try {
      await sqliteDB.insert('_sync_queue', {
        id: task.id,
        table_name: task.tableName,
        method: task.method,
        url: task.url,
        payload: JSON.stringify(task.payload || {}),
        status: 'pending',
        retry_count: 0,
        created_at: task.createdAt,
        error_msg: null
      })
      console.log('[OfflineQueue] 任务入队:', task.id, task.url)
    } catch (e) {
      console.error('[OfflineQueue] 入队失败:', e)
    }
    // #endif
  },

  /**
   * 获取待同步任务（按时间升序，一次最多 limit 条）
   */
  async getPendingTasks(limit = 20) {
    // #ifdef APP-PLUS
    try {
      return await sqliteDB.selectSql(
        `SELECT * FROM _sync_queue WHERE status = 'pending' ORDER BY created_at ASC LIMIT ${limit}`
      )
    } catch (e) {
      console.error('[OfflineQueue] 获取待同步任务失败:', e)
      return []
    }
    // #endif

    // #ifndef APP-PLUS
    return []
    // #endif
  },

  /**
   * 更新任务状态
   */
  async updateStatus(taskId, status, errorMsg = null) {
    // #ifdef APP-PLUS
    try {
      const errorVal = errorMsg ? sqliteDB._escapeValue(errorMsg) : 'NULL'
      await sqliteDB.executeSql(
        `UPDATE _sync_queue SET status = ${sqliteDB._escapeValue(status)}, error_msg = ${errorVal} WHERE id = ${sqliteDB._escapeValue(taskId)}`
      )
    } catch (e) {
      console.error('[OfflineQueue] 更新状态失败:', e)
    }
    // #endif
  },

  /**
   * 标记任务为同步中
   */
  async markSyncing(taskId) {
    // #ifdef APP-PLUS
    await sqliteDB.executeSql(
      `UPDATE _sync_queue SET status = 'syncing' WHERE id = ${sqliteDB._escapeValue(taskId)}`
    )
    // #endif
  },

  /**
   * 增加重试计数
   */
  async incrementRetry(taskId) {
    // #ifdef APP-PLUS
    try {
      await sqliteDB.executeSql(
        `UPDATE _sync_queue SET retry_count = retry_count + 1, status = 'pending' WHERE id = '${taskId}'`
      )
    } catch (e) {
      console.error('[OfflineQueue] 增加重试失败:', e)
    }
    // #endif
  },

  /**
   * 清除已同步的任务
   */
  async cleanSynced() {
    // #ifdef APP-PLUS
    try {
      await sqliteDB.executeSql(`DELETE FROM _sync_queue WHERE status = 'synced'`)
    } catch (e) {
      console.error('[OfflineQueue] 清除已同步任务失败:', e)
    }
    // #endif
  },

  /**
   * 获取队列统计
   */
  async getStats() {
    // #ifdef APP-PLUS
    try {
      const pending = await sqliteDB.count('_sync_queue', { status: 'pending' })
      const syncing = await sqliteDB.count('_sync_queue', { status: 'syncing' })
      const conflict = await sqliteDB.count('_sync_queue', { status: 'conflict' })
      return { pending, syncing, conflict }
    } catch (e) {
      return { pending: 0, syncing: 0, conflict: 0 }
    }
    // #endif

    // #ifndef APP-PLUS
    return { pending: 0, syncing: 0, conflict: 0 }
    // #endif
  },

  /**
   * 获取冲突任务列表
   */
  async getConflictTasks() {
    // #ifdef APP-PLUS
    try {
      return await sqliteDB.selectSql(
        `SELECT * FROM _sync_queue WHERE status = 'conflict' ORDER BY created_at ASC`
      )
    } catch (e) {
      return []
    }
    // #endif

    // #ifndef APP-PLUS
    return []
    // #endif
  }
}

export default offlineQueue
