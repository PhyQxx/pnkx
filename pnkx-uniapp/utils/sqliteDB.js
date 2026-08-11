/**
 * SQLite 数据库封装模块
 * 基于 plus.sqlite API，仅 APP-PLUS 环境可用
 *
 * 本地离线表结构（列名与服务器数据库一致）：
 * - px_diary             日记
 * - px_todo              待办
 * - px_bookkeeping_record   记账记录
 * - px_note              笔记
 * - px_commemoration_day 纪念日
 * - px_card              卡券（对应服务器 px_lovers_card）
 * - _sync_queue          同步队列
 * - _sync_cursor         同步游标
 */

// #ifdef APP-PLUS

const DB_NAME = '_pnkx_offline'
const DB_PATH = '_doc/pnkx_offline.db'
const DB_VERSION = 13 // 数据库版本号，结构变更时递增

let dbOpened = false
let openPromise = null  // 缓存 open promise，防止竞态重复打开

/**
 * 本地表列名白名单（与服务器数据库列名完全一致）
 * 来源：各 Mapper XML 的 resultMap 中 property→column 映射
 */
const TABLE_COLUMNS = {
  // 日记 — Mapper: date, content, rich_text, del_flag, version
  px_diary: ['id', 'title', 'mood', 'weather', 'date', 'content', 'rich_text', 'del_flag', 'version', 'client_uuid', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', '_sync_status', '_server_id', '_updated_at'],
  // 待办 — Mapper: content, performer, plan_start_time, plan_end_time, status, label, version, finish_by, finish_time
  px_todo: ['id', 'content', 'performer', 'plan_start_time', 'plan_end_time', 'status', 'label', 'version', 'client_uuid', 'create_by', 'create_time', 'update_by', 'update_time', 'finish_by', 'finish_time', 'remark', '_sync_status', '_server_id', '_updated_at'],
  // 记账记录 — Mapper: account, other_account, type, money, images, pay_time, version
  px_bookkeeping_record: ['id', 'version', 'account', 'other_account', 'type', 'money', 'images', 'pay_time', 'del_flag', 'client_uuid', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', '_sync_status', '_server_id', '_updated_at'],
  // 笔记 — Mapper: title, content, rich_text, folder, `order`, del_flag, version
  px_note: ['id', 'title', 'content', 'rich_text', 'folder', 'order', 'del_flag', 'version', 'client_uuid', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', '_sync_status', '_server_id', '_updated_at'],
  // 纪念日 — Mapper: name, icon, date, `repeat`, order_num, del_flag, version
  px_commemoration_day: ['id', 'name', 'icon', 'date', 'repeat', 'order_num', 'del_flag', 'version', 'client_uuid', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', '_sync_status', '_server_id', '_updated_at'],
  // 卡券定义（对应服务器 px_lovers_card，来自同步接口）
  px_card: ['id', 'title', 'describe', 'logo', 'thumbnail', 'money', 'number', 'del_flag', 'version', 'client_uuid', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', '_sync_status', '_server_id', '_updated_at'],
  // 卡券使用记录（来自 px_card_record + JOIN）
  px_card_record: ['id', 'card_id', 'user_id', 'instructions', 'confirm', 'confirm_time', 'score', 'score_time', 'card_name', 'user_name', 'del_flag', 'version', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', 'client_uuid', '_sync_status', '_server_id', '_updated_at'],
  // 记账分类 — Mapper: type_icon, type_name, type_level, type_parent_id, type_difference, order_num, statistics, del_flag, version
  px_bookkeeping_classification: ['id', 'version', 'type_icon', 'type_name', 'type_level', 'type_parent_id', 'type_difference', 'order_num', 'statistics', 'del_flag', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', 'client_uuid', '_sync_status', '_server_id', '_updated_at'],
  // 记账账户 — Mapper: account_type, account_icon, account_name, balance, inflow, flow_out, del_flag, version
  px_bookkeeping_account: ['id', 'version', 'account_type', 'account_icon', 'account_name', 'balance', 'inflow', 'flow_out', 'del_flag', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', 'client_uuid', '_sync_status', '_server_id', '_updated_at'],
  // 笔记文件夹 — Mapper: name, parent_id, password, `order`, del_flag, version, note_count
  px_note_folder: ['id', 'name', 'parent_id', 'password', 'order', 'del_flag', 'version', 'note_count', 'create_by', 'create_time', 'update_by', 'update_time', 'remark', 'client_uuid', '_sync_status', '_server_id', '_updated_at'],
  // 菜单（只读缓存）
  px_menu: ['id', 'menu_id', 'menu_name', 'parent_id', 'parent_name', 'path', 'icon', 'is_app', 'app_path', '_sync_status'],
  _sync_queue: ['id', 'table_name', 'method', 'url', 'payload', 'status', 'retry_count', 'created_at', 'error_msg'],
  _sync_cursor: ['table_name', 'last_cursor', 'last_sync_at']
}

/**
 * 服务端 JSON 驼峰字段名 → 本地蛇形字段名 映射
 * 仅需映射「驼峰名 ≠ 数据库列名」的字段
 */
const CAMEL_TO_SNAKE = {
  createBy: 'create_by',
  createTime: 'create_time',
  updateBy: 'update_by',
  updateTime: 'update_time',
  orderNum: 'order_num',
  clientUuid: 'client_uuid',
  delFlag: 'del_flag',
  // 笔记
  richText: 'rich_text',
  // 待办
  planStartTime: 'plan_start_time',
  planEndTime: 'plan_end_time',
  finishBy: 'finish_by',
  finishTime: 'finish_time',
  // 记账
  otherAccount: 'other_account',
  payTime: 'pay_time',
  // 记账分类
  typeIcon: 'type_icon',
  typeName: 'type_name',
  typeLevel: 'type_level',
  typeParentId: 'type_parent_id',
  typeDifference: 'type_difference',
  // 记账账户
  accountType: 'account_type',
  accountIcon: 'account_icon',
  accountName: 'account_name',
  flowOut: 'flow_out',
  // 卡券（用户卡券）
  cardId: 'card_id',
  cardNumber: 'card_number',
  // 卡券使用记录
  confirmTime: 'confirm_time',
  scoreTime: 'score_time',
  cardName: 'card_name',
  userName: 'user_name',
  // 笔记文件夹
  parentId: 'parent_id',
  noteCount: 'note_count',
  // 卡券 — 字段名与 JSON key 相同，无需映射
  // 纪念日 — date/repeat 直接就是数据库列名，无需映射
  // 菜单
  menuId: 'menu_id',
  menuName: 'menu_name',
  parentName: 'parent_name',
  isApp: 'is_app',
  appPath: 'app_path'
}

/** 无需同步的冗余字段（服务端返回但本地不需要存储） */
const IGNORE_FIELDS = new Set([
  'searchValue', 'params',     // BaseEntity 通用字段
  'typeObject', 'accountObject', 'otherAccountObject',  // 记账关联对象
  'children', 'statistics', 'noteCount',      // 分类/账户的嵌套对象和计算字段
  'nickName'                                   // sys_user JOIN 字段（已存为 user_name）
])

const sqliteDB = {
  /** 打开数据库（带防竞态锁） */
  async open() {
    if (dbOpened) return
    if (openPromise) return openPromise
    openPromise = new Promise((resolve, reject) => {
      plus.sqlite.openDatabase({
        name: DB_NAME,
        path: DB_PATH,
        success: () => {
          dbOpened = true
          openPromise = null
          console.log('[SQLite] 数据库已打开')
          resolve()
        },
        fail: (e) => {
          openPromise = null
          // 如果报 "Same Name Already Open"，说明数据库已经打开了
          if (e.code === -1402) {
            dbOpened = true
            console.log('[SQLite] 数据库已处于打开状态')
            resolve()
            return
          }
          console.error('[SQLite] 数据库打开失败:', e)
          reject(e)
        }
      })
    })
    return openPromise
  },

  /** 关闭数据库 */
  async close() {
    if (!dbOpened) return
    return new Promise((resolve, reject) => {
      plus.sqlite.closeDatabase({
        name: DB_NAME,
        success: () => {
          dbOpened = false
          resolve()
        },
        fail: reject
      })
    })
  },

  /** 执行 SQL（无返回值，用于 INSERT/UPDATE/DELETE/CREATE） */
  async executeSql(sql) {
    await this.open()
    return new Promise((resolve, reject) => {
      plus.sqlite.executeSql({
        name: DB_NAME,
        sql,
        success: resolve,
        fail: (e) => {
          console.error('[SQLite] SQL执行失败:', sql.substring(0, 100), e)
          reject(e)
        }
      })
    })
  },

  /** 查询 SQL（返回行数组） */
  async selectSql(sql) {
    await this.open()
    return new Promise((resolve, reject) => {
      plus.sqlite.selectSql({
        name: DB_NAME,
        sql,
        success: (data) => resolve(data || []),
        fail: (e) => {
          console.error('[SQLite] SQL查询失败:', sql.substring(0, 100), e)
          reject(e)
        }
      })
    })
  },

  /** 初始化所有本地表（带版本迁移） */
  async initTables() {
    // 检查数据库版本，决定是否需要迁移
    let needMigration = false
    try {
      const verRows = await this.selectSql('SELECT ver FROM _db_meta LIMIT 1')
      const currentVer = verRows?.[0]?.ver || 0
      if (currentVer < DB_VERSION) {
        needMigration = true
      }
    } catch (e) {
      // _db_meta 表不存在，首次创建
      needMigration = true
    }

    if (needMigration) {
      console.log(`[SQLite] 数据库需要迁移: 版本升级 → ${DB_VERSION}`)
      // 重置同步游标（让所有表重新全量同步，upsert 幂等不会重复）
      // 不删业务表数据，保留已有的正常记录
      try {
        await this.executeSql('DELETE FROM _sync_cursor')
      } catch (e) {
        // _sync_cursor 表可能不存在
      }
      // 删除可能结构不完整的业务表，重新创建（版本升级时列定义可能变化）
      const dropTables = [
        'px_diary', 'px_todo', 'px_bookkeeping_record',
        'px_note', 'px_note_folder', 'px_commemoration_day', 'px_card', 'px_card_record', 'px_menu',
        'px_bookkeeping_classification', 'px_bookkeeping_account',
        '_sync_cursor', '_db_meta'
      ]
      for (const t of dropTables) {
        await this.executeSql(`DROP TABLE IF EXISTS ${t}`)
      }
      // _sync_queue 保留（pending 任务不应丢失）
    }

    const tables = [
      // 日记 — 对齐服务器 px_diary 列
      `CREATE TABLE IF NOT EXISTS px_diary (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title VARCHAR(200),
        mood VARCHAR(10),
        weather VARCHAR(10),
        date VARCHAR(20),
        content TEXT,
        rich_text TEXT,
        del_flag TINYINT DEFAULT 0,
        version VARCHAR(20),
        client_uuid VARCHAR(36) UNIQUE,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 待办 — 对齐服务器 px_to_do 列
      `CREATE TABLE IF NOT EXISTS px_todo (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        content TEXT,
        performer VARCHAR(50),
        plan_start_time VARCHAR(20),
        plan_end_time VARCHAR(20),
        status TINYINT DEFAULT 0,
        label VARCHAR(50),
        version VARCHAR(20),
        client_uuid VARCHAR(36) UNIQUE,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        finish_by VARCHAR(50),
        finish_time VARCHAR(20),
        remark VARCHAR(500),
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 记账记录 — 对齐服务器 px_bookkeeping_record 列
      `CREATE TABLE IF NOT EXISTS px_bookkeeping_record (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        version VARCHAR(20),
        account INTEGER,
        other_account INTEGER,
        type INTEGER,
        money VARCHAR(20),
        images VARCHAR(500),
        pay_time VARCHAR(20),
        del_flag TINYINT DEFAULT 0,
        client_uuid VARCHAR(36) UNIQUE,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 笔记 — 对齐服务器 px_note 列
      `CREATE TABLE IF NOT EXISTS px_note (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title VARCHAR(200),
        content TEXT,
        rich_text TEXT,
        folder INTEGER,
        \`order\` INTEGER,
        del_flag TINYINT DEFAULT 0,
        version VARCHAR(20),
        client_uuid VARCHAR(36) UNIQUE,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 纪念日 — 对齐服务器 px_commemoration_day 列
      `CREATE TABLE IF NOT EXISTS px_commemoration_day (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name VARCHAR(100),
        icon VARCHAR(100),
        date VARCHAR(20),
        \`repeat\` TINYINT DEFAULT 0,
        order_num INTEGER,
        del_flag TINYINT DEFAULT 0,
        version VARCHAR(20),
        client_uuid VARCHAR(36) UNIQUE,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 卡券定义 — 对齐服务器 px_lovers_card 列（同步接口返回）
      `CREATE TABLE IF NOT EXISTS px_card (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title VARCHAR(100),
        \`describe\` TEXT,
        logo VARCHAR(200),
        thumbnail VARCHAR(200),
        money INTEGER,
        \`number\` INTEGER,
        del_flag TINYINT DEFAULT 0,
        version VARCHAR(20),
        client_uuid VARCHAR(36) UNIQUE,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 卡券使用记录（来自 px_card_record + JOIN）
      `CREATE TABLE IF NOT EXISTS px_card_record (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        card_id INTEGER,
        user_id INTEGER,
        instructions TEXT,
        confirm TINYINT DEFAULT 0,
        confirm_time VARCHAR(20),
        score INTEGER DEFAULT 0,
        score_time VARCHAR(20),
        card_name VARCHAR(100),
        user_name VARCHAR(100),
        del_flag TINYINT DEFAULT 0,
        version VARCHAR(20),
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        client_uuid VARCHAR(36) UNIQUE,
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 菜单（只读缓存）
      `CREATE TABLE IF NOT EXISTS px_menu (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        menu_id INTEGER,
        menu_name VARCHAR(100),
        parent_id INTEGER,
        parent_name VARCHAR(100),
        path VARCHAR(200),
        icon VARCHAR(100),
        is_app VARCHAR(5),
        app_path VARCHAR(255),
        _sync_status TINYINT DEFAULT 1
      )`,

      // 记账分类 — 对齐服务器 px_bookkeeping_classification 列
      `CREATE TABLE IF NOT EXISTS px_bookkeeping_classification (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        version VARCHAR(20),
        type_icon VARCHAR(200),
        type_name VARCHAR(100),
        type_level VARCHAR(5),
        type_parent_id INTEGER,
        type_difference VARCHAR(5),
        order_num INTEGER,
        statistics VARCHAR(100),
        del_flag TINYINT DEFAULT 0,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        client_uuid VARCHAR(36) UNIQUE,
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 记账账户 — 对齐服务器 px_bookkeeping_account 列
      `CREATE TABLE IF NOT EXISTS px_bookkeeping_account (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        version VARCHAR(20),
        account_type VARCHAR(50),
        account_icon VARCHAR(200),
        account_name VARCHAR(100),
        balance VARCHAR(20),
        inflow VARCHAR(20),
        flow_out VARCHAR(20),
        del_flag TINYINT DEFAULT 0,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        client_uuid VARCHAR(36) UNIQUE,
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,

      // 笔记文件夹 — 对齐服务器 px_note_folder 列
      `CREATE TABLE IF NOT EXISTS px_note_folder (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name VARCHAR(100),
        parent_id INTEGER,
        password VARCHAR(100),
        \`order\` INTEGER,
        del_flag TINYINT DEFAULT 0,
        version VARCHAR(20),
        note_count INTEGER,
        create_by VARCHAR(50),
        create_time DATETIME,
        update_by VARCHAR(50),
        update_time DATETIME,
        remark VARCHAR(500),
        client_uuid VARCHAR(36) UNIQUE,
        _sync_status TINYINT DEFAULT 0,
        _server_id INTEGER,
        _updated_at DATETIME
      )`,
      `CREATE TABLE IF NOT EXISTS _sync_queue (
        id VARCHAR(36) PRIMARY KEY,
        table_name VARCHAR(50),
        method VARCHAR(10),
        url VARCHAR(200),
        payload TEXT,
        status VARCHAR(20) DEFAULT 'pending',
        retry_count INTEGER DEFAULT 0,
        created_at DATETIME,
        error_msg TEXT
      )`,

      // 同步游标（记录每个模块最后同步时间）
      `CREATE TABLE IF NOT EXISTS _sync_cursor (
        table_name VARCHAR(50) PRIMARY KEY,
        last_cursor DATETIME,
        last_sync_at DATETIME
      )`,

      // 数据库版本元信息
      `CREATE TABLE IF NOT EXISTS _db_meta (
        ver INTEGER PRIMARY KEY
      )`
    ]

    for (const sql of tables) {
      await this.executeSql(sql)
    }

    // 写入/更新版本号
    await this.executeSql(`DELETE FROM _db_meta`)
    await this.executeSql(`INSERT INTO _db_meta (ver) VALUES (${DB_VERSION})`)

    console.log('[SQLite] 离线表初始化完成, version=' + DB_VERSION)
  },

  // ──────────── 字段映射与过滤 ────────────

  /**
   * 将服务端返回的数据转换为本地表可接受的格式
   * 1. 驼峰字段名 → 蛇形字段名
   * 2. 过滤掉本地表不存在的列
   */
  _normalizeData(tableName, data) {
    const validCols = TABLE_COLUMNS[tableName]
    if (!validCols) return data  // 未知表不做过滤

    const result = {}
    for (const [key, value] of Object.entries(data)) {
      if (value === undefined || value === null) continue

      // 跳过已知的冗余字段
      if (IGNORE_FIELDS.has(key)) continue

      // 驼峰转蛇形
      const snakeKey = CAMEL_TO_SNAKE[key] || key

      // 只保留表中存在的列
      if (validCols.includes(snakeKey)) {
        result[snakeKey] = value
      }
    }
    return result
  },

  // ──────────── 通用 CRUD 操作 ────────────

  /**
   * 将 where 条件的 key 从驼峰转为蛇形，并过滤掉非表列的参数
   * 用于 offlineProxy 传入的 API 查询参数（如 planEndTime → plan_end_time）
   */
  _normalizeWhereKeys(tableName, where = {}) {
    const validCols = TABLE_COLUMNS[tableName]
    if (!validCols) return where  // 未知表不过滤

    // 非列名的辅助参数
    const SKIP_KEYS = new Set(['pageNum', 'pageSize', 'orderBy', 'isAsc', 'beginTime', 'endTime'])

    const result = {}
    for (const [key, value] of Object.entries(where)) {
      if (value === undefined || value === null || value === '') continue
      if (SKIP_KEYS.has(key)) continue

      // 先查已知映射，再做通用驼峰→蛇形转换
      const snakeKey = CAMEL_TO_SNAKE[key] || key.replace(/[A-Z]/g, l => `_${l.toLowerCase()}`)

      // 只保留表中存在的列
      if (validCols.includes(snakeKey)) {
        result[snakeKey] = value
      }
    }
    return result
  },

  /** 查询 */
  async query(tableName, where = {}, orderBy = '_updated_at DESC', limit = 0) {
    const normalizedWhere = this._normalizeWhereKeys(tableName, where)
    let sql = `SELECT * FROM ${tableName} WHERE 1=1`
    // 自动过滤已删除记录（含 del_flag 列的表）
    const validCols = TABLE_COLUMNS[tableName]
    if (validCols && validCols.includes('del_flag') && normalizedWhere.del_flag === undefined) {
      sql += ' AND `del_flag` = 0'
    }
    const conditions = this._buildWhere(normalizedWhere)
    if (conditions) sql += ` AND ${conditions}`
    if (orderBy) sql += ` ORDER BY ${orderBy}`
    if (limit > 0) sql += ` LIMIT ${limit}`
    return this.selectSql(sql)
  },

  /** 根据 ID 查询 */
  async getById(tableName, id) {
    const rows = await this.selectSql(`SELECT * FROM ${tableName} WHERE \`id\` = ${this._escapeValue(id)} LIMIT 1`)
    return rows?.[0] || null
  },

  /** 插入 */
  async insert(tableName, data) {
    // 过滤无效列名
    const filtered = this._normalizeData(tableName, data)
    const keys = Object.keys(filtered).filter(k => filtered[k] !== undefined)
    if (keys.length === 0) return
    const values = keys.map(k => this._escapeValue(filtered[k]))
    const sql = `INSERT INTO ${tableName} (${keys.map(k => '`' + k + '`').join(',')}) VALUES (${values.join(',')})`
    await this.executeSql(sql)
  },

  /** 更新 */
  async update(tableName, data, where, keepSyncStatus = false) {
    const filtered = this._normalizeData(tableName, data)
    const sets = Object.entries(filtered)
      .filter(([k, v]) => v !== undefined && k !== 'id')
      .map(([k, v]) => '`' + k + '` = ' + this._escapeValue(v))
      .join(', ')

    if (!sets) return

    const whereStr = this._buildWhere(where)
    // keepSyncStatus=true 时不下推 _sync_status=0（用于下行同步 upsert 场景）
    const syncSuffix = keepSyncStatus ? '' : ', `_sync_status` = 0'
    const sql = `UPDATE ${tableName} SET ${sets}${syncSuffix}, \`_updated_at\` = '${this._now()}' WHERE ${whereStr}`
    await this.executeSql(sql)
  },

  /** 删除 */
  async remove(tableName, where) {
    const whereStr = this._buildWhere(where)
    await this.executeSql(`DELETE FROM ${tableName} WHERE ${whereStr}`)
  },

  /** 统计行数 */
  async count(tableName, where = {}) {
    const normalizedWhere = this._normalizeWhereKeys(tableName, where)
    let sql = `SELECT COUNT(*) as count FROM ${tableName} WHERE 1=1`
    const conditions = this._buildWhere(normalizedWhere)
    if (conditions) sql += ` AND ${conditions}`
    const result = await this.selectSql(sql)
    return result?.[0]?.count || 0
  },

  /**
   * 批量 upsert（同步缓存用）
   * @returns {{ total: number, failed: number }} 成功/失败计数
   */
  async upsertBatch(tableName, rows) {
    if (!rows || rows.length === 0) return { total: 0, failed: 0 }
    let failed = 0
    for (const row of rows) {
      try {
        // 检查是否已存在（按 _server_id 或 client_uuid）
        // 注意：服务器返回 clientUuid（驼峰），需要兼容两种字段名
        const clientUuid = row.client_uuid || row.clientUuid
        let existing = []
        if (row.id) {
          existing = await this.selectSql(
            `SELECT id FROM ${tableName} WHERE \`_server_id\` = ${this._escapeValue(row.id)} LIMIT 1`
          )
        }
        if ((!existing || existing.length === 0) && clientUuid) {
          existing = await this.selectSql(
            `SELECT id FROM ${tableName} WHERE \`client_uuid\` = ${this._escapeValue(clientUuid)} LIMIT 1`
          )
        }

        const record = {
          ...row,
          _server_id: row.id || row._server_id,
          _sync_status: row._sync_status ?? 1,
          _updated_at: row._updated_at || this._now()
        }

        if (existing && existing.length > 0) {
          // 更新（保留本地 id，保持同步状态）
          const localId = existing[0].id
          delete record.id
          await this.update(tableName, record, { id: localId }, true)
        } else {
          // 插入前再检查：是否已有 client_uuid 相同但 _server_id 为 null 的离线记录
          // 这种情况发生在上行同步的 _syncOneTask 更新 _server_id 失败时
          if (clientUuid && row.id) {
            const offlineExisting = await this.selectSql(
              `SELECT id FROM ${tableName} WHERE \`client_uuid\` = ${this._escapeValue(clientUuid)} AND \`_server_id\` IS NULL LIMIT 1`
            )
            if (offlineExisting && offlineExisting.length > 0) {
              // 合并：更新离线记录的 _server_id 和其他字段
              const localId = offlineExisting[0].id
              delete record.id
              await this.update(tableName, record, { id: localId }, true)
              continue
            }
          }
          // 插入
          delete record.id  // 让自增 ID 生效
          await this.insert(tableName, record)
        }
      } catch (e) {
        console.warn('[SQLite] upsertBatch 单条失败:', e)
        failed++
      }
    }
    return { total: rows.length, failed }
  },

  /** 获取待同步记录数 */
  async getPendingCount(tableName) {
    return this.count(tableName, { _sync_status: 0 })
  },

  /** 获取所有模块的待同步总数 */
  async getAllPendingCount() {
    const tables = [
      'px_diary', 'px_todo', 'px_bookkeeping_record',
      'px_note', 'px_commemoration_day', 'px_card'
    ]
    let total = 0
    for (const t of tables) {
      try {
        total += await this.getPendingCount(t)
      } catch (e) {
        // 表可能尚未创建，忽略
      }
    }
    // 加上队列中的 pending 数
    try {
      const queueCount = await this.count('_sync_queue', { status: 'pending' })
      total += queueCount
    } catch (e) {
      // _sync_queue 可能尚未创建
    }
    return total
  },

  // ──────────── 辅助方法 ────────────

  /** 构建 WHERE 条件字符串 */
  _buildWhere(where) {
    return Object.entries(where)
      .filter(([k, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => '`' + k + '` = ' + this._escapeValue(v))
      .join(' AND ')
  },

  /** SQL 值转义 */
  _escapeValue(v) {
    if (v === null || v === undefined) return 'NULL'
    if (typeof v === 'number') return String(v)
    if (typeof v === 'boolean') return v ? '1' : '0'
    // 字符串：单引号转义
    return "'" + String(v).replace(/'/g, "''") + "'"
  },

  /** 获取当前时间 ISO 字符串 */
  _now() {
    return new Date().toISOString()
  }
}

// #endif

// #ifndef APP-PLUS
// 非 APP 环境提供空实现，避免 import 报错
const sqliteDB = {
  open: async () => {},
  close: async () => {},
  executeSql: async () => {},
  selectSql: async () => [],
  initTables: async () => {},
  query: async () => [],
  getById: async () => null,
  insert: async () => {},
  update: async () => {},
  remove: async () => {},
  count: async () => 0,
  upsertBatch: async () => {},
  getPendingCount: async () => 0,
  getAllPendingCount: async () => 0
}
// #endif

export default sqliteDB
