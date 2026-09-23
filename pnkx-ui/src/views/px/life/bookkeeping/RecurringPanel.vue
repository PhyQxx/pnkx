<template>
  <div class="recurring-panel">
    <div class="panel-toolbar">
      <span class="tip">固定支出（房租/会员等）按周期自动生成记账记录，每日 08:30 由系统执行</span>
      <el-button type="primary" size="small" @click="openAdd">新增规则</el-button>
    </div>

    <el-table :data="list" v-loading="loading" class="modern-table">
      <el-table-column label="名称" prop="name" min-width="120" />
      <el-table-column label="周期" width="140">
        <template #default="{row}">
          {{ row.frequency === 'month' ? `每月 ${row.dayNumber} 号` : `每周${weekName(row.dayNumber)}` }}
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="typeName" min-width="110">
        <template #default="{row}">
          {{ row.type === 0 ? '转账' : (row.typeName || '-') }}
        </template>
      </el-table-column>
      <el-table-column label="账户" prop="accountName" min-width="100" />
      <el-table-column label="金额（元）" prop="money" width="110" />
      <el-table-column label="下次执行" width="110">
        <template #default="{row}">{{ parseTime(row.nextRunDate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{row}">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text :type="row.enabled ? 'warning' : 'success'" @click="handleToggle(row)">
            {{ row.enabled ? '停用' : '启用' }}
          </el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="visible" :title="form.id ? '编辑周期规则' : '新增周期规则'" width="460px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如：房租、视频会员" maxlength="30" />
        </el-form-item>
        <el-form-item label="收支类型">
          <el-radio-group v-model="form.typeDifference" :disabled="Boolean(form.id)">
            <el-radio label="1">支出</el-radio>
            <el-radio label="0">收入</el-radio>
            <el-radio label="2">转账</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="频率">
          <el-select v-model="form.frequency" style="width: 110px" @change="onFrequencyChange">
            <el-option label="每月" value="month" />
            <el-option label="每周" value="week" />
          </el-select>
          <el-select v-model="form.dayNumber" style="width: 130px; margin-left: 8px">
            <el-option v-for="n in dayOptions" :key="n.value" :label="n.label" :value="n.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.typeDifference !== '2'" label="分类">
          <el-select v-model="form.type" placeholder="选择分类" style="width: 100%" filterable>
            <el-option v-for="c in classificationOptions" :key="c.id" :label="c.typeName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户">
          <el-select v-model="form.account" placeholder="选择账户" style="width: 100%">
            <el-option v-for="a in accountOptions" :key="a.id" :label="a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.typeDifference === '2'" label="转入账户">
          <el-select v-model="form.otherAccount" placeholder="选择转入账户" style="width: 100%">
            <el-option v-for="a in accountOptions" :key="a.id" :label="a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额（元）">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="选填，生成记录时自动追加周期标记" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ form.id ? '保存' : '新增' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {addRecurring, delRecurring, listRecurring, toggleRecurring, updateRecurring} from '@/api/px/life/bookkeeping/recurring'
import {getClassificationList} from '@/api/px/life/bookkeeping/classification'
import {getAccountList} from '@/api/px/life/bookkeeping/account'

export default {
    name: 'RecurringPanel',
    data() {
        return {
            list: [],
            loading: false,
            visible: false,
            saving: false,
            classificationOptions: [],
            accountOptions: [],
            form: this.emptyForm()
        }
    },
    computed: {
        dayOptions() {
            if (this.form.frequency === 'week') {
                const names = ['一', '二', '三', '四', '五', '六', '日']
                return names.map((n, i) => ({value: i + 1, label: `每周${n}`}))
            }
            return Array.from({length: 28}, (_, i) => ({value: i + 1, label: `${i + 1} 号`}))
        }
    },
    created() {
        this.load()
        getClassificationList({}).then(res => {
            this.classificationOptions = (res.data || []).filter(c => c.typeLevel === '1')
        })
        getAccountList().then(res => {
            this.accountOptions = res.data || []
        })
    },
    methods: {
        emptyForm() {
            return {
                id: null, name: '', frequency: 'month', dayNumber: 1,
                typeDifference: '1', type: null, account: null, otherAccount: null,
                amount: 1000, remark: ''
            }
        },
        load() {
            this.loading = true
            listRecurring().then(res => {
                this.list = res.data || []
            }).finally(() => {
                this.loading = false
            })
        },
        weekName(day) {
            return ['一', '二', '三', '四', '五', '六', '日'][day - 1] || ''
        },
        onFrequencyChange() {
            this.form.dayNumber = 1
        },
        openAdd() {
            this.form = this.emptyForm()
            this.visible = true
        },
        openEdit(row) {
            this.form = {
                id: row.id,
                name: row.name,
                frequency: row.frequency,
                dayNumber: row.dayNumber,
                typeDifference: row.typeDifference,
                type: row.type || null,
                account: row.account,
                otherAccount: row.otherAccount,
                amount: Number(row.money),
                remark: row.remark
            }
            this.visible = true
        },
        handleSave() {
            const data = {
                ...this.form,
                money: this.form.amount
            }
            this.saving = true
            const req = data.id ? updateRecurring(data) : addRecurring(data)
            req.then(() => {
                this.msgSuccess(data.id ? '已保存' : '已新增')
                this.visible = false
                this.load()
            }).finally(() => {
                this.saving = false
            })
        },
        handleToggle(row) {
            toggleRecurring(row.id, !row.enabled).then(() => {
                this.msgSuccess(row.enabled ? '已停用' : '已启用')
                this.load()
            })
        },
        handleDelete(row) {
            this.$confirm(`确认删除周期规则「${row.name}」？（已生成的历史记录不受影响）`, '提示', {type: 'warning'})
                .then(() => delRecurring(row.id))
                .then(() => {
                    this.msgSuccess('已删除')
                    this.load()
                }).catch(() => {
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.recurring-panel {
  padding: 8px 4px;

  .panel-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .tip {
      font-size: 12px;
      color: #999;
    }
  }
}
</style>
