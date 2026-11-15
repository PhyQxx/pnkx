<template>
    <div class="preference-page m-form">
        <!-- 渠道开关 -->
        <van-cell-group :border="false" class="m-cell-card">
            <van-cell center :border="false">
                <template #title>
                    <div class="pref-title">站内实时提醒</div>
                    <div class="pref-hint">页面在线时通过 WebSocket 接收</div>
                </template>
                <template #right-icon>
                    <van-switch v-model="form.websocketEnabled" size="20px" />
                </template>
            </van-cell>
            <van-cell center :border="false">
                <template #title>
                    <div class="pref-title">浏览器通知（如已授权）</div>
                    <div class="pref-hint">仅保存偏好；实际通知依赖浏览器授权</div>
                </template>
                <template #right-icon>
                    <van-switch v-model="form.pushEnabled" size="20px" />
                </template>
            </van-cell>
            <van-cell center :border="false">
                <template #title>
                    <div class="pref-title">邮件提醒</div>
                    <div class="pref-hint">发送到账号绑定邮箱</div>
                </template>
                <template #right-icon>
                    <van-switch v-model="form.emailEnabled" size="20px" />
                </template>
            </van-cell>
        </van-cell-group>

        <!-- 免打扰时段 -->
        <van-cell-group :border="false" class="m-cell-card">
            <div class="quiet-section-title">免打扰时段</div>
            <van-cell center title="开始时间" :value="form.quietStart" is-link @click="pickField = 'quietStart'" />
            <van-cell center title="结束时间" :value="form.quietEnd" is-link @click="pickField = 'quietEnd'" />
            <div class="pref-hint quiet-hint">免打扰期间到期的提醒会保留，并在时段结束后投递。</div>
        </van-cell-group>

        <div class="m-sticky-actions">
            <van-button round type="primary" block :loading="saving" @click="save">保存设置</van-button>
        </div>

        <!-- 时间选择 -->
        <van-popup
            v-model:show="showTimePicker"
            position="bottom"
            round
            teleport="body"
        >
            <van-time-picker
                :model-value="pickValue"
                title="选择时间"
                :min-hour="0"
                :max-hour="23"
                @confirm="onTimeConfirm"
                @cancel="showTimePicker = false"
            />
        </van-popup>
    </div>
</template>

<script>
import { getReminderPreference, saveReminderPreference } from '@/api/px/life/reminder'
import { mSuccess, mError } from '@/mobile/utils/ui'

export default {
    name: 'MReminderSettings',
    data() {
        return {
            saving: false,
            form: {
                websocketEnabled: true,
                pushEnabled: true,
                emailEnabled: true,
                quietStart: '23:00',
                quietEnd: '07:00'
            },
            pickField: ''
        }
    },
    computed: {
        showTimePicker: {
            get() {
                return !!this.pickField
            },
            set(value) {
                if (!value) this.pickField = ''
            }
        },
        pickValue() {
            const value = this.form[this.pickField] || '00:00'
            return value.split(':')
        }
    },
    mounted() {
        this.loadPreference()
    },
    methods: {
        async loadPreference() {
            try {
                const res = await getReminderPreference()
                this.form = { ...this.form, ...(res.data || {}) }
            } catch (e) {
                // 读取失败时保留默认值
            }
        },
        onTimeConfirm({ selectedValues }) {
            if (this.pickField) {
                this.form[this.pickField] = selectedValues.join(':')
            }
            this.pickField = ''
        },
        async save() {
            this.saving = true
            try {
                await saveReminderPreference(this.form)
                mSuccess('已保存')
            } catch (e) {
                mError('保存失败')
            } finally {
                this.saving = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.preference-page {
    padding-top: 12px;
}

.pref-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--pnkx-text);
    line-height: 1.3;
}

.pref-hint {
    font-size: 12px;
    color: var(--pnkx-text-muted);
    line-height: 1.5;
    margin-top: 3px;
}

.quiet-section-title {
    padding: 14px 16px 4px;
    font-size: 14px;
    font-weight: 600;
    color: var(--pnkx-text);
}

.quiet-hint {
    padding: 6px 16px 14px;
}
</style>
