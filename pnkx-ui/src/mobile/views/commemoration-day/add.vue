<template>
    <div class="m-form commemoration-add-page">
        <!-- 基本信息 -->
        <div class="m-cell-card">
            <van-field
                v-model="formData.name"
                label="纪念日名称"
                placeholder="请输入纪念日名称"
                maxlength="50"
                :clearable="true"
            />
        </div>

        <!-- 到期提醒（编辑态） -->
        <div v-if="formData.id" class="m-cell-card">
            <van-cell is-link @click="openReminder">
                <template #title>
                    <span>到期提醒</span>
                </template>
                <template #value>
                    <span>设置提醒</span>
                </template>
            </van-cell>
        </div>

        <div class="m-cell-card">
            <van-field
                :model-value="formData.date ? formData.date.substring(0, 10) : '请选择日期'"
                label="纪念日日期"
                readonly
                is-link
                placeholder="请选择日期"
                @click="showDatePicker = true"
            />
            <van-cell title="重复提醒" is-link :value="repeatLabel" @click="showRepeatPicker = true" />
            <van-cell title="选择图标" is-link @click="openIconPicker">
                <template #value>
                    <span class="icon-preview">
                        <img :src="displayIcon" alt="">
                    </span>
                </template>
            </van-cell>
        </div>

        <!-- 备注 -->
        <div class="m-cell-card">
            <van-field
                v-model="formData.remark"
                label="备注"
                type="textarea"
                rows="3"
                autosize
                maxlength="200"
                show-word-limit
                placeholder="请输入备注信息..."
            />
        </div>

        <!-- 准备待办（编辑态，复用首页组件） -->
        <div v-if="formData.id && preparationItems.length > 0" class="m-card preparation-section">
            <div class="preparation-title">准备待办</div>
            <CommemorationPreparations
                :items="preparationItems"
                :can-edit="preparationCanEdit"
                @changed="loadPreparations"
            />
        </div>

        <!-- 删除（编辑态） -->
        <div v-if="formData.id" class="delete-section">
            <van-button block plain type="danger" @click="handleDelete">删除纪念日</van-button>
        </div>

        <!-- 保存 -->
        <div class="m-sticky-actions">
            <van-button type="primary" round :loading="saving" @click="handleSave">保存</van-button>
        </div>

        <!-- 日期选择 -->
        <van-calendar
            v-model:show="showDatePicker"
            :min-date="pickerMinDate"
            :max-date="pickerMaxDate"
            :default-date="formData.date ? new Date(formData.date.substring(0, 10).replace(/-/g, '/')) : new Date()"
            @confirm="onDateConfirm"
        />

        <!-- 重复提醒选择 -->
        <van-popup v-model:show="showRepeatPicker" position="bottom" round>
            <div class="repeat-picker">
                <div class="repeat-picker__header">
                    <button class="repeat-picker__cancel" @click="showRepeatPicker = false">取消</button>
                    <span class="repeat-picker__title">重复提醒</span>
                    <button class="repeat-picker__confirm" @click="confirmRepeat">确定</button>
                </div>
                <div class="repeat-picker__options">
                    <button
                        class="repeat-picker__option"
                        :class="{ active: tempRepeat === false }"
                        @click="tempRepeat = false"
                    >
                        <span>不重复</span>
                        <van-icon v-if="tempRepeat === false" name="success" size="16" color="var(--pnkx-warning)" />
                    </button>
                    <button
                        class="repeat-picker__option"
                        :class="{ active: tempRepeat === true }"
                        @click="tempRepeat = true"
                    >
                        <span>每年重复</span>
                        <van-icon v-if="tempRepeat === true" name="success" size="16" color="var(--pnkx-warning)" />
                    </button>
                </div>
            </div>
        </van-popup>

        <IconPicker ref="iconPicker" v-model="formData.icon" />

        <ReminderSetting
            v-if="formData.id && formData.date"
            ref="reminderSetting"
            source-type="commemoration"
            :source-id="formData.id"
            :source-name="formData.name"
            :event-time="formData.date"
            :annual="formData.repeat"
        />
    </div>
</template>

<script>
import { getDay, addDay, updateDay, delDay } from '@/api/px/life/commemorationDay'
import { getActionCenter } from '@/api/px/life/actionCenter'
import { mConfirm, mSuccess, mError, mLoading, closeToast, showToast, showDialog } from '@/mobile/utils/ui'
import { iconUrl } from '@/mobile/utils/icons'
import ReminderSetting from '@/mobile/components/ReminderSetting/index.vue'
import IconPicker from './components/IconPicker.vue'
import CommemorationPreparations from '@/mobile/views/home/components/CommemorationPreparations.vue'

