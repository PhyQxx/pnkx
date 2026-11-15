<template>
    <div class="m-form menstruation-record-page">
        <!-- 头部说明 -->
        <div class="record-hero">
            <span class="record-hero__eyebrow">{{ recordId ? '编辑记录' : '新增记录' }}</span>
            <span class="record-hero__title">{{ form.date || '选择日期' }}</span>
            <span class="record-hero__text">把关键变化轻量记下来，日后看趋势会更省心。</span>
        </div>

        <!-- 记录日期 -->
        <div class="m-card form-section">
            <div class="section-title">
                <span class="section-title__main">记录日期</span>
                <span class="section-title__sub">默认带入当前选择的日期</span>
            </div>
            <van-cell-group inset :border="false">
                <van-field
                    :model-value="form.date || '请选择日期'"
                    label="日期"
                    readonly
                    is-link
                    placeholder="请选择日期"
                    @click="showDatePicker = true"
                />
            </van-cell-group>
        </div>

        <!-- 经期状态 -->
        <div class="m-card form-section">
            <div class="section-title">
                <span class="section-title__main">经期状态</span>
                <span class="section-title__sub">开始和结束只能选择一个</span>
            </div>

            <div class="toggle-row">
                <div class="toggle-card" :class="{ active: form.come }" @click="toggleCome">
                    <span class="toggle-card__icon toggle-card__icon--start">
                        <van-icon name="like" size="14" color="#fff" />
                    </span>
                    <div class="toggle-card__text">
                        <span class="toggle-card__title">姨妈来了</span>
                        <span class="toggle-card__desc">标记经期开始</span>
                    </div>
                    <van-switch :model-value="form.come" size="20" @click.stop @update:model-value="onComeChange" />
                </div>

                <div class="toggle-card" :class="{ active: form.go }" @click="toggleGo">
                    <span class="toggle-card__icon toggle-card__icon--end">
                        <van-icon name="checked" size="15" color="#fff" />
                    </span>
                    <div class="toggle-card__text">
                        <span class="toggle-card__title">姨妈走了</span>
                        <span class="toggle-card__desc">标记经期结束</span>
                    </div>
                    <van-switch :model-value="form.go" size="20" @click.stop @update:model-value="onGoChange" />
                </div>
            </div>
        </div>

        <!-- 身体数据 -->
        <div class="m-card form-section">
            <div class="section-title">
                <span class="section-title__main">身体数据</span>
                <span class="section-title__sub">选填，适合记录体温和体重变化</span>
            </div>
            <div class="field-grid">
                <van-field v-model="form.temperature" type="number" label="体温" placeholder="℃" />
                <van-field v-model="form.weight" type="number" label="体重" placeholder="kg" />
            </div>
        </div>

        <!-- 其他信息 -->
        <div class="m-card form-section">
            <div class="section-title">
                <span class="section-title__main">其他信息</span>
                <span class="section-title__sub">检查、同房和备注会展示在记录卡片中</span>
            </div>

            <div class="toggle-card toggle-card--single" :class="{ active: form.makeLove }" @click="toggleMakeLove">
                <span class="toggle-card__icon toggle-card__icon--soft">
                    <van-icon name="like-o" size="14" color="var(--pnkx-danger)" />
                </span>
                <div class="toggle-card__text">
                    <span class="toggle-card__title">有同房记录</span>
                    <span class="toggle-card__desc">用于后续回看周期事件</span>
                </div>
                <van-switch :model-value="form.makeLove" size="20" @click.stop @update:model-value="onMakeLoveChange" />
            </div>

            <div class="field-list">
                <van-field v-model="form.items" label="检查项目" placeholder="例如：排卵试纸、B超、激素检查" />
                <van-field
                    v-model="form.results"
                    label="检查结果"
                    type="textarea"
                    rows="2"
                    autosize
                    maxlength="500"
                    show-word-limit
                    placeholder="记录检查结果或医生建议"
                />
                <van-field
                    v-model="form.remark"
                    label="备注"
                    type="textarea"
                    rows="2"
                    autosize
                    maxlength="200"
                    show-word-limit
                    placeholder="身体感受、情绪、疼痛程度等"
                />
            </div>
        </div>

        <!-- 删除（编辑态） -->
        <div v-if="recordId" class="delete-section">
            <van-button block plain type="danger" @click="handleDelete">删除记录</van-button>
        </div>

        <!-- 保存 -->
        <div class="m-sticky-actions">
            <van-button type="primary" round :loading="saving" @click="handleSave">保存记录</van-button>
        </div>

        <!-- 日期选择 -->
        <van-calendar
            v-model:show="showDatePicker"
            :min-date="pickerMinDate"
            :max-date="pickerMaxDate"
            :default-date="form.date ? new Date(form.date.replace(/-/g, '/')) : new Date()"
            @confirm="onDateConfirm"
        />
    </div>
</template>

<script>
import {
    getMenstruationRecord,
    addMenstruationRecord,
    updateMenstruationRecord,
    delMenstruationRecord
} from '@/api/px/life/menstruationRecord'
import { mConfirm, mSuccess, mError, mLoading, closeToast, showToast } from '@/mobile/utils/ui'

