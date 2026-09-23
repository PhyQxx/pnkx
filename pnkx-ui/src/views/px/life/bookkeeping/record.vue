<!--
 * @File: record
 * @Author: PHY
 * @Date: 2021-11-05 19:51
 * @Description: 记账记录 - Modern UI Refactored
-->
<template>
  <div class="bookkeeping-page">
    <el-tabs v-model="activeTab" class="bookkeeping-tabs">
      <el-tab-pane label="记账记录" name="record">
        <div class="bookkeeping-record-container">
    <!-- 左侧列表面板 -->
    <aside class="sidebar">
      <!-- 搜索栏 -->
      <div class="search-wrapper">
        <div class="search-box">
          <svg-icon icon-class="搜索" class="search-icon" />
          <input
            v-model="queryForm.searchValue"
            placeholder="搜索记录..."
            class="search-input"
            @keyup.enter="listRecord"
          >
        </div>
        <el-button
            class="export-btn"
            size="small"
            text
            :loading="exportLoading"
            title="按当前筛选条件导出 Excel"
            @click="handleExport"
        >
          <svg-icon icon-class="download" style="margin-right: 2px" />导出
        </el-button>
      </div>

      <!-- 筛选区域 -->
      <div class="filter-section">
        <el-cascader
          v-model="queryForm.type"
          :options="typeList"
          :props="typeProps"
          :show-all-levels="false"
          placeholder="选择分类"
          size="small"
          clearable
          class="filter-cascader"
          @change="selectTypeTarget"
        />
        <el-cascader
          v-model="queryForm.account"
          :options="accountList"
          :props="accountProps"
          :show-all-levels="false"
          placeholder="选择账户"
          size="small"
          clearable
          class="filter-cascader"
          @change="selectAccountTarget"
        />
        <el-date-picker
          v-model="queryForm.payTime"
          value-format="YYYY-MM"
          type="month"
          placeholder="选择月份"
          size="small"
          class="filter-date"
          @change="listRecord"
        />
      </div>

      <!-- 月度汇总 -->
      <div class="month-summary">
        <div class="summary-item">
          <span class="summary-label">收入</span>
          <span class="summary-value income">{{ moneyFilter(MoneyTotal[0]) }}</span>
        </div>
        <div class="summary-divider" />
        <div class="summary-item">
          <span class="summary-label">支出</span>
          <span class="summary-value expenditure">{{ moneyFilter(MoneyTotal[1]) }}</span>
        </div>
      </div>

      <!-- 记录列表 -->
      <div
        v-loading="listLoading"
        class="record-list"
        @contextmenu.prevent.stop="handleContextMenu($event, null)"
      >
        <div v-if="recordData.length < 1" class="empty-state">
          <svg-icon icon-class="账本" class="empty-icon" />
          <p>暂无记录</p>
          <p class="hint">右键或点击右下角按钮新增</p>
        </div>

        <transition-group v-else name="item-list" tag="div" class="record-items">
          <div
            v-for="(item, index) in recordData"
            :key="item.id"
            class="record-card"
            :class="{ active: active && active.id === item.id }"
            :style="{ animationDelay: `${index * 0.03}s` }"
            @click="handleSelect(item)"
            @contextmenu.prevent.stop="handleContextMenu($event, item)"
          >
            <div class="card-icon-wrapper" :class="billTypeClass(item)">
              <svg-icon :icon-class="item.typeObject && item.typeObject.typeIcon || '账本'" class="card-icon" />
            </div>
            <div class="card-info">
              <div class="card-title">{{ item.typeObject && item.typeObject.typeName }}</div>
              <div v-if="item.remark" class="card-remark">{{ item.remark }}</div>
              <div class="card-meta">
                <span class="card-account">{{ item.accountObject && item.accountObject.accountName }}</span>
                <span class="card-time">{{ timeFilter(item.payTime) }}</span>
              </div>
            </div>
            <div class="card-amount" :class="{ income: billType(item) === '收入', expenditure: billType(item) === '支出' }">
              {{ billType(item) === '收入' ? '+' : '-' }}{{ item.money }}
            </div>
          </div>
        </transition-group>
      </div>

      <!-- 分页 -->
      <div class="sidebar-pagination">
        <el-pagination
          small
          layout="total, prev, pager, next"
          :current-page="queryForm.pageNum"
          :page-size="queryForm.pageSize"
          :total="total"
          @current-change="handleCurrentChange"
        />
      </div>
    </aside>

    <!-- 右侧详情面板 -->
    <main v-loading="loading" class="detail-area">
      <!-- 空状态 -->
      <div v-if="!active" class="empty-detail">
        <svg-icon icon-class="账本" class="empty-detail-icon" />
        <p>选择一条记录查看详情</p>
      </div>

      <!-- 记录详情 -->
      <div v-else class="record-detail">
        <div class="detail-header">
          <div class="detail-icon-wrapper" :class="billTypeClass(active)">
            <svg-icon :icon-class="active.typeObject && active.typeObject.typeIcon || '账本'" class="detail-icon" />
          </div>
          <div class="detail-title-section">
            <div class="detail-type-badge" :class="billTypeClass(active)">{{ billType(active) }}</div>
            <h2 class="detail-amount">
              {{ billType(active) === '收入' ? '+' : '-' }}{{ active.money }}
              <span class="detail-unit">元</span>
            </h2>
          </div>
          <div class="detail-actions">
            <el-button type="primary" size="small" @click="getRecord(active.id)">
              <svg-icon icon-class="编辑" class="action-icon" /> 编辑
            </el-button>
            <el-button type="danger" size="small" @click="delRecord(active.id)">
              <svg-icon icon-class="删除" class="action-icon" /> 删除
            </el-button>
          </div>
        </div>

        <div class="detail-body">
          <div class="detail-info-grid">
            <div class="info-item">
              <span class="info-label">分类</span>
              <span class="info-value">{{ active.typeObject && active.typeObject.typeName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">账户</span>
              <span class="info-value">{{ active.accountObject && active.accountObject.accountName }}</span>
            </div>
            <div v-if="active.otherAccountObject" class="info-item">
              <span class="info-label">商家/对方账户</span>
              <span class="info-value">{{ active.otherAccountObject.accountName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">时间</span>
              <span class="info-value">{{ timeFilter(active.payTime) }}</span>
            </div>
            <div v-if="active.commemorationDay" class="info-item">
              <span class="info-label">关联纪念日</span>
              <span class="info-value">{{ active.commemorationDay.name }}</span>
            </div>
          </div>

          <div v-if="active.remark" class="detail-remark">
            <h4>备注</h4>
            <p>{{ active.remark }}</p>
          </div>
        </div>
      </div>
    </main>

    <!-- 浮动新增按钮组 -->
    <div class="fab-group">
      <el-dropdown trigger="click" placement="top-end" @command="handleFabCommand">
        <div class="fab-action main" title="新增记录">
          <el-icon><Plus /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="fab-dropdown-menu">
            <el-dropdown-item command="single">
              <el-icon><EditPen /></el-icon> 普通记账
            </el-dropdown-item>
            <el-dropdown-item command="batch">
              <el-icon><Files /></el-icon> 批量记账
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 新增/修改记录弹窗 -->
    <el-dialog
      :title="recordTitle"
      v-model="record"
      width="520px"
      custom-class="modern-dialog"
      :modal-append-to-body="true"
      @closed="resetRecordForm"
    >
      <el-tabs v-model="recordForm.typeDifference" class="modern-tabs" @tab-click="init">
        <el-tab-pane label="支出" name="1" :disabled="Boolean(recordForm.id)" />
        <el-tab-pane label="收入" name="0" :disabled="Boolean(recordForm.id)" />
        <el-tab-pane label="转账" name="2" :disabled="Boolean(recordForm.id)" />
      </el-tabs>
      <el-form ref="recordForm" :model="recordForm" :rules="rules" label-position="top" class="modern-form">
        <el-form-item v-if="recordForm.typeDifference !== '2'" label="分类" prop="type">
          <el-cascader
            :key="1"
            v-model="recordForm.type"
            :options="typeList"
            :props="typeProps"
            :show-all-levels="false"
            placeholder="请选择分类"
            style="width: 100%"
            @change="selectTypeTargetAdd"
          />
        </el-form-item>
        <el-form-item v-if="recordForm.typeDifference !== '2'" label="账户" prop="account">
          <el-cascader
            :key="2"
            v-model="recordForm.account"
            :options="accountList"
            :props="accountProps"
            :show-all-levels="false"
            placeholder="请选择账户"
            style="width: 100%"
            @change="selectAccountTargetAdd"
          />
        </el-form-item>
        <el-form-item v-if="recordForm.typeDifference === '2'" label="转出账户" prop="account">
          <el-cascader
            :key="3"
            v-model="recordForm.account"
            :options="accountList"
            :props="accountProps"
            :show-all-levels="false"
            placeholder="请选择转出账户"
            style="width: 100%"
            @change="selectAccountTargetAdd"
          />
        </el-form-item>
        <el-form-item v-if="recordForm.typeDifference === '2'" label="转入账户" prop="otherAccount">
          <el-cascader
            :key="4"
            v-model="recordForm.otherAccount"
            :options="accountList"
            :props="accountProps"
            :show-all-levels="false"
            placeholder="请选择转入账户"
            style="width: 100%"
            @change="selectOtherAccountTargetAdd"
          />
        </el-form-item>
        <el-form-item label="金额" prop="money">
          <div class="money-input-wrapper">
            <span class="money-preview">{{ recordForm.money }}</span>
            <el-input v-model="money" class="money-input" placeholder="请输入金额" />
          </div>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="recordForm.payTime"
            value-format="YYYY-MM-DD HH:mm:ss"
            type="datetime"
            placeholder="请选择时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="recordForm.typeDifference === '1'" label="关联纪念日">
          <el-select
            v-model="recordForm.commemorationDayId"
            clearable
            filterable
            placeholder="可选；礼物分类保存时也会自动匹配"
            style="width: 100%"
          >
            <el-option
              v-for="day in commemorationDayList"
              :key="day.id"
              :label="day.name"
              :value="day.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="recordForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="record = false">取消</el-button>
          <el-button type="primary" class="btn-confirm" @click="addRecord">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量记账弹窗 -->
    <el-dialog
      title="批量记账"
      v-model="batchVisible"
      width="90%"
      top="5vh"
      custom-class="modern-dialog batch-dialog"
      :modal-append-to-body="true"
    >
      <div class="batch-toolbar">
        <el-button type="primary" size="small" @click="addBatchRow">
          <el-icon><Plus /></el-icon> 添加一行
        </el-button>
        <el-button type="success" size="small" @click="aiImportVisible = true">
          <svg-icon icon-class="AI-Robot" /> AI 智能导入
        </el-button>
        <el-button type="danger" size="small" plain @click="batchRecordList = []">清空</el-button>
      </div>

      <el-table :data="batchRecordList" height="50vh" class="modern-table">
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-select v-model="scope.row.typeDifference" size="small" @change="handleBatchTypeChange(scope.row)">
              <el-option label="支出" value="1" />
              <el-option label="收入" value="0" />
              <el-option label="转账" value="2" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="200">
          <template #default="scope">
            <el-date-picker
              v-model="scope.row.payTime"
              value-format="YYYY-MM-DD HH:mm:ss"
              type="datetime"
              size="small"
              :placeholder="scope.$index === 0 ? '时间' : '同上'"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="分类" width="160">
          <template #default="scope">
            <el-cascader
              v-if="scope.row.typeDifference !== '2'"
              v-model="scope.row.type"
              :options="typeList"
              :props="typeProps"
              :show-all-levels="false"
              :placeholder="scope.$index === 0 ? '分类' : '同上'"
              size="small"
              @change="(val) => scope.row.type = val[1]"
            />
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="batchAccountLabel(batchRecordList[0])" width="160">
          <template #default="scope">
            <el-cascader
              v-model="scope.row.account"
              :options="accountList"
              :props="accountProps"
              :show-all-levels="false"
              :placeholder="scope.$index === 0 ? '账户' : '同上'"
              size="small"
              @change="(val) => scope.row.account = val[1]"
            />
          </template>
        </el-table-column>
        <el-table-column label="转入账户" width="160">
          <template #default="scope">
            <el-cascader
              v-if="scope.row.typeDifference === '2'"
              v-model="scope.row.otherAccount"
              :options="accountList"
              :props="accountProps"
              :show-all-levels="false"
              :placeholder="scope.$index === 0 ? '转入账户' : '同上'"
              size="small"
              @change="(val) => scope.row.otherAccount = val[1]"
            />
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="scope">
            <el-input v-model="scope.row.money" size="small" placeholder="金额" />
          </template>
        </el-table-column>
        <el-table-column label="备注">
          <template #default="scope">
            <el-input v-model="scope.row.remark" size="small" placeholder="备注" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" fixed="right">
          <template #default="scope">
            <el-button type="danger" link @click="batchRecordList.splice(scope.$index, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="batchVisible = false">取消</el-button>
          <el-button type="primary" :loading="batchLoading" @click="submitBatch">
            保存 {{ batchRecordList.length }} 条记录
          </el-button>
        </div>
      </template>

      <!-- AI导入内部弹窗 -->
      <el-dialog
        title="AI 智能解析"
        v-model="aiImportVisible"
        width="500px"
        append-to-body
        custom-class="modern-dialog"
      >
        <div class="ai-import-tip">
          <p>粘贴一段或多段自然语言描述，AI 将自动识别金额、分类和账户。</p>
          <p class="example">例如：昨天下午肯德基30元招行信用卡；今天早饭6元现金</p>
        </div>
        <el-input
          v-model="aiImportText"
          type="textarea"
          :rows="6"
          placeholder="在此输入账单描述文本..."
          class="ai-import-input"
        />
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="aiImportVisible = false">取消</el-button>
            <el-button type="success" :loading="aiParsing" @click="handleAiBatchParse">开始解析</el-button>
          </div>
        </template>
      </el-dialog>
    </el-dialog>

    <!-- 右键菜单 -->
    <transition name="context-menu">
      <div
        v-if="contextMenuVisible"
        v-clickOutSide="closeContextMenu"
        class="context-menu"
        :style="contextMenuStyle"
      >
        <div
          v-for="item in contextMenuItems"
          :key="item.id"
          class="menu-item"
          @click="handleContextAction(item)"
        >
          <svg-icon :icon-class="item.icon" class="menu-icon" />
          <span>{{ item.name }}</span>
        </div>
      </div>
    </transition>
        </div>
      </el-tab-pane>
      <el-tab-pane label="周期记账" name="recurring">
        <recurring-panel v-if="activeTab === 'recurring'" />
      </el-tab-pane>
      <el-tab-pane label="账户管理" name="account">
        <bk-account v-if="activeTab === 'account'" />
      </el-tab-pane>
      <el-tab-pane label="分类管理" name="classification">
        <bk-classification v-if="activeTab === 'classification'" />
      </el-tab-pane>
      <el-tab-pane label="图表统计" name="statistics">
        <bk-statistics v-if="activeTab === 'statistics'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getClassificationList } from '@/api/px/life/bookkeeping/classification'
import { getAccountList } from '@/api/px/life/bookkeeping/account'
import {
  addRecord,
  delRecord,
  exportRecord,
  getRecord,
  listRecord,
  updateRecord
} from '@/api/px/life/bookkeeping/record'
import recordBatchMixin from './recordBatchMixin'
import {timeFilter} from "../../../../utils/filters.js";
import { listDay } from '@/api/px/life/commemorationDay'
import BkAccount from './account.vue'
import RecurringPanel from './RecurringPanel.vue'
import BkClassification from './classification.vue'
import BkStatistics from './statistics.vue'

export default {
  name: 'Record',
  components: { BkAccount, BkClassification, BkStatistics, RecurringPanel },
  mixins: [recordBatchMixin],
  data() {
    return {
      // 当前激活的 tab
      activeTab: 'record',
      // 加载标志
      listLoading: false,
      // 导出加载标志
      exportLoading: false,
      loading: false,
      // 查询表单
      queryForm: {
        searchValue: '',
        type: '',
        account: '',
        otherAccount: '',
        payTime: this.parseTime(new Date(), '{y}-{m}'),
        pageNum: 1,
        pageSize: 10
      },
      // 分页
      total: 0,
      MoneyTotal: '',
      // 记录金额
      money: '',
      // 表单数据
      recordForm: {
        typeDifference: '1',
        payTime: this.parseTime(new Date()),
        type: '',
        account: '',
        otherAccount: '',
        commemorationDayId: null,
        money: 0,
        remark: ''
      },
      // 分类下拉
      typeList: [],
      typeProps: {
        value: 'id',
        label: 'typeName'
      },
      // 账户下拉
      accountList: [],
      commemorationDayList: [],
      accountProps: {
        value: 'id',
        label: 'accountName'
      },
      // 校验规则
      rules: {
        type: { required: true, message: '请选择分类', trigger: 'change' },
        account: { required: true, message: '请选择账户', trigger: 'change' },
        otherAccount: { required: true, message: '请选择账户', trigger: 'change' },
        money: { required: true, message: '请输入金额', trigger: 'blur' }
      },
      // 弹窗
      record: false,
      recordTitle: '',
      recordData: [],
      // 右键菜单
      contextMenuVisible: false,
      contextMenuStyle: '',
      contextMenuItems: [],
      // 当前选中
      active: null
    }
  },
  watch: {
    money(value) {
      // eslint-disable-next-line no-eval
      this.recordForm.money = eval(value)
    }
  },
  async mounted() {
    await this.init()
    if (this.$route.query.remember) {
      this.record = true
    }
    if (this.$route.query.recordId) {
      this.getRecord(this.$route.query.recordId)
    }
  },
  methods: {
      timeFilter,
    /**
     * 翻译账单类型
     */
    billType(record) {
      if (record.typeObject && record.typeObject.typeDifference === '0') {
        return '收入'
      }
      if (record.typeObject && record.typeObject.typeDifference === '1') {
        return '支出'
      }
      if (record.typeObject && record.typeObject.typeDifference === '2') {
        return '转账'
      }
      if (record.typeObject && record.typeObject.typeDifference === '3') {
        return '修改余额'
      }
    },
    /**
     * 获取类型样式类
     */
    billTypeClass(record) {
      if (!record.typeObject) return ''
      const map = {
        '0': 'type-income',
        '1': 'type-expenditure',
        '2': 'type-transfer',
        '3': 'type-adjust'
      }
      return map[record.typeObject.typeDifference] || ''
    },
    /**
     * 初始化
     */
    init() {
      this.listClassification()
      this.listAccount()
      this.listCommemorationDays()
      this.listRecord()
    },
    /**
     * 获取分类数据
     */
    listClassification() {
      getClassificationList({ typeDifference: this.recordForm.typeDifference }).then(res => {
        this.typeList = res.data
      })
    },
    /**
     * 获取账户数据
     */
    listAccount() {
      getAccountList().then(res => {
        this.accountList = res.data
      })
    },
    listCommemorationDays() {
      listDay({ pageNum: 1, pageSize: 200 }).then(res => {
        this.commemorationDayList = res.rows || res.data || []
      })
    },
    /**
     * 获取记录list
     */
    /**
     * 按当前筛选条件导出 Excel
     */
    handleExport() {
      this.exportLoading = true
      exportRecord({...this.queryForm, pageNum: undefined, pageSize: undefined}).then(res => {
        this.download(res.msg)
      }).finally(() => {
        this.exportLoading = false
      })
    },
    listRecord() {
      this.listLoading = true
      listRecord(this.queryForm).then(res => {
        this.recordData = res.rows
        this.total = res.total
        this.MoneyTotal = res.msg.split(',')
        this.listLoading = false
      })
    },
    /**
     * 选中记录
     */
    handleSelect(item) {
      this.active = item
    },
    /**
     * 获取分类选中节点的目标值
     */
    selectTypeTarget(item) {
      this.queryForm.type = item[1]
      this.listRecord()
    },
    /**
     * 获取账户选中节点的目标值
     */
    selectAccountTarget(item) {
      this.queryForm.account = item[1]
      this.listRecord()
    },
    /**
     * 弹窗获取分类选中节点的目标值
     */
    selectTypeTargetAdd(item) {
      this.recordForm.type = item[1]
    },
    /**
     * 弹窗获取账户选中节点的目标值
     */
    selectAccountTargetAdd(item) {
      this.recordForm.account = item[1]
    },
    /**
     * 弹窗获取转入账户选中节点的目标值
     */
    selectOtherAccountTargetAdd(item) {
      this.recordForm.otherAccount = item[1]
    },
    /**
     * 打开新增记录弹窗
     */
    openRecord() {
      this.recordTitle = '新增记录'
      this.record = true
    },
    /**
     * 新增/修改账户记录
     */
    addRecord() {
      this.$refs.recordForm.validate(valid => {
        if (valid) {
          if (this.recordForm.typeDifference === '2') {
            this.recordForm.type = 0
          }
          if (this.recordForm.account === this.recordForm.otherAccount) {
            this.$message.warning('转出账户和转入账户不能相同')
            return
          }
          if (this.recordForm.id) {
            updateRecord(this.recordForm).then(() => {
              this.record = false
              this.$notify.success('修改记录成功')
              this.listRecord()
              this.resetRecordForm()
            })
          } else {
            addRecord(this.recordForm).then(() => {
              this.record = false
              this.$notify.success('新增记录成功')
              this.listRecord()
              this.resetRecordForm()
            })
          }
        }
      })
    },
    /**
     * 清除查询表单数据
     */
    resetQueryForm() {
      this.queryForm = {
        searchValue: '',
        payTime: '',
        type: '',
        account: '',
        otherAccount: '',
        pageNum: 1,
        pageSize: this.queryForm.pageSize
      }
      this.listRecord()
    },
    /**
     * 清除表单数据
     */
    resetRecordForm() {
      this.recordForm = {
        payTime: this.parseTime(new Date()),
        typeDifference: '1',
        type: '',
        account: '',
        otherAccount: '',
        commemorationDayId: null,
        money: '',
        remark: ''
      }
      this.money = ''
      if (this.$refs.recordForm) {
        this.$refs.recordForm.clearValidate()
      }
    },
    /**
     * 切换当前页
     */
    handleCurrentChange(val) {
      this.queryForm.pageNum = val
      this.listRecord()
    },
    /**
     * 修改当前记录前数据回显
     */
    getRecord(id) {
      this.loading = true
      getRecord(id).then(res => {
        this.recordForm = res.data
        this.recordForm.typeDifference = this.recordForm.typeObject.typeDifference
        this.recordTitle = '修改记录'
        this.record = true
        this.loading = false
      })
    },
    /**
     * 删除当前记录
     */
    delRecord(id) {
      this.$confirm('确认删除当前记录？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(() => {
        return delRecord(id)
      }).then(() => {
        this.$notify.success('删除成功')
        this.active = null
        this.listRecord()
      }).catch(() => {})
    },
    /**
     * 右键菜单
     */
    handleContextMenu(event, item) {
      if (item) {
        this.active = item
        this.contextMenuItems = [
          { id: 1, name: '编辑', icon: '编辑' },
          { id: 2, name: '删除', icon: '删除' }
        ]
      } else {
        this.contextMenuItems = [
          { id: 3, name: '新增记录', icon: '编辑02' }
        ]
      }
      this.contextMenuVisible = true
      this.contextMenuStyle = `top: ${Math.min(event.y, window.innerHeight - this.contextMenuItems.length * 48)}px; left: ${Math.min(event.x - 180, window.innerWidth - 200)}px;`
    },
    /**
     * 关闭右键菜单
     */
    closeContextMenu() {
      this.contextMenuVisible = false
    },
    /**
     * 右键菜单操作
     */
    handleContextAction(item) {
      this.contextMenuVisible = false
      switch (item.id) {
        case 1:
          if (this.active) this.getRecord(this.active.id)
          break
        case 2:
          if (this.active) this.delRecord(this.active.id)
          break
        case 3:
          this.openRecord()
          break
      }
    },
    /**
     * 处理 FAB 菜单命令
     */
    handleFabCommand(command) {
      if (command === 'single') {
        this.openRecord()
      } else if (command === 'batch') {
        this.openBatchRecord()
      }
    }
  }
}
</script>

<style lang="scss" scoped src="./record.scss"></style>
