import { aiBatchParse, addBatchRecord } from '@/api/px/life/bookkeeping/record'

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
      aiParsing: false
    }
  },
  methods: {
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
              type: item.type || '',
              account: item.account || '',
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
      this.batchRecordList.forEach((item, index) => {
        const previous = resolvedList[index - 1]
        const isTransfer = item.typeDifference === '2'
        resolvedList.push({
          ...item,
          payTime: item.payTime || previous?.payTime || '',
          type: isTransfer ? 0 : (item.type || previous?.type || ''),
          account: item.account || previous?.account || '',
          otherAccount: isTransfer ? (item.otherAccount || previous?.otherAccount || '') : ''
        })
      })

      // 校验
      for (let i = 0; i < resolvedList.length; i++) {
        const item = resolvedList[i]
        if (!item.money) {
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