export default {
    name: 'MMenstruationRecord',
    data() {
        return {
            recordId: null,
            form: {
                date: '',
                come: false,
                go: false,
                makeLove: false,
                temperature: '',
                weight: '',
                items: '',
                results: '',
                remark: ''
            },
            showDatePicker: false,
            saving: false
        }
    },
    computed: {
        pickerMinDate() {
            const year = new Date().getFullYear()
            return new Date(year - 3, 0, 1)
        },
        pickerMaxDate() {
            return new Date()
        }
    },
    mounted() {
        const query = this.$route.query
        if (query.id) {
            this.recordId = query.id
            this.loadRecordDetail()
        } else if (query.date) {
            this.form.date = String(query.date).substring(0, 10)
        } else {
            this.form.date = this.formatDate(new Date())
        }
    },
    methods: {
        formatDate(date) {
            if (!date) return ''
            const d = new Date(date)
            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
        },
        async loadRecordDetail() {
            mLoading('加载中...')
            try {
                const response = await getMenstruationRecord(this.recordId)
                if (response.code === 200) {
                    const data = response.data
                    this.form = {
                        date: data.date ? data.date.substring(0, 10) : '',
                        come: data.type === '0',
                        go: data.type === '1',
                        makeLove: !!data.makeLove,
                        temperature: data.temperature !== null && data.temperature !== undefined ? String(data.temperature) : '',
                        weight: data.weight !== null && data.weight !== undefined ? String(data.weight) : '',
                        items: data.items || '',
                        results: data.results || '',
                        remark: data.remark || ''
                    }
                }
                closeToast()
            } catch (error) {
                mError('加载失败')
            }
        },
        onDateConfirm(date) {
            this.form.date = this.formatDate(date)
            this.showDatePicker = false
        },
        toggleCome() {
            this.form.come = !this.form.come
            if (this.form.come) this.form.go = false
        },
        toggleGo() {
            this.form.go = !this.form.go
            if (this.form.go) this.form.come = false
        },
        toggleMakeLove() {
            this.form.makeLove = !this.form.makeLove
        },
        onComeChange(value) {
            this.form.come = value
            if (this.form.come) this.form.go = false
        },
        onGoChange(value) {
            this.form.go = value
            if (this.form.go) this.form.come = false
        },
        onMakeLoveChange(value) {
            this.form.makeLove = value
        },
        async handleSave() {
            if (!this.form.date) {
                showToast('请选择日期')
                return
            }

            this.saving = true
            try {
                const data = {
                    date: this.form.date,
                    makeLove: this.form.makeLove,
                    temperature: this.form.temperature ? parseFloat(this.form.temperature) : null,
                    weight: this.form.weight ? parseFloat(this.form.weight) : null,
                    items: this.form.items,
                    results: this.form.results,
                    remark: this.form.remark
                }

                if (this.form.come) {
                    data.type = '0'
                } else if (this.form.go) {
                    data.type = '1'
                } else {
                    data.type = null
                }

                let response
                if (this.recordId) {
                    data.id = this.recordId
                    response = await updateMenstruationRecord(data)
                } else {
                    response = await addMenstruationRecord(data)
                }

                if (response.code === 200) {
                    mSuccess(this.recordId ? '修改成功' : '添加成功')
                    setTimeout(() => {
                        this.$router.back()
                    }, 800)
                }
            } catch (error) {
                mError('保存失败')
            } finally {
                this.saving = false
            }
        },
        handleDelete() {
            mConfirm('确认删除', '确定要删除这条记录吗？').then(async () => {
                mLoading('删除中...')
                try {
                    const response = await delMenstruationRecord(this.recordId)
                    if (response.code === 200) {
                        mSuccess('删除成功')
                        setTimeout(() => {
                            this.$router.back()
                        }, 800)
                    } else {
                        closeToast()
                    }
                } catch (error) {
                    mError('删除失败')
                }
            }).catch(() => {})
        }
    }
}
</script>

<style lang="scss" scoped>
// 底部留白交给 .m-form（固定保存栏遮挡高度），此处不得再覆盖
.record-hero {
    padding: 15px;
    border-radius: 14px;
    margin-bottom: 11px;
    background: linear-gradient(135deg, var(--pnkx-danger-soft) 0%, var(--pnkx-surface) 62%, var(--pnkx-primary-soft) 100%);
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
        font-size: 21px;
        font-weight: 800;
        color: var(--pnkx-text);
    }

    &__text {
        display: block;
        margin-top: 6px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        line-height: 1.5;
    }
}

.form-section {
    padding: 13px;
}

.section-title {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 11px;

    &__main {
        display: block;
        font-size: 15px;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__sub {
        display: block;
        margin-top: 3px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.toggle-row {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.toggle-card {
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 52px;
    padding: 9px;
    border-radius: 10px;
    background: var(--pnkx-surface-muted);
    border: 1px solid transparent;

    &--single {
        margin-bottom: 11px;
    }

    &.active {
        border-color: var(--pnkx-danger);
        background: var(--pnkx-danger-soft);
    }

    &__icon {
        width: 29px;
        height: 29px;
        border-radius: 9px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        &--start {
            background: var(--pnkx-danger);
        }

        &--end {
            background: var(--pnkx-primary);
        }

        &--soft {
            background: var(--pnkx-danger-soft);
        }
    }

    &__text {
        flex: 1;
        min-width: 0;
    }

    &__title {
        display: block;
        font-size: 14px;
        color: var(--pnkx-text);
        font-weight: 700;
    }

    &__desc {
        display: block;
        margin-top: 3px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.field-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;

    :deep(.van-field) {
        background: var(--pnkx-surface-muted);
        border-radius: 8px;
    }
}

.field-list {
    display: flex;
    flex-direction: column;
    gap: 8px;

    :deep(.van-field) {
        background: var(--pnkx-surface-muted);
        border-radius: 8px;
    }
}

.delete-section {
    margin-top: 4px;
}
</style>
