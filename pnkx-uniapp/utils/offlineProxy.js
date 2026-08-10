/**
 * 离线请求代理模块
 *
 * 核心职责：
 * - 离线时 GET/DELETE → 从本地 SQLite 读取
 * - 离线时 POST/PUT   → 写入本地 SQLite + 进同步队列
 * - 在线时 GET 成功   → 缓存响应数据到本地
 */
// #ifdef APP-PLUS
import offlineQueue from '@/utils/offlineQueue'
import sqliteDB from '@/utils/sqliteDB'
// #endif

/** URL 前缀 → 本地表名 映射（按长度降序匹配最长前缀） */
const URL_TABLE_MAP = {
  '/note/folder': 'px_note_folder',
  '/note': 'px_note',
  '/bookkeeping/classification': 'px_bookkeeping_classification',
  '/bookkeeping/account': 'px_bookkeeping_account',
  '/bookkeeping/record': 'px_bookkeeping_record',
  '/admin/diary': 'px_diary',
  '/admin/toDo': 'px_todo',
  '/commemorationDay': 'px_commemoration_day',
  '/px/card/listRecord': 'px_card_record',
  '/px/card/getToDoCard': 'px_card_record',
  '/px/card': 'px_card',
  '/system/menu': 'px_menu'
}

// 预排序的 key 列表（长→短），确保最长前缀优先匹配
const SORTED_URL_KEYS = Object.keys(URL_TABLE_MAP).sort((a, b) => b.length - a.length)

