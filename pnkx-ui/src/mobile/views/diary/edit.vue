<template>
    <div class="diary-edit m-form">
        <!-- 页头：标题 + 字数统计 -->
        <div class="edit-header">
            <div class="edit-header__main">
                <div class="edit-header__title">{{ isEditMode ? '编辑日记' : '写日记' }}</div>
                <div class="edit-header__subtitle">{{ formattedDateText }}</div>
            </div>
            <div class="edit-header__count">
                <span class="edit-header__count-num">{{ contentLength }}</span>
                <span class="edit-header__count-label">字</span>
            </div>
        </div>

        <div class="editor-card">
            <!-- 日期 -->
            <van-cell center is-link title="日期" :value="diaryForm.date || '请选择日期'" @click="dateShow = true" />

            <!-- 心情 / 天气 -->
            <div class="picker-section">
                <MoodWeatherPicker
                    :mood="diaryForm.mood"
                    :weather="diaryForm.weather"
                    @mood-change="handleMoodChange"
                    @weather-change="handleWeatherChange"
                />
            </div>

            <!-- 标题 -->
            <van-field
                v-model="diaryForm.title"
                class="title-field"
                placeholder="给今天起个标题"
                maxlength="100"
            />

            <!-- 正文 -->
            <van-field
                v-model="diaryForm.content"
                type="textarea"
                class="content-field"
                placeholder="写下今天发生了什么，或只是此刻的心情..."
                maxlength="10000"
                rows="12"
                autosize
            />
        </div>

        <!-- 删除 -->
        <div v-if="diaryForm.id" class="danger-zone">
            <van-button block plain type="danger" icon="delete-o" @click="handleDelete">删除这篇日记</van-button>
        </div>

        <!-- 底部操作 -->
        <div class="m-sticky-actions">
            <span class="save-hint">{{ saveHint }}</span>
            <van-button round type="primary" class="save-btn" :loading="saving" @click="handleSave">保存</van-button>
        </div>

        <!-- 日期选择 -->
        <van-calendar
            v-model:show="dateShow"
            title="选择日期"
            :min-date="minDate"
            :max-date="maxDate"
            :default-date="defaultCalendarDate"
            @confirm="onDateConfirm"
        />
    </div>
</template>

<script>
import { getDiary, addDiary, updateDiary, delDiary } from '@/api/px/life/diary'
import { showToast, mSuccess, mError, mConfirm, mLoading, closeToast } from '@/mobile/utils/ui'
import MoodWeatherPicker from './components/MoodWeatherPicker.vue'

