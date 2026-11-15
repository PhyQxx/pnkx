<template>
    <div class="m-form maintenance-page">
        <!-- 基本信息 -->
        <div class="m-section-title">基本信息</div>
        <van-cell-group inset :border="false">
            <van-field
                v-model="form.accountName"
                label="账户名称"
                placeholder="请输入账户名称"
                required
                maxlength="30"
            />
            <van-field
                v-model="form.balance"
                label="账户余额"
                type="number"
                placeholder="请输入账户余额"
                required
            />
            <van-field
                :model-value="accountTypeName"
                label="账户类型"
                placeholder="请选择账户类型"
                readonly
                is-link
                required
                @click="showTypePicker = true"
            />
        </van-cell-group>

        <!-- 个性化 -->
        <div class="m-section-title">个性化</div>
        <van-cell-group inset :border="false">
            <van-field
                :model-value="form.accountIcon"
                label="账户图标"
                placeholder="选择图标"
                readonly
                is-link
                @click="showIconPicker = true"
            >
                <template #left-icon>
                    <span class="maintenance-icon-preview">
                        <img v-if="iconOf(form.accountIcon)" :src="iconOf(form.accountIcon)" alt="">
                        <van-icon v-else name="balance-o" />
                    </span>
                </template>
            </van-field>
        </van-cell-group>

        <!-- 其他 -->
        <div class="m-section-title">其他</div>
        <van-cell-group inset :border="false">
            <van-field
                v-model="form.remark"
                label="备注"
                type="textarea"
                placeholder="请输入备注"
                rows="2"
                autosize
                maxlength="200"
                show-word-limit
            />
        </van-cell-group>

        <!-- 底部操作 -->
        <div class="m-sticky-actions">
            <van-button v-if="form.id" round plain type="danger" @click="handleDelete">删除账户</van-button>
            <van-button round type="primary" :loading="saving" @click="handleSave">保存</van-button>
        </div>

        <!-- 账户类型选择 -->
        <van-popup v-model:show="showTypePicker" position="bottom" round teleport="body">
            <van-picker
                title="账户类型"
                :columns="typeColumns"
                @confirm="onTypeConfirm"
                @cancel="showTypePicker = false"
            />
        </van-popup>

        <!-- 图标选择 -->
        <icon-select
            v-model:show="showIconPicker"
            v-model="form.accountIcon"
            prefix="a-"
        />
    </div>
</template>

<script>
import { getAccount, addAccount, updateAccount, delAccount } from '@/api/px/life/bookkeeping/account'
import { getDicts } from '@/api/system/dict/data'
import { iconUrl } from '@/mobile/utils/icons'
import { mConfirm, mError, mLoading, mSuccess, closeToast } from '@/mobile/utils/ui'
import IconSelect from './components/IconSelect.vue'

export default {
    name: 'MBookkeepingAccountMaintenance',
    components: { IconSelect },
    data() {
        return {
            form: {
                id: null,
                accountName: '',
                balance: 0,
                accountType: '',
                accountIcon: '',
                remark: ''
            },
            accountTypeOptions: [],
            showTypePicker: false,
            showIconPicker: false,
            saving: false
        }
    },
    computed: {
        accountTypeName() {
            const option = this.accountTypeOptions.find(item => item.value === this.form.accountType)
            return option ? option.text : ''
        },
        typeColumns() {
            return this.accountTypeOptions.map(item => ({ text: item.text, value: item.value }))
        }
    },
    mounted() {
        // query.id：有值=编辑模式
        if (this.$route.query.id) {
            this.form.id = this.$route.query.id
            this.getAccountInfo()
        }
        this.getAccountTypes()
    },
    methods: {
        iconOf(name) {
            return iconUrl(name)
        },
        async getAccountTypes() {
            try {
                const res = await getDicts('px_bookkeeping_account_type')
                this.accountTypeOptions = (res.data || []).map(item => ({
                    value: item.dictValue,
                    text: item.dictLabel
                }))
            } catch (e) {
                console.error('获取账户类型失败', e)
            }
        },
        async getAccountInfo() {
            try {
                const res = await getAccount(this.form.id)
                this.form = { ...this.form, ...res.data }
            } catch (e) {
                console.error('获取账户信息失败', e)
            }
        },
        onTypeConfirm({ selectedOptions }) {
            if (selectedOptions[0]) {
                this.form.accountType = selectedOptions[0].value
            }
            this.showTypePicker = false
        },
        async handleSave() {
            if (!this.form.accountName) {
                mError('请输入账户名称')
                return
            }
            if (this.form.balance === '' || this.form.balance === null) {
                mError('请输入账户余额')
                return
            }
            if (!this.form.accountType) {
                mError('请选择账户类型')
                return
            }

            this.saving = true
            mLoading('保存中')
            try {
                const payload = { ...this.form, balance: Number(this.form.balance) }
                if (this.form.id) {
                    await updateAccount(payload)
                    mSuccess('修改成功')
                } else {
                    await addAccount(payload)
                    mSuccess('新增成功')
                }
                setTimeout(() => {
                    this.$router.back()
                }, 600)
            } catch (e) {
                console.error('保存失败', e)
            } finally {
                this.saving = false
                closeToast()
            }
        },
        async handleDelete() {
            try {
                await mConfirm('删除确认', '删除该账户也会删除其下流水记录，确定删除？')
            } catch (e) {
                return
            }
            try {
                await delAccount(this.form.id)
                mSuccess('删除成功')
                setTimeout(() => {
                    this.$router.back()
                }, 600)
            } catch (e) {
                console.error('删除失败', e)
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.maintenance-icon-preview {
    width: 26px;
    height: 26px;
    margin-right: 8px;
    border-radius: 6px;
    background: var(--pnkx-surface-muted);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--pnkx-primary);
    font-size: 15px;
    overflow: hidden;

    img {
        width: 18px;
        height: 18px;
        object-fit: contain;
    }
}
</style>
