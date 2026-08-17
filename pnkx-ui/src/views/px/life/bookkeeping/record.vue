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
  addBatchRecord,
  addRecord,
  aiBatchParse,
  delRecord,
  getRecord,
  listRecord,
  updateRecord
} from '@/api/px/life/bookkeeping/record'
import {timeFilter} from "../../../../utils/filters.js";
import { listDay } from '@/api/px/life/commemorationDay'
import BkAccount from './account.vue'
import BkClassification from './classification.vue'
import BkStatistics from './statistics.vue'

export default {
  name: 'Record',
  components: { BkAccount, BkClassification, BkStatistics },
  data() {
    return {
      // 当前激活的 tab
      activeTab: 'record',
      // 加载标志
      listLoading: false,
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
      // 批量记账
      batchVisible: false,
      batchLoading: false,
      batchRecordList: [],
      // AI智能导入
      aiImportVisible: false,
      aiImportText: '',
      aiParsing: false,
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

      // 如果已有记录，类型沿用上一条；时间、分类、账户显示“同上”，提交时再解析
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

      // 空的时间、分类、账户、转入账户表示“同上”，转换成接口需要的实际值
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
</script>

<style lang="scss" scoped>
@import '@/assets/styles/design-tokens.scss';

$bk-red: $theme-bookkeeping-red;
$bk-green: $theme-bookkeeping-green;

.bookkeeping-page {
  height: calc(100vh - 84px);
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.bookkeeping-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__content) {
    flex: 1;
    overflow: hidden;
  }
}

.bookkeeping-record-container {
  display: flex;
  height: 100%;
  background: var(--bg-body);
  font-family: var(--font-family-base);
}

// 左侧边栏
.sidebar {
  width: 40vw;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--border-primary);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-sm);
  position: relative;
  z-index: 10;
}

// 搜索栏
.search-wrapper {
  padding: var(--space-5);
  border-bottom: 1px solid var(--border-primary);

  .search-box {
    position: relative;
    display: flex;
    align-items: center;

    .search-icon {
      position: absolute;
      left: 14px;
      font-size: var(--text-lg);
      color: var(--text-tertiary);
      pointer-events: none;
    }

    .search-input {
      width: 100%;
      height: 40px;
      padding: 0 var(--space-4) 0 42px;
      border: none;
      border-radius: var(--radius-lg);
      background: var(--bg-body);
      font-size: var(--text-base);
      color: var(--text-primary);
      box-shadow: var(--shadow-sm);
      transition: all var(--duration-normal) var(--ease-default);

      &::placeholder { color: var(--text-tertiary); }
      &:focus {
        outline: none;
        box-shadow: 0 0 0 3px var(--color-primary-100), var(--shadow-md);
      }
    }
  }
}

// 筛选区域
.filter-section {
  padding: var(--space-3) var(--space-5);
  border-bottom: 1px solid var(--border-primary);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);

  .filter-cascader {
    flex: 1;
    min-width: 120px;
  }

  .filter-date {
    width: 140px;
  }

  ::v-deep .el-input__inner {
    border-radius: var(--radius-sm);
    border-color: var(--border-primary);
    font-size: var(--text-sm);
  }
}

// 月度汇总
.month-summary {
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-6);

  .summary-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-1);

    .summary-label {
      font-size: var(--text-xs);
      color: var(--text-tertiary);
    }

    .summary-value {
      font-size: var(--text-lg);
      font-weight: var(--font-bold);
      font-variant-numeric: tabular-nums;

      &.income { color: $bk-red; }
      &.expenditure { color: $bk-green; }
    }
  }

  .summary-divider {
    width: 1px;
    height: 24px;
    background: var(--border-primary);
  }
}

// 记录列表
.record-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-3) var(--space-4);

  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: var(--color-slate-300);
    border-radius: 3px;
    &:hover { background: var(--color-slate-400); }
  }
}

