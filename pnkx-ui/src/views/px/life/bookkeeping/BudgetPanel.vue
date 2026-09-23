<template>
  <div class="budget-panel">
    <div class="panel-header">
      <h3 class="panel-title">预算</h3>
      <div class="header-actions">
        <div class="month-nav">
          <button class="nav-btn" @click="changeMonth(0)">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <span class="month-display">{{ month }}</span>
          <button class="nav-btn" :disabled="isCurrentMonth" @click="changeMonth(1)">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <el-button size="small" text type="primary" @click="openSetting">设置预算</el-button>
      </div>
    </div>

    <div v-if="statusList.length === 0" class="budget-empty">
      还没有设置预算，点击「设置预算」开始规划当月支出
    </div>

    <div v-else class="budget-list">
      <div v-for="item in statusList" :key="item.id" class="budget-item" :class="{exceeded: item.exceeded}">
        <div class="item-row">
          <span class="item-name">
            {{ item.typeName }}
            <el-tag v-if="item.exceeded" size="small" type="danger" effect="dark" class="exceed-tag">超支</el-tag>
          </span>
          <span class="item-amount">
            {{ item.used }} / {{ item.amount }} 元
          </span>
          <el-button class="item-delete" size="small" text type="danger" @click="handleDelete(item)">删除</el-button>
        </div>
        <el-progress
            :percentage="item.percent || 0"
            :color="item.exceeded ? '#f56c6c' : (item.percent >= 80 ? '#e6a23c' : '#67c23a')"
            :stroke-width="8"
        />
      </div>
    </div>

    <!-- 设置预算弹窗 -->
    <el-dialog v-model="settingVisible" title="设置预算" width="420px" append-to-body>
      <el-form :model="form" label-width="80px">
        <el-form-item label="月份">
          <el-input v-model="form.month" disabled />
        </el-form-item>
        <el-form-item label="预算类型">
          <el-select v-model="form.typeId" placeholder="选择分类（总预算/支出分类）" style="width: 100%">
            <el-option label="总预算" :value="0" />
            <el-option v-for="c in classificationOptions" :key="c.id" :label="c.typeName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额（元）">
          <el-input-number v-model="form.amount" :min="1" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {ArrowLeft, ArrowRight} from '@element-plus/icons-vue'
import {delBudget, getBudgetStatus, saveBudget} from '@/api/px/life/bookkeeping/budget'
import {getClassificationList} from '@/api/px/life/bookkeeping/classification'

export default {
    name: 'BudgetPanel',
    components: {ArrowLeft, ArrowRight},
    data() {
        return {
            month: this.parseTime(new Date(), '{y}-{m}'),
            statusList: [],
            settingVisible: false,
            saving: false,
            classificationOptions: [],
            form: {
                month: '',
                typeId: 0,
                amount: 1000
            }
        }
    },
    computed: {
        isCurrentMonth() {
            return this.month === this.parseTime(new Date(), '{y}-{m}')
        }
    },
    created() {
        this.loadStatus()
        this.loadClassifications()
    },
    methods: {
        loadStatus() {
            getBudgetStatus(this.month).then(res => {
                this.statusList = res.data || []
            })
        },
        loadClassifications() {
            getClassificationList({typeDifference: '1'}).then(res => {
                // 只取二级分类（记账记录的 type 即二级分类）
                this.classificationOptions = (res.data || []).filter(c => c.typeLevel === '1')
            })
        },
        changeMonth(direction) {
            const [y, m] = this.month.split('-').map(Number)
            const d = new Date(y, m - 1 + (direction ? 1 : -1), 1)
            this.month = this.parseTime(d, '{y}-{m}')
            this.loadStatus()
        },
        openSetting() {
            this.form = {month: this.month, typeId: 0, amount: 1000}
            this.settingVisible = true
        },
        handleSave() {
            this.saving = true
            saveBudget(this.form).then(() => {
                this.msgSuccess('预算已保存')
                this.settingVisible = false
                this.loadStatus()
            }).finally(() => {
                this.saving = false
            })
        },
        handleDelete(item) {
            this.$confirm(`确认删除「${item.typeName}」预算？`, '提示', {type: 'warning'}).then(() => {
                return delBudget(item.id)
            }).then(() => {
                this.msgSuccess('已删除')
                this.loadStatus()
            }).catch(() => {
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.budget-panel {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .panel-title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 8px;

  .nav-btn {
    border: none;
    background: #f5f7fa;
    border-radius: 6px;
    width: 24px;
    height: 24px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }

  .month-display {
    font-size: 14px;
    font-weight: 500;
    min-width: 56px;
    text-align: center;
  }
}

.budget-empty {
  color: #999;
  font-size: 13px;
  padding: 8px 0;
}

.budget-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px 24px;
}

.budget-item {
  .item-row {
    display: flex;
    align-items: center;
    margin-bottom: 4px;

    .item-name {
      font-size: 13px;
      font-weight: 500;
      flex: 1;

      .exceed-tag {
        margin-left: 6px;
      }
    }

    .item-amount {
      font-size: 12px;
      color: #666;
      margin-right: 4px;
    }

    .item-delete {
      padding: 2px 6px;
      height: auto;
    }
  }

  &.exceeded .item-name {
    color: #f56c6c;
  }
}
</style>