const WEEK_LIST = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export default {
    name: 'MDiaryEdit',
    components: {
        MoodWeatherPicker
    },
    data() {
        return {
            isEditMode: false,
            saving: false,
            dateShow: false,
            diaryForm: {
                id: null,
                title: '',
                content: '',
                mood: '',
                weather: '',
                date: ''
            }
        }
    },
    computed: {
        contentLength() {
            return (this.diaryForm.content || '').length
        },
        formattedDateText() {
            if (!this.diaryForm.date) return '选择一个日期'
            const date = new Date(String(this.diaryForm.date).replace(/-/g, '/'))
            if (Number.isNaN(date.getTime())) return this.diaryForm.date
            return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${WEEK_LIST[date.getDay()]}`
        },
        saveHint() {
            if (!this.diaryForm.date) return '先选择日期'
            if (!this.contentLength) return '正文还空着'
            return this.diaryForm.title ? '准备好了' : '未填标题将自动生成'
        },
        minDate() {
            const year = new Date().getFullYear()
            return new Date(year - 10, 0, 1)
        },
        maxDate() {
            const now = new Date()
            return new Date(now.getFullYear(), now.getMonth() + 6, now.getDate())
        },
        defaultCalendarDate() {
            if (!this.diaryForm.date) return new Date()
            const date = new Date(String(this.diaryForm.date).replace(/-/g, '/'))
            return Number.isNaN(date.getTime()) ? new Date() : date
        }
    },
    mounted() {
        const query = this.$route.query
        if (query.id && query.id !== 'undefined') {
            this.isEditMode = true
            this.loadDiaryDetail(query.id)
        } else {
            this.isEditMode = false
            this.initNewDiary()
            if (query.date) this.diaryForm.date = query.date
        }
    },
    methods: {
        formatDate(date) {
            if (!date) return ''
            const d = new Date(date)
            if (Number.isNaN(d.getTime())) return ''
            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
        },
        initNewDiary() {
            this.diaryForm.date = this.formatDate(new Date())
        },
        async loadDiaryDetail(id) {
            mLoading('加载中...')
            try {
                const response = await getDiary(id)
                if (response.code === 200 && response.data) {
                    const data = response.data
                    this.diaryForm = {
                        id: data.id || id,
                        title: data.title || '',
                        content: data.content || data.richText || '',
                        mood: data.mood || '',
                        weather: data.weather || '',
                        date: this.formatDate(data.date) || ''
                    }
                }
            } catch (error) {
                console.error('加载日记详情失败:', error)
                mError('加载失败')
            } finally {
                closeToast()
            }
        },
        handleMoodChange(mood) {
            this.diaryForm.mood = mood
        },
        handleWeatherChange(weather) {
            this.diaryForm.weather = weather
        },
        onDateConfirm(date) {
            this.diaryForm.date = this.formatDate(date)
            this.dateShow = false
        },
        buildDefaultTitle() {
            const date = this.diaryForm.date || this.formatDate(new Date())
            return `${date} 随记`
        },
        validateForm() {
            if (!this.diaryForm.date) {
                showToast('请选择日期')
                return false
            }
            if (!this.diaryForm.content || !this.diaryForm.content.trim()) {
                showToast('请写一点内容')
                return false
            }
            return true
        },
        async handleSave() {
            if (this.saving || !this.validateForm()) return
            this.saving = true
            mLoading('保存中...')
            try {
                const data = {
                    ...this.diaryForm,
                    title: this.diaryForm.title || this.buildDefaultTitle(),
                    richText: this.diaryForm.content
                }
                const response = this.isEditMode ? await updateDiary(data) : await addDiary(data)
                if (response.code === 200) {
                    mSuccess('保存成功')
                    setTimeout(() => {
                        this.$router.back()
                    }, 800)
                }
            } catch (error) {
                console.error('保存日记失败:', error)
                mError('保存失败')
            } finally {
                this.saving = false
                closeToast()
            }
        },
        async handleDelete() {
            try {
                await mConfirm('删除确认', '确定删除这篇日记吗？')
            } catch (e) {
                return
            }
            mLoading('删除中...')
            try {
                const response = await delDiary(this.diaryForm.id)
                if (response.code === 200) {
                    mSuccess('删除成功')
                    setTimeout(() => {
                        this.$router.back()
                    }, 800)
                }
            } catch (error) {
                console.error('删除日记失败:', error)
                mError('删除失败')
            } finally {
                closeToast()
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.diary-edit {
    padding-top: 12px;
}

.edit-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    padding: 0 2px;

    &__main {
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;
    }

    &__title {
        font-size: 21px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.2;
    }

    &__subtitle {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__count {
        flex-shrink: 0;
        width: 58px;
        height: 58px;
        border-radius: 50%;
        background: var(--pnkx-primary-soft);
        border: 1px solid var(--pnkx-border-soft);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
    }

    &__count-num {
        max-width: 46px;
        font-size: 14px;
        font-weight: 700;
        color: var(--pnkx-primary);
        line-height: 1.1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__count-label {
        margin-top: 2px;
        font-size: 9px;
        color: var(--pnkx-text-muted);
    }
}

.editor-card {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.picker-section {
    padding: 12px 14px;
    border-top: 1px solid var(--pnkx-border-soft);
    border-bottom: 1px solid var(--pnkx-border-soft);
}

.title-field {
    padding: 14px;

    :deep(.van-field__control) {
        font-size: 19px;
        font-weight: 600;
        line-height: 1.35;
        color: var(--pnkx-text);
    }
}

.content-field {
    padding: 12px 14px 16px;

    :deep(.van-field__control) {
        min-height: 280px;
        font-size: 14px;
        line-height: 1.75;
        color: var(--pnkx-text);
    }
}

.danger-zone {
    margin-top: 20px;
}

.save-hint {
    flex: 1;
    min-width: 0;
    font-size: 12px;
    color: var(--pnkx-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.save-btn {
    width: 110px;
    flex: none;
}
</style>
