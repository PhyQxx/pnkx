<template>
    <div class="m-form menstruation-settings-page">
        <!-- 预测偏好 -->
        <div class="settings-hero">
            <span class="settings-hero__eyebrow">预测偏好</span>
            <span class="settings-hero__title">让提醒更贴近你的节律</span>
            <span class="settings-hero__text">周期和经期时长会用于主页预测，不会影响历史记录。</span>
        </div>

        <!-- 设置卡片 -->
        <div class="m-card setting-card">
            <div class="setting-card__main">
                <span class="setting-card__label">经期周期</span>
                <span class="setting-card__tip">从本次经期第一天到下次经期第一天</span>
            </div>
            <div class="setting-card__control">
                <van-stepper v-model="cycle" min="21" max="35" integer theme="danger" />
                <span class="setting-card__unit">天</span>
            </div>
        </div>

        <div class="m-card setting-card">
            <div class="setting-card__main">
                <span class="setting-card__label">经期时长</span>
                <span class="setting-card__tip">每次经期通常持续的天数</span>
            </div>
            <div class="setting-card__control">
                <van-stepper v-model="duration" min="2" max="8" integer theme="danger" />
                <span class="setting-card__unit">天</span>
            </div>
        </div>

        <div v-if="lastStart && predictedDate" class="m-cell-card">
            <van-cell is-link @click="openReminder">
                <template #title>
                    <div class="setting-card__label">下次经期提醒</div>
                    <div class="setting-card__tip">预测日期 {{ predictedDate }}，可设置提前提醒</div>
                </template>
            </van-cell>
        </div>

        <!-- 参考范围 -->
        <div class="m-card info-section">
            <div class="info-section__header">
                <span class="info-section__icon">
                    <van-icon name="info-o" size="14" color="var(--pnkx-info)" />
                </span>
                <span class="info-section__title">参考范围</span>
            </div>
            <div class="info-list">
                <div class="info-item">
                    <span class="info-item__dot"></span>
                    <span>常见月经周期为 21-35 天，平均约 28 天。</span>
                </div>
                <div class="info-item">
                    <span class="info-item__dot"></span>
                    <span>常见经期时长为 2-8 天，平均约 4-6 天。</span>
                </div>
                <div class="info-item">
                    <span class="info-item__dot"></span>
                    <span>预测仅作记录参考，身体不适时建议及时就医。</span>
                </div>
            </div>
        </div>

        <!-- 保存 -->
        <div class="m-sticky-actions">
            <van-button type="primary" round @click="handleSave">保存设置</van-button>
        </div>

        <ReminderSetting
            v-if="lastStart && predictedDate"
            ref="reminderSetting"
            source-type="menstruation"
            :source-id="lastStart.id"
            source-name="下次经期"
            :event-time="predictedDate"
        />
    </div>
</template>

<script>
import { getLastStartDate } from '@/api/px/life/menstruationRecord'
import { mSuccess } from '@/mobile/utils/ui'
import ReminderSetting from '@/mobile/components/ReminderSetting/index.vue'

export default {
    name: 'MMenstruationSettings',
    components: {
        ReminderSetting
    },
    data() {
        return {
            cycle: 28,
            duration: 5,
            lastStart: null
        }
    },
    computed: {
        predictedDate() {
            if (!this.lastStart || !this.lastStart.date) return ''
            const date = new Date(String(this.lastStart.date).replace(/-/g, '/'))
            date.setDate(date.getDate() + Number(this.cycle || 28))
            const pad = value => String(value).padStart(2, '0')
            return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
        }
    },
    mounted() {
        this.loadSettings()
        this.loadLastStart()
    },
    methods: {
        loadSettings() {
            const cycle = localStorage.getItem('menstruation_cycle')
            const duration = localStorage.getItem('menstruation_duration')
            if (cycle) this.cycle = parseInt(cycle)
            if (duration) this.duration = parseInt(duration)
        },
        async loadLastStart() {
            try {
                const response = await getLastStartDate()
                this.lastStart = response.data || null
            } catch (error) {
                // 静默失败，不展示提醒入口
            }
        },
        openReminder() {
            this.$refs.reminderSetting.open()
        },
        handleSave() {
            localStorage.setItem('menstruation_cycle', this.cycle.toString())
            localStorage.setItem('menstruation_duration', this.duration.toString())
            mSuccess('保存成功')
            setTimeout(() => {
                this.$router.back()
            }, 800)
        }
    }
}
</script>

<style lang="scss" scoped>
.settings-hero {
    padding: 15px;
    border-radius: 14px;
    margin-bottom: 11px;
    background: linear-gradient(135deg, var(--pnkx-danger-soft) 0%, var(--pnkx-surface) 60%, var(--pnkx-primary-soft) 100%);
    border: 1px solid var(--pnkx-border-soft);

    &__eyebrow {
        display: block;
        font-size: 12px;
        color: var(--pnkx-danger);
        font-weight: 600;
    }

    &__title {
        display: block;
        margin-top: 5px;
        font-size: 19px;
        line-height: 1.25;
        font-weight: 800;
        color: var(--pnkx-text);
    }

    &__text {
        display: block;
        margin-top: 6px;
        font-size: 12px;
        line-height: 1.5;
        color: var(--pnkx-text-secondary);
    }
}

.setting-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    &__main {
        flex: 1;
        min-width: 0;
    }

    &__label {
        display: block;
        font-size: 15px;
        color: var(--pnkx-text);
        font-weight: 700;
    }

    &__tip {
        display: block;
        margin-top: 4px;
        font-size: 11px;
        line-height: 1.45;
        color: var(--pnkx-text-muted);
    }

    &__control {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;
    }

    &__unit {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }
}

.info-section {
    &__header {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 10px;
    }

    &__icon {
        width: 21px;
        height: 21px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background: var(--pnkx-info-soft);
    }

    &__title {
        font-size: 15px;
        color: var(--pnkx-text);
        font-weight: 700;
    }
}

.info-list {
    display: flex;
    flex-direction: column;
    gap: 7px;
}

.info-item {
    display: flex;
    align-items: flex-start;
    gap: 6px;

    &__dot {
        width: 5px;
        height: 5px;
        margin-top: 8px;
        border-radius: 50%;
        background: var(--pnkx-danger);
        flex-shrink: 0;
    }

    span:last-child {
        flex: 1;
        min-width: 0;
        font-size: 12px;
        line-height: 1.55;
        color: var(--pnkx-text-secondary);
    }
}
</style>