.record-items {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.record-card {
  display: flex;
  align-items: center;
  padding: var(--space-3) 14px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  box-shadow: var(--shadow-sm);
  border-left: 3px solid transparent;
  animation: fadeSlideIn 0.4s ease forwards;
  opacity: 0;

  &.active {
    background: var(--bg-selected);
    border-left-color: var(--color-primary);
  }

  &:hover {
    transform: translateX(4px);
    box-shadow: var(--shadow-md);
    background: var(--bg-hover);
  }

  .card-icon-wrapper {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-md);
    margin-right: var(--space-3);
    flex-shrink: 0;
    background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);

    &.type-income { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
    &.type-expenditure { background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%); }
    &.type-transfer { background: linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%); }
    &.type-adjust { background: linear-gradient(135deg, #ffd89b 0%, #f2994a 100%); }

    .card-icon {
      font-size: 18px;
      color: white;
    }
  }

  .card-info {
    flex: 1;
    min-width: 0;

    .card-title {
      font-size: var(--text-base);
      font-weight: var(--font-medium);
      color: var(--text-primary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-bottom: 2px;
    }

    .card-remark {
      font-size: var(--text-xs);
      color: var(--text-secondary);
      opacity: 0.8;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-bottom: 4px;
      font-style: italic;
    }

    .card-meta {
      display: flex;
      gap: var(--space-2);
      font-size: var(--text-xs);
      color: var(--text-secondary);

      .card-account {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 100px;
      }

      .card-time { color: var(--text-tertiary); }
    }
  }

  .card-amount {
    font-size: 15px;
    font-weight: var(--font-semibold);
    color: var(--text-primary);
    flex-shrink: 0;
    margin-left: var(--space-2);
    font-variant-numeric: tabular-nums;

    &.income { color: $bk-red; }
    &.expenditure { color: $bk-green; }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-tertiary);

  .empty-icon { font-size: 64px; opacity: 0.3; margin-bottom: var(--space-4); }
  p { margin: var(--space-1) 0; }
  .hint { font-size: var(--text-xs); opacity: 0.7; }
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

// 分页
.sidebar-pagination {
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--border-primary);
  display: flex;
  justify-content: center;

  ::v-deep .el-pagination {
    .btn-prev, .btn-next, .el-pager li {
      background: transparent;
      border-radius: var(--radius-sm);
    }

    .el-pager li.active {
      background: var(--color-primary);
      color: white;
    }
  }
}

// 右侧详情
.detail-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.empty-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);

  .empty-detail-icon { font-size: 80px; opacity: 0.2; margin-bottom: var(--space-5); }
  p { font-size: var(--text-lg); }
}

.record-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: 28px var(--space-8) 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-primary);

  .detail-icon-wrapper {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-lg);
    flex-shrink: 0;
    background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
    box-shadow: 0 4px 16px rgba(212, 252, 121, 0.3);

    &.type-income {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      box-shadow: 0 4px 16px rgba(240, 147, 251, 0.3);
    }
    &.type-transfer {
      background: linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%);
      box-shadow: 0 4px 16px rgba(137, 247, 254, 0.3);
    }
    &.type-adjust {
      background: linear-gradient(135deg, #ffd89b 0%, #f2994a 100%);
      box-shadow: 0 4px 16px rgba(255, 216, 155, 0.3);
    }

    .detail-icon {
      font-size: 28px;
      color: white;
    }
  }

  .detail-title-section {
    flex: 1;

    .detail-type-badge {
      display: inline-block;
      font-size: var(--text-xs);
      padding: 2px 10px;
      border-radius: var(--radius-full);
      margin-bottom: var(--space-2);
      background: var(--bg-hover);
      color: var(--text-secondary);

      &.type-income { background: rgba(241, 82, 58, 0.1); color: $bk-red; }
      &.type-expenditure { background: rgba(20, 186, 137, 0.1); color: $bk-green; }
      &.type-transfer { background: var(--color-primary-50); color: var(--color-primary); }
    }

    .detail-amount {
      font-size: var(--text-4xl);
      font-weight: var(--font-bold);
      color: var(--text-primary);
      margin: 0;
      font-variant-numeric: tabular-nums;

      .detail-unit {
        font-size: var(--text-base);
        font-weight: var(--font-normal);
        color: var(--text-secondary);
        margin-left: var(--space-1);
      }
    }
  }

  .detail-actions {
    display: flex;
    gap: var(--space-2);
    flex-shrink: 0;

    .el-button {
      border-radius: var(--radius-sm);
      transition: all var(--duration-fast) var(--ease-default);
      .action-icon { font-size: var(--text-base); margin-right: var(--space-1); }
    }
  }
}

.detail-body {
  padding: var(--space-6) var(--space-8);
}

.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-5);
  padding: var(--space-6);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--space-6);

  .info-item {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);

    .info-label {
      font-size: var(--text-xs);
      color: var(--text-tertiary);
      font-weight: var(--font-medium);
    }

    .info-value {
      font-size: 15px;
      color: var(--text-primary);
      font-weight: var(--font-medium);
    }
  }
}

.detail-remark {
  padding: var(--space-5) var(--space-6);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);

  h4 {
    font-size: var(--text-base);
    font-weight: var(--font-medium);
    color: var(--text-secondary);
    margin: 0 0 var(--space-2) 0;
  }

  p {
    font-size: var(--text-base);
    color: var(--text-primary);
    line-height: var(--leading-relaxed);
    margin: 0;
    white-space: pre-wrap;
  }
}

// FAB
.fab-group {
  position: fixed;
  right: var(--space-8);
  bottom: var(--space-8);
  z-index: 100;
}