export default {
    name: 'MCommemorationDayAdd',
    components: {
        ReminderSetting,
        IconPicker,
        CommemorationPreparations
    },
    data() {
        return {
            formData: {
                id: null,
                name: '',
                date: '',
                repeat: false,
                icon: '',
                remark: ''
            },
            isEditMode: false,
            fromNotification: false,
            showRepeatPicker: false,
            showDatePicker: false,
            tempRepeat: false,
            preparationItems: [],
            preparationCanEdit: false,
            saving: false
        }
    },
    computed: {
        repeatLabel() {
            return this.formData.repeat ? '每年重复' : '不重复'
        },
        displayIcon() {
            return iconUrl(this.formData.icon) || iconUrl('jinianri')
        },
        pickerMinDate() {
            const year = new Date().getFullYear()
            return new Date(year - 50, 0, 1)
        },
        pickerMaxDate() {
            const year = new Date().getFullYear()
            return new Date(year + 50, 11, 31)
        }
    },
    mounted() {
        const query = this.$route.query
        this.fromNotification = query.fromNotification === '1'
        if (query.id) {
            this.isEditMode = true
            this.loadCommemorationDetail(query.id)
        } else {
            this.formData.date = this.formatDate(new Date())
        }
    },
    methods: {
        formatDate(date) {
            if (!date) return ''
            const d = new Date(date)
            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
        },
        async loadCommemorationDetail(id) {
            mLoading('加载中...')
            try {
                const response = await getDay(id)
                if (response.code === 200 && response.data) {
                    this.formData = {
                        ...this.formData,
                        ...response.data,
                        date: response.data.date ? String(response.data.date).replace(' ', 'T').substring(0, 10) : ''
                    }
                    this.loadPreparations()
                }
                closeToast()
            } catch (error) {
                if (this.fromNotification) {
                    closeToast()
                    showDialog({
                        title: '提醒已失效',
                        message: '对应纪念日可能已删除，将返回列表。'
                    }).then(() => {
                        this.$router.replace('/m/commemoration-day')
                    })
                    return
                }
                mError('加载失败')
            }
        },
        async loadPreparations() {
            if (!this.formData.id) return
            try {
                const response = await getActionCenter({ scope: 'personal' })
                const data = response.data || response || {}
                const commemorations = data.commemorations || []
                this.preparationItems = commemorations.filter(item => String(item.id) === String(this.formData.id))
                this.preparationCanEdit = !!data.canEdit
            } catch (error) {
                this.preparationItems = []
            }
        },
        openReminder() {
            this.$refs.reminderSetting.open()
        },
        openIconPicker() {
            this.$refs.iconPicker.open()
        },
        confirmRepeat() {
            this.formData.repeat = this.tempRepeat
            this.showRepeatPicker = false
        },
        onDateConfirm(date) {
            this.formData.date = this.formatDate(date)
            this.showDatePicker = false
        },
        async handleSave() {
            if (!this.validateForm()) {
                return
            }

            this.saving = true
            try {
                // 时间格式为 yyyy-MM-dd HH:mm:ss
                const data = {
                    ...this.formData,
                    date: this.$moment(this.formData.date).format('YYYY-MM-DD HH:mm:ss')
                }
                const response = this.isEditMode ? await updateDay(data) : await addDay(data)

                if (response.code === 200) {
                    mSuccess('保存成功')
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
        validateForm() {
            if (!this.formData.name) {
                showToast('请输入纪念日名称')
                return false
            }

            if (!this.formData.date) {
                showToast('请选择纪念日日期')
                return false
            }

            return true
        },
        handleDelete() {
            mConfirm('确认删除', '确定要删除这个纪念日吗？').then(async () => {
                mLoading('删除中...')
                try {
                    const response = await delDay(this.formData.id)
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
    },
    watch: {
        showRepeatPicker(val) {
            if (val) {
                this.tempRepeat = this.formData.repeat
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.commemoration-add-page {
    padding-bottom: 24px;
}

.icon-preview {
    display: inline-flex;
    width: 26px;
    height: 26px;
    align-items: center;
    justify-content: center;

    img {
        width: 22px;
        height: 22px;
        object-fit: contain;
    }
}

.preparation-section {
    padding: 12px;
}

.preparation-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--pnkx-text);
    margin-bottom: 8px;
}

.delete-section {
    margin-top: 4px;
}

.repeat-picker {
    padding: 14px 16px calc(14px + env(safe-area-inset-bottom));
    background: var(--pnkx-surface);
    border-radius: 14px 14px 0 0;

    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-bottom: 10px;
        border-bottom: 1px solid var(--pnkx-border-soft);

        .repeat-picker__cancel {
            font-size: 14px;
            color: var(--pnkx-text-muted);
            background: transparent;
            border: none;
        }

        .repeat-picker__title {
            font-size: 16px;
            font-weight: 600;
            color: var(--pnkx-text);
        }

        .repeat-picker__confirm {
            font-size: 14px;
            color: var(--pnkx-warning);
            background: transparent;
            border: none;
        }
    }

    &__options {
        padding-top: 6px;
    }

    &__option {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        padding: 13px 4px;
        font-size: 14px;
        color: var(--pnkx-text);
        background: transparent;
        border: none;

        &.active {
            color: var(--pnkx-warning);
        }
    }
}
</style>
