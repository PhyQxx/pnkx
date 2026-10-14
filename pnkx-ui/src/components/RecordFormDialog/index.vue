<template>
  <el-dialog
    :title="editData && editData.id ? '修改记录' : '新增记录'"
    :model-value="modelValue"
    width="520px"
    custom-class="modern-dialog"
    :modal-append-to-body="true"
    @update:model-value="v => $emit('update:modelValue', v)"
    @closed="resetRecordForm"
  >
    <el-tabs v-model="recordForm.typeDifference" class="modern-tabs" @tab-click="listClassification">
      <el-tab-pane label="支出" name="1" :disabled="Boolean(recordForm.id)" />
      <el-tab-pane label="收入" name="0" :disabled="Boolean(recordForm.id)" />
      <el-tab-pane label="转账" name="2" :disabled="Boolean(recordForm.id)" />
    </el-tabs>
    <el-form ref="recordFormRef" :model="recordForm" :rules="rules" label-position="top" class="modern-form">
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
        <el-button class="btn-cancel" @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" class="btn-confirm" @click="addRecord">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { addRecord, updateRecord } from '@/api/px/life/bookkeeping/record'
import { listClassification } from '@/api/px/life/bookkeeping/classification'
import { getAccountList } from '@/api/px/life/bookkeeping/account'
import { listDay } from '@/api/px/life/commemorationDay'

/**
 * 新增/修改记账弹窗（共用组件）。
 * 生活账本页与工作台「快捷录入」共用；打开时自动拉取分类/账户/纪念日，
 * 编辑场景传 edit-data 回显，保存成功 emit('saved') 由调用方刷新列表。
 */
export default {
  name: 'RecordFormDialog',
  props: {
    modelValue: { type: Boolean, default: false },
    editData: { type: Object, default: null }
  },
  emits: ['update:modelValue', 'saved'],
  data() {
    return {
      money: '',
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
      typeList: [],
      typeProps: { value: 'id', label: 'typeName' },
      accountList: [],
      commemorationDayList: [],
      accountProps: { value: 'id', label: 'accountName' },
      rules: {
        type: { required: true, message: '请选择分类', trigger: 'change' },
        account: { required: true, message: '请选择账户', trigger: 'change' },
        otherAccount: { required: true, message: '请选择账户', trigger: 'change' },
        money: { required: true, message: '请输入金额', trigger: 'blur' }
      }
    }
  },
  watch: {
    money(value) {
      // eslint-disable-next-line no-eval
      this.recordForm.money = eval(value)
    },
    modelValue: {
      handler(v) {
        if (!v) return
        this.listClassification()
        this.listAccount()
        this.listCommemorationDays()
        if (this.editData && this.editData.id) {
          this.recordForm = { ...this.editData }
        } else {
          this.resetRecordForm()
        }
      },
      immediate: true
    }
  },
  methods: {
    listClassification() {
      listClassification({ typeDifference: this.recordForm.typeDifference }).then(res => {
        this.typeList = res.data
      })
    },
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
    selectTypeTargetAdd(item) { this.recordForm.type = item[1] },
    selectAccountTargetAdd(item) { this.recordForm.account = item[1] },
    selectOtherAccountTargetAdd(item) { this.recordForm.otherAccount = item[1] },
    addRecord() {
      this.$refs.recordFormRef.validate(valid => {
        if (valid) {
          if (this.recordForm.typeDifference === '2') {
            this.recordForm.type = 0
          }
          if (this.recordForm.account === this.recordForm.otherAccount) {
            this.$message.warning('转出账户和转入账户不能相同')
            return
          }
          const done = () => {
            this.$emit('update:modelValue', false)
            this.$notify.success(this.recordForm.id ? '修改记录成功' : '新增记录成功')
            this.emitSaved()
            this.resetRecordForm()
          }
          if (this.recordForm.id) {
            updateRecord(this.recordForm).then(done)
          } else {
            addRecord(this.recordForm).then(done)
          }
        }
      })
    },
    /** 供调用方在 saved 后拿到刚保存的数据（可选） */
    emitSaved() {
      this.$emit('saved', { ...this.recordForm })
    },
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
      if (this.$refs.recordFormRef) {
        this.$refs.recordFormRef.clearValidate()
      }
    }
  }
}
</script>