.fab-action {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.4);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);

  i { font-size: var(--text-3xl); }
  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 28px rgba(14, 165, 233, 0.5);
  }
  &:active { transform: scale(0.95); }
}

.fab-dropdown-menu {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-primary);

  .el-dropdown-menu__item {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-4);
    font-size: var(--text-sm);

    i { font-size: 16px; }
  }
}

// 批量记账弹窗样式
.batch-dialog {
  .batch-toolbar {
    margin-bottom: var(--space-4);
    display: flex;
    gap: var(--space-2);
  }

  .modern-table {
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-md);

    ::v-deep .el-table__header-wrapper th {
      background: var(--bg-body);
      color: var(--text-secondary);
      font-weight: var(--font-semibold);
    }
  }

  .text-muted {
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
}

.ai-import-tip {
  margin-bottom: var(--space-4);
  font-size: var(--text-sm);
  color: var(--text-secondary);

  .example {
    font-size: var(--text-xs);
    color: var(--text-tertiary);
    background: var(--bg-body);
    padding: var(--space-2);
    border-radius: var(--radius-sm);
    margin-top: var(--space-2);
  }
}

.ai-import-input {
  margin-bottom: var(--space-2);
}

// 右键菜单
.context-menu {
  position: fixed;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--space-2) 0;
  min-width: 180px;
  z-index: 9999;
  border: 1px solid var(--border-primary);

  .menu-item {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    font-size: var(--text-base);
    color: var(--text-primary);
    cursor: pointer;
    transition: all var(--duration-fast) var(--ease-default);

    .menu-icon { font-size: var(--text-lg); color: var(--text-secondary); }
    &:hover {
      background: var(--bg-hover);
      color: var(--color-primary);
      .menu-icon { color: var(--color-primary); }
    }
  }
}

.context-menu-enter-active,
.context-menu-leave-active { transition: all 0.2s ease; }
.context-menu-enter,
.context-menu-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(-8px);
}

.item-list-enter-active,
.item-list-leave-active { transition: all 0.3s ease; }
.item-list-enter,
.item-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

// 对话框样式
::v-deep .modern-dialog {
  border-radius: var(--radius-xl) !important;
  overflow: hidden;
  box-shadow: var(--shadow-xl) !important;

  .el-dialog__header {
    padding: var(--space-5) var(--space-6) var(--space-4);
    border-bottom: 1px solid var(--border-primary);
    background: var(--bg-card);
    .el-dialog__title {
      font-size: var(--text-xl);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }
  }

  .el-dialog__body { padding: var(--space-6); }

  .el-dialog__footer {
    padding: var(--space-4) var(--space-6) var(--space-5);
    border-top: 1px solid var(--border-primary);
    background: var(--bg-body);
  }
}

// 现代Tab样式
::v-deep .modern-tabs {
  .el-tabs__nav-wrap::after {
    height: 1px;
    background: var(--border-primary);
  }

  .el-tabs__item {
    font-size: var(--text-base);
    color: var(--text-secondary);
    transition: all var(--duration-normal) var(--ease-default);

    &.is-active {
      color: var(--color-primary);
      font-weight: var(--font-medium);
    }

    &:hover { color: var(--color-primary); }
  }

  .el-tabs__active-bar {
    background-color: var(--color-primary);
  }
}

.modern-form {
  ::v-deep .el-form-item {
    margin-bottom: var(--space-5);
    .el-form-item__label {
      font-size: var(--text-sm);
      font-weight: var(--font-medium);
      color: var(--text-secondary);
      padding-bottom: var(--space-2);
    }
    .el-input__inner {
      border-radius: var(--radius-sm);
      border-color: var(--border-primary);
      transition: all var(--duration-normal) var(--ease-default);
      &:focus {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 3px var(--color-primary-100);
      }
    }
    .el-cascader { width: 100%; }
  }
}

// 金额输入
.money-input-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-3);

  .money-preview {
    white-space: nowrap;
    padding: 0 var(--space-4);
    height: 40px;
    display: flex;
    align-items: center;
    border: 1px solid $bk-green;
    border-radius: var(--radius-sm);
    font-size: var(--text-base);
    color: $bk-green;
    font-weight: var(--font-medium);
    background: rgba(20, 186, 137, 0.04);
  }

  .money-input {
    flex: 1;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);

  .btn-cancel {
    border-radius: var(--radius-sm);
    padding: 10px var(--space-5);
    transition: all var(--duration-normal) var(--ease-default);
    &:hover { background: var(--bg-hover); }
  }

  .btn-confirm {
    border-radius: var(--radius-sm);
    padding: 10px var(--space-6);
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
    border: none;
    transition: all var(--duration-normal) var(--ease-default);
    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }
}

// Loading 美化
::v-deep .el-loading-mask {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

// 通知美化
::v-deep .el-notification {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: none;
}
</style>