const offlineProxy = {
  /**
   * 处理离线请求（由 request.js 调用）
   */
  async handle(config) {
    const method = (config.method || 'get').toLowerCase()

    // 只读请求 → 从本地读取
    if (method === 'get' || method === 'delete') {
      return this.readLocal(config)
    }

    // 写请求 → 本地写入 + 进队列
    return this.writeLocal(config)
  },

  /**
   * 离线读：从本地 SQLite 查询
   */
  async readLocal(config) {
    const tableName = this._parseTableName(config.url)
    if (!tableName) {
      // 不支持的 URL，返回空数据（避免组件 .length 报错）
      return Promise.resolve({ code: 200, msg: 'offline_no_table', rows: [], total: 0, data: [] })
    }

    try {
      // #ifdef APP-PLUS
      const query = this._parseQuery(config)
      let data = []

      if (query.id) {
        // 单条查询：/admin/diary/123
        const row = await sqliteDB.getById(tableName, query.id)
        data = row ? [row] : []
        return Promise.resolve({ code: 200, msg: 'offline', data: row, rows: data })
      }

      console.log('[OfflineProxy] 离线查询:', config.url, 'tableName:', tableName, 'query:', JSON.stringify(query))

      // 列表查询
      if (tableName === 'px_bookkeeping_record') {
        // 服务器用 version 参数标记"查询当天记录"（LEFT(pay_time,10)=today）
        // 服务器用 payTime 参数做月度筛选（LEFT(pay_time,7)=LEFT(payTime,7)）
        const conditions = ['`del_flag` = 0']
        if (query.version) {
          delete query.version
          const today = new Date()
          const y = today.getFullYear()
          const m = String(today.getMonth() + 1).padStart(2, '0')
          const d = String(today.getDate()).padStart(2, '0')
          conditions.push(`\`pay_time\` LIKE ${sqliteDB._escapeValue(y + '-' + m + '-' + d + '%')}`)
        }
        if (query.payTime || query.pay_time) {
          const payTimeVal = query.payTime || query.pay_time
          delete query.payTime
          delete query.pay_time
          // 服务器: LEFT(pay_time, 7) = LEFT(#{payTime}, 7)，支持 "2025-01" 或 "2025-01-15" 格式
          const monthPrefix = String(payTimeVal).substring(0, 7)
          conditions.push(`\`pay_time\` LIKE ${sqliteDB._escapeValue(monthPrefix + '%')}`)
        }
        // 其他等值条件
        const normalizedWhere = sqliteDB._normalizeWhereKeys(tableName, query)
        const extraConds = sqliteDB._buildWhere(normalizedWhere)
        if (extraConds) conditions.push(extraConds)
        const sql = `SELECT * FROM ${tableName} WHERE ${conditions.join(' AND ')} ORDER BY \`pay_time\` DESC`
        console.log('[OfflineProxy] 账单查询SQL:', sql)
        data = await sqliteDB.selectSql(sql)
      } else {
        data = await sqliteDB.query(tableName, query)
      }

      // 防空保护
      if (!data || !Array.isArray(data)) {
        data = []
      }

      // 字段适配：部分 API 返回 JOIN 数据，本地只有基础表，需转换
      data = await this._adaptData(config.url, tableName, data)

      // 防空保护（_adaptData 不应返回 undefined，但保险起见）
      if (!data || !Array.isArray(data)) {
        data = []
      }

      // 分页处理（分类/账户等字典数据不分页，直接全量返回）
      const noPagedTables = ['px_bookkeeping_classification', 'px_bookkeeping_account', 'px_commemoration_day']
      let pagedData, total
      if (noPagedTables.includes(tableName)) {
        pagedData = data
        total = data.length
      } else {
        const pageNum = parseInt(query.pageNum) || 1
        const pageSize = parseInt(query.pageSize) || 10
        const start = (pageNum - 1) * pageSize
        pagedData = data.slice(start, start + pageSize)
        total = data.length
      }

      return Promise.resolve({
        code: 200,
        msg: 'offline',
        rows: pagedData,
        total: total,
        data: pagedData   // 兼容在线 API 返回格式（部分页面用 res.data 获取列表）
      })
      // #endif

      // #ifndef APP-PLUS
      return Promise.resolve({ code: 0, msg: 'offline_not_supported' })
      // #endif
    } catch (e) {
      console.error('[OfflineProxy] 本地读取失败:', config.url, e)
      return Promise.resolve({ code: 200, msg: 'offline_read_error', rows: [], total: 0, data: [] })
    }
  },

  /**
   * 离线写：写入本地 SQLite + 进同步队列
   */
  async writeLocal(config) {
    const method = (config.method || 'post').toLowerCase()
    const tableName = this._parseTableName(config.url)

    if (!tableName) {
      // 不支持的 URL，写操作无法离线处理
      return Promise.resolve({ code: 500, msg: 'offline_not_supported' })
    }

    try {
      // #ifdef APP-PLUS
      const clientUuid = this._uuid()
      const now = new Date().toISOString()

      // 构建本地记录
      const record = {
        ...(config.data || {}),
        client_uuid: clientUuid,
        _sync_status: 0,
        _server_id: null,
        _updated_at: now,
        create_time: now,
        update_time: now
      }

      if (method === 'post') {
        // 新增
        await sqliteDB.insert(tableName, record)
      } else if (method === 'put') {
        // 修改
        const id = record.id || this._parseIdFromUrl(config.url)
        if (id) {
          delete record.id  // id 不应被更新
          await sqliteDB.update(tableName, record, { id })
        }
      }

      // 进同步队列
      await offlineQueue.enqueue({
        id: clientUuid,
        tableName,
        method,
        url: config.url,
        payload: config.data,
        status: 'pending',
        createdAt: now
      })

      console.log('[OfflineProxy] 离线写入成功:', method, tableName, clientUuid)

      // 乐观 UI 响应
      return Promise.resolve({
        code: 200,
        msg: 'offline_queued',
        data: record,
        taskId: clientUuid
      })
      // #endif

      // #ifndef APP-PLUS
      return Promise.resolve({ code: 0, msg: 'offline_not_supported' })
      // #endif
    } catch (e) {
      console.error('[OfflineProxy] 离线写入失败:', config.url, e)
      return Promise.resolve({ code: 500, msg: 'offline_write_error' })
    }
  },

  /**
   * 缓存在线 GET 响应（供 request.js 在线请求成功后调用）
   */
  async cacheResponse(config, data) {
    // 仅缓存关键模块的列表数据
    const tableName = this._parseTableName(config.url)
    if (!tableName) return

    // 提取数据行
    let rows = data?.rows || data?.data?.rows || (Array.isArray(data?.data) ? data.data : null)
    if (!rows || !Array.isArray(rows) || rows.length === 0) return

    // #ifdef APP-PLUS
    try {
      // 分类接口返回树形结构（含children），需要展平再存储
      if (tableName === 'px_bookkeeping_classification') {
        const flatRows = []
        const flatten = (list) => {
          for (const item of list) {
            const children = item.children || item.childList || []
            // 只存储有 id 的真实分类记录（跳过"最近使用"等虚拟节点）
            if (item.id) {
              const clone = { ...item }
              delete clone.children
              delete clone.childList
              flatRows.push(clone)
            }
            if (children.length > 0) flatten(children)
          }
        }
        flatten(rows)
        rows = flatRows
      }

      // 账户接口返回树形结构（虚拟类型节点 + 实际账户），需要展平并提取类型映射
      if (tableName === 'px_bookkeeping_account') {
        const flatRows = []
        const accountTypeMap = {} // accountType → { accountName, accountIcon }
        const flatten = (list) => {
          for (const item of list) {
            const children = item.children || item.childList || []
            if (item.id) {
              // 有 id 的是实际账户记录，存储
              const clone = { ...item }
              delete clone.children
              delete clone.childList
              flatRows.push(clone)
            } else {
              // 无 id 的是虚拟类型节点，提取类型映射
              accountTypeMap[item.accountType] = {
                accountName: item.accountName,
                accountIcon: item.accountIcon
              }
            }
            if (children.length > 0) flatten(children)
          }
        }
        flatten(rows)
        rows = flatRows
        // 缓存账户类型映射到 storage，供离线时构建账户树
        if (Object.keys(accountTypeMap).length > 0) {
          try {
            uni.setStorageSync('offline_account_type_map', accountTypeMap)
          } catch (e) {
            console.warn('[OfflineProxy] 缓存账户类型映射失败:', e)
          }
        }
      }

      const items = rows.map(row => ({
        ...row,
        _sync_status: 1,
        _server_id: row.id,
        _updated_at: row.updateTime || row.update_time || new Date().toISOString()
      }))
      await sqliteDB.upsertBatch(tableName, items)
    } catch (e) {
      // 缓存失败不影响正常流程
      console.warn('[OfflineProxy] 缓存响应失败:', tableName, e)
    }
    // #endif
  },

  // ──────────── 辅助方法 ────────────

  /**
   * 离线数据字段适配
   * 服务器部分 API 返回 JOIN 数据，本地只有基础表，需做字段映射
   */
  async _adaptData(url, tableName, data) {
    if (!data || !Array.isArray(data)) return data

    // 通用：蛇形字段名 → 驼峰（前端组件统一使用驼峰）
    data = data.map(row => {
      const mapped = { ...row }
      for (const key of Object.keys(row)) {
        if (key.includes('_')) {
          const camelKey = key.replace(/_([a-z])/g, (_, c) => c.toUpperCase())
          if (camelKey !== key && !(camelKey in mapped)) {
            mapped[camelKey] = row[key]
          }
        }
      }
      return mapped
    })

    // getCardByUserId: 服务器返回 px_card_user + px_lovers_card JOIN 数据
    // 本地 px_card 只存卡券定义，需映射 id→cardId, number→cardNumber
    if (url && url.includes('/getCardByUserId') && tableName === 'px_card') {
      return data.map(row => ({
        ...row,
        cardId: row.id,
        cardNumber: row.number,
        cardName: row.title
      }))
    }

    // 记账分类：服务器返回树形结构（含 children），本地需要从扁平数据构建树
    if (tableName === 'px_bookkeeping_classification') {
      return this._buildClassificationTree(data)
    }

    // 记账账户：服务器返回按类型分组的树形结构，本地需要从扁平数据构建树
    if (tableName === 'px_bookkeeping_account') {
      return this._buildAccountTree(data)
    }

    // 记账记录：服务器返回 typeObject/accountObject 关联对象，本地需要关联分类/账户表
    if (tableName === 'px_bookkeeping_record') {
      // 去重：可能存在离线创建记录（_server_id=null）和同步记录（_server_id!=null）同时存在的情况
      // 优先保留 _server_id 不为 null 的版本（已同步的）
      const seenServerIds = new Set()
      const seenClientUuids = new Set()
      const dedupedData = []
      // 先排个序，_server_id 不为 null 的排前面（优先保留）
      const sorted = [...data].sort((a, b) => {
        if (a._server_id && !b._server_id) return -1
        if (!a._server_id && b._server_id) return 1
        return 0
      })
      for (const row of sorted) {
        const serverKey = row._server_id ? String(row._server_id) : null
        const clientKey = row.client_uuid || null
        // 如果 _server_id 已见过，跳过重复
        if (serverKey && seenServerIds.has(serverKey)) continue
        // 如果 client_uuid 已见过且当前记录 _server_id 为 null，跳过（已有同步版本）
        if (clientKey && seenClientUuids.has(clientKey) && !serverKey) continue
        if (serverKey) seenServerIds.add(serverKey)
        if (clientKey) seenClientUuids.add(clientKey)
        dedupedData.push(row)
      }
      data = dedupedData

      const classificationMap = {}
      const classificationLocalMap = {}
      const accountMap = {}
      const accountLocalMap = {}
      let classifications = null
      let accounts = null
      try {
        // 批量加载分类和账户数据用于关联
        classifications = await sqliteDB.selectSql('SELECT `id`, `_server_id`, `type_name`, `type_icon`, `type_difference` FROM px_bookkeeping_classification WHERE `_server_id` IS NOT NULL OR `type_name` IS NOT NULL')
        if (classifications) {
          classifications.forEach(c => {
            // 同步记录的 type 是服务器ID → 用 _server_id 映射
            if (c._server_id) classificationMap[String(c._server_id)] = c
            // 只将有 type_name 的记录加入本地映射（排除旧的虚拟节点脏数据）
            if (c.type_name) classificationLocalMap[String(c.id)] = c
          })
        }
        accounts = await sqliteDB.selectSql('SELECT `id`, `_server_id`, `account_name`, `account_icon` FROM px_bookkeeping_account WHERE `_server_id` IS NOT NULL OR `account_name` IS NOT NULL')
        if (accounts) {
          accounts.forEach(a => {
            if (a._server_id) accountMap[String(a._server_id)] = a
            // 只将有 account_name 的记录加入本地映射（排除旧的虚拟类型节点脏数据）
            if (a.account_name) accountLocalMap[String(a.id)] = a
          })
        }
      } catch (e) {
        // 关联数据可能尚未同步
      }

      // 调试：打印记录的 type 值和分类表的 _server_id/id 映射
      console.log('[OfflineProxy] 分类映射表(classificationMap):', JSON.stringify(
        classifications ? classifications.map(c => ({ localId: c.id, serverId: c._server_id, name: c.type_name })) : []
      ))
      console.log('[OfflineProxy] 记录type值:', data.map(r => ({ id: r.id, _server_id: r._server_id, type: r.type, type_type: typeof r.type })))

      return data.map(row => {
        // 离线创建记录的 type 可能是服务器ID（从 _buildClassificationTree 构建的分类树中选取的）
        // 也可能是本地ID（如果分类本身也是离线创建的），因此需要同时查两个映射
        const typeObj = classificationMap[String(row.type)] || classificationLocalMap[String(row.type)] || null
        const accountObj = accountMap[String(row.account)] || accountLocalMap[String(row.account)] || null
        const otherAccountObj = accountMap[String(row.other_account)] || accountLocalMap[String(row.other_account)] || null
        console.log('[OfflineProxy] 匹配结果: row.type=', row.type, 'row._server_id=', row._server_id, '→ matched=', !!typeObj, typeObj ? typeObj.type_name : 'null')
        return {
          ...row,
          typeObject: typeObj ? {
            id: typeObj._server_id || typeObj.id,
            typeName: typeObj.type_name,
            typeIcon: typeObj.type_icon,
            typeDifference: typeObj.type_difference
          } : null,
          accountObject: accountObj ? {
            id: accountObj._server_id || accountObj.id,
            accountName: accountObj.account_name,
            accountIcon: accountObj.account_icon
          } : null,
          otherAccountObject: otherAccountObj ? {
            id: otherAccountObj._server_id || otherAccountObj.id,
            accountName: otherAccountObj.account_name,
            accountIcon: otherAccountObj.account_icon
          } : null
        }
      })
    }

    return data
  },

  /**
   * 从扁平分类列表构建树形结构（服务器 getClassificationList 返回带 children 的树）
   * 
   * 关键：本地表中 id 是自增本地ID，_server_id 是服务器ID，
   * 而 type_parent_id 存的是服务器ID（同步下来的值），
   * 所以必须用 _server_id 建立映射，typeParentId 才能正确匹配到父节点
   */
  _buildClassificationTree(flatList) {
    if (!flatList || flatList.length === 0) return flatList

    // 过滤掉脏数据：之前 cacheResponse 未过滤时，虚拟节点（如"最近使用"，无 _server_id）也被存入了表
    flatList = flatList.filter(item => item._server_id || item.id)

    if (flatList.length === 0) return []

    // 将蛇形字段转为驼峰，匹配前端组件期望的格式
    const items = flatList.map(item => ({
      ...item,
      // 前端组件通过 item.id 标识分类，需要用服务器ID（因为记录表中的 type 字段存的是服务器ID）
      id: item._server_id || item.id,
      typeName: item.type_name || item.typeName,
      typeIcon: item.type_icon || item.typeIcon,
      typeDifference: item.type_difference || item.typeDifference,
      typeLevel: item.type_level || item.typeLevel,
      typeParentId: item.type_parent_id || item.typeParentId,
      children: []
    }))

    // 用 _server_id 建立映射（type_parent_id 的值是服务器ID）
    const serverIdMap = {}
    const roots = []
    items.forEach(item => {
      const serverId = item._server_id || item.id
      if (serverId) serverIdMap[String(serverId)] = item
    })

    items.forEach(item => {
      if (item.typeParentId && serverIdMap[String(item.typeParentId)]) {
        serverIdMap[String(item.typeParentId)].children.push(item)
      } else {
        roots.push(item)
      }
    })

    return roots
  },

  /**
   * 从扁平账户列表构建树形结构（服务器 getAccountList 返回按类型分组的树）
   * 
   * 服务器返回的树结构：
   * - 顶级是虚拟类型节点（来自数据字典 px_bookkeeping_account_type），无 id
   * - 每个 type 节点的 children 是该类型的实际账户
   * 
   * 离线构建策略：
   * - 从 uni storage 读取缓存的 accountType 映射（在线时 cacheResponse 提取）
   * - 按 account_type 值分组，为每组创建虚拟类型节点
   * - 实际账户的 id 使用 _server_id（服务器ID），因为记录表中 account 字段存的是服务器ID
   */
  _buildAccountTree(flatList) {
    if (!flatList || flatList.length === 0) return flatList

    // 过滤掉脏数据：之前 cacheResponse 未展平时，虚拟类型节点（无 _server_id）也被存入了表
    // 虚拟类型节点的特征：_server_id 为 NULL 或空
    flatList = flatList.filter(item => item._server_id || item.id)

    if (flatList.length === 0) return []

    // 读取缓存的账户类型映射
    let accountTypeMap = {}
    try {
      accountTypeMap = uni.getStorageSync('offline_account_type_map') || {}
    } catch (e) {
      console.warn('[OfflineProxy] 读取账户类型映射失败:', e)
    }

    // 将蛇形字段转为驼峰，匹配前端组件期望的格式
    const items = flatList.map(item => ({
      ...item,
      // 前端组件通过 item.id 标识账户，需要用服务器ID（因为记录表中的 account 字段存的是服务器ID）
      id: item._server_id || item.id,
      accountType: item.account_type || item.accountType,
      accountName: item.account_name || item.accountName,
      accountIcon: item.account_icon || item.accountIcon,
    }))

    // 按 accountType 分组
    const groups = {}
    items.forEach(item => {
      const type = String(item.accountType || '0')
      if (!groups[type]) groups[type] = []
      groups[type].push(item)
    })

    // 为每个分组创建虚拟类型节点（模仿服务器 Controller 的构建逻辑）
    const roots = []
    for (const [typeValue, children] of Object.entries(groups)) {
      const typeInfo = accountTypeMap[typeValue]
      roots.push({
        accountType: typeValue,
        accountName: typeInfo ? typeInfo.accountName : ('类型' + typeValue),
        accountIcon: typeInfo ? typeInfo.accountIcon : '',
        children: children
      })
    }

    return roots
  },

  /** 从 API URL 解析出本地表名（最长前缀匹配） */
  _parseTableName(url) {
    if (!url) return null
    for (const key of SORTED_URL_KEYS) {
      if (url.startsWith(key)) {
        return URL_TABLE_MAP[key]
      }
    }
    return null
  },

  /** 解析查询条件（从 config.params 和 URL 查询字符串） */
  _parseQuery(config) {
    const query = {}
    // 1. 从 config.params 取（如果还没被 request.js 消费）
    if (config.params && typeof config.params === 'object') {
      Object.assign(query, config.params)
    }
    // 2. 从 URL 查询字符串解析（request.js 已把 params 拼到 URL 上）
    if (config.url && config.url.includes('?')) {
      const search = config.url.split('?')[1]
      if (search) {
        search.split('&').forEach(pair => {
          const eqIdx = pair.indexOf('=')
          if (eqIdx > 0) {
            const key = decodeURIComponent(pair.substring(0, eqIdx))
            const val = decodeURIComponent(pair.substring(eqIdx + 1))
            // 尝试转为数字或布尔值
            if (val === 'true') query[key] = true
            else if (val === 'false') query[key] = false
            else if (val !== '' && !isNaN(Number(val))) query[key] = Number(val)
            else query[key] = val
          }
        })
      }
    }
    // 3. 从 URL 中解析 id：如 /admin/diary/123
    const id = this._parseIdFromUrl(config.url)
    if (id) {
      query.id = id
    }
    return query
  },

  /** 从 URL 尾部解析数字 ID */
  _parseIdFromUrl(url) {
    if (!url) return null
    const match = url.match(/\/(\d+)$/)
    return match ? parseInt(match[1]) : null
  },

  /** 生成 UUID v4 */
  _uuid() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
    })
  }
}

export default offlineProxy
