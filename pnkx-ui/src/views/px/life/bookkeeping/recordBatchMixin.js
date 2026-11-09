import { aiBatchParse, addBatchRecord, importWechat } from '@/api/px/life/bookkeeping/record'

/**
 * 记账「批量记账 + AI 智能导入」逻辑
 * 从 record.vue 抽出：批量弹窗状态、行编辑、AI 批量解析与"同上"语义提交。
 * 依赖父组件提供：parseTime（全局属性）、listRecord()（提交成功后刷新）。
 */
export default {
  data() {
    return {
      // 批量记账
      batchVisible: false,
      batchLoading: false,
      batchRecordList: [],
      // AI智能导入
      aiImportVisible: false,
      aiImportText: '',
      aiParsing: false,
      wechatLoading: false,
      wechatProgress: { completed: 0, total: 0 }
    }
  },
  methods: {
    async importWechatFile(event) {
      const file = event.target.files[0]
      event.target.value = ''
      if (!file) return
      this.wechatLoading = true
      try {
        const res = await importWechat(file)
        if (this.batchRecordList.length === 1 && !this.batchRecordList[0].money) this.batchRecordList = []
        const firstImportedIndex = this.batchRecordList.length
        for (const row of res.data) {
          if (row.sourceOrder && this.batchRecordList.some(r => r.sourceOrder === row.sourceOrder)) {
            row.selected = false
            row.importStatus = '本次预览中已有相同单号'
          }
          this.batchRecordList.push(row)
        }
        this.wechatLoading = false
        await this.$nextTick()
        // 从响应式表格中读取代理对象，不能直接修改接口返回的原始对象。
        await this.recognizeWechat(this.batchRecordList.slice(firstImportedIndex).filter(row => row.selected))
        this.$message.success('账单导入及识别完成，请核对分类和账户后手动保存')
      } finally { this.wechatLoading = false }
    },
    // 使用选项中的原始 ID，兼容 AI 返回字符串 ID 和账户名称的旧接口。
    matchImportOption(options, id) {
      if (id == null || id === '') return ''
      for (const option of options || []) {
        if (option.children?.length) {
          const match = this.matchImportOption(option.children, id)
          if (match !== '') return match
        } else if (String(option.id) === String(id)) return option.id
      }
      return ''
    },
    async recognizeWechat(rows) {
      if (this.aiParsing) return
      const targets = Array.isArray(rows) ? rows : this.batchRecordList.filter(r => r.imported && r.selected)
      if (!targets.length) return
      this.aiParsing = true
      this.wechatProgress = { completed: 0, total: targets.length }
      targets.forEach(row => { row.importStatus = '等待识别' })
      try {
        for (const row of targets) {
          row.importStatus = 'AI识别中…'
          try {
            const res = await aiBatchParse(JSON.stringify({ typeDifference: row.typeDifference, money: row.money, payTime: row.payTime, remark: row.source }))
            const suggestion = res.data?.list?.[0]
            if (suggestion) {
              // 不覆盖用户已经手动填写的值；AI 只提供分类和账户建议。
              if (!row.type) row.type = this.matchImportOption(this.typeList, suggestion.typeObject?.id ?? suggestion.type)
              if (!row.account) row.account = this.matchImportOption(this.accountList, suggestion.accountObject?.id ?? suggestion.account)
              row.importStatus = row.type && row.account ? 'AI建议，请核对' : '部分未识别，请手动补齐'
            } else row.importStatus = '未识别，请手动选择'
          } catch { row.importStatus = '识别失败，请手动选择' }
          this.wechatProgress.completed++
          // 每一条响应都触发界面更新，然后再请求下一条。
          await this.$nextTick()
        }
      } finally { this.aiParsing = false }
    },
    /**
     * 打开批量记账弹窗
     */
    openBatchRecord() {
      this.batchRecordList = []
      this.addBatchRow()
      this.batchVisible = true
    },
    /**
     * 添加一行批量记录
     */
    addBatchRow() {
      let newRow = {
        typeDifference: '1',
        payTime: this.parseTime(new Date()),
        type: '',
        account: '',
        otherAccount: '',
        money: '',
        remark: ''
      }

      // 如果已有记录，类型沿用上一条；时间、分类、账户显示"同上"，提交时再解析
      if (this.batchRecordList.length > 0) {
        const lastRow = this.batchRecordList[this.batchRecordList.length - 1]
        newRow = {
          ...newRow,
          typeDifference: lastRow.typeDifference,
          payTime: ''
        }
      }

      this.batchRecordList.push(newRow)
    },
    /**
     * 处理批量类型的列标签
     */
    batchAccountLabel(row) {
      if (!row) return '账户'
      return row.typeDifference === '2' ? '转出账户' : '账户'
    },
    /**
     * 处理批量行类型变化
     */
    handleBatchTypeChange(row) {
      row.type = ''
      row.account = ''
      row.otherAccount = ''
    },
    /**
     * AI批量解析
     */
    handleAiBatchParse() {
      if (!this.aiImportText.trim()) {
        this.$message.warning('请输入账单描述文本')
        return
      }
      this.aiParsing = true
      aiBatchParse(this.aiImportText).then(res => {
        const list = res.data.list || []
        if (list.length > 0) {
          // 清除初始的空行（如果只有一行且为空）
          if (this.batchRecordList.length === 1 && !this.batchRecordList[0].money) {
            this.batchRecordList = []
          }
          list.forEach(item => {
            this.batchRecordList.push({
              typeDifference: item.typeDifference || '1',
              payTime: item.payTime || this.parseTime(new Date()),
              type: this.matchImportOption(this.typeList, item.typeObject?.id ?? item.type),
              account: this.matchImportOption(this.accountList, item.accountObject?.id ?? item.account),
              otherAccount: item.otherAccount || '',
              money: item.money || '',
              remark: item.remark || ''
            })
          })
          this.$message.success(`成功解析 ${list.length} 条记录`)
          this.aiImportVisible = false
          this.aiImportText = ''
        } else {
          this.$message.warning('未解析出有效记账数据')
        }
      }).finally(() => {
        this.aiParsing = false
      })
    },
    /**
     * 提交批量记录
     */
    submitBatch() {
      if (this.batchRecordList.length === 0) {
        this.$message.warning('请至少添加一条记录')
        return
      }

      // 空的时间、分类、账户、转入账户表示"同上"，转换成接口需要的实际值
      const resolvedList = []
      this.batchRecordList.filter(item => !item.imported || item.selected).forEach((item, index) => {
        const previous = item.imported ? undefined : resolvedList[index - 1]
        const isTransfer = item.typeDifference === '2'
        resolvedList.push({
          ...item,
          payTime: item.payTime || previous?.payTime || '',
          type: isTransfer ? 0 : (item.type || previous?.type || ''),
          account: item.account || previous?.account || '',
          otherAccount: isTransfer ? (item.otherAccount || previous?.otherAccount || '') : ''
        })
      })

      if (!resolvedList.length) { this.$message.warning("请勾选需要保存的记录"); return }
      // 校验
      for (let i = 0; i < resolvedList.length; i++) {
        const item = resolvedList[i]
        if (!Number.isFinite(Number(item.money)) || Number(item.money) <= 0) {
          this.$message.warning(`第 ${i + 1} 行未填写金额`)
          return
        }
        if (!item.payTime) {
          this.$message.warning(`第 ${i + 1} 行未选择时间`)
          return
        }
        if (item.typeDifference !== '2' && !item.type) {
          this.$message.warning(`第 ${i + 1} 行未选择分类`)
          return
        }
        if (!item.account) {
          this.$message.warning(`第 ${i + 1} 行未选择账户`)
          return
        }
        if (item.typeDifference === '2' && !item.otherAccount) {
          this.$message.warning(`第 ${i + 1} 行未选择转入账户`)
          return
        }
        if (item.typeDifference === '2' && item.account === item.otherAccount) {
          this.$message.warning(`第 ${i + 1} 行转出和转入账户不能相同`)
          return
        }
      }

      this.batchLoading = true
      addBatchRecord(resolvedList).then(() => {
        this.$notify.success('批量新增记录成功')
        this.batchVisible = false
        this.listRecord()
      }).finally(() => {
        this.batchLoading = false
      })
    }
  }
}
