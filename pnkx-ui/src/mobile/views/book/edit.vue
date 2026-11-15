<template>
    <div class="m-book-edit m-form">
        <!-- 封面预览 -->
        <div class="book-edit-hero">
            <div class="book-edit-hero__cover" :class="'scheme-' + coverScheme">
                <van-icon name="bookmark" size="40" class="book-edit-hero__icon" />
                <span class="book-edit-hero__spine"></span>
            </div>
            <span class="m-text-muted">{{ isEdit ? '编辑书籍信息' : '新建一本书' }}</span>
        </div>

        <!-- 表单 -->
        <van-cell-group inset class="m-cell-card">
            <van-field
                v-model="formData.title"
                label="书名"
                placeholder="请输入书名"
                maxlength="200"
                clearable
                required
                :rules="[{ required: true }]"
            />
            <van-field
                v-model="formData.author"
                label="作者"
                placeholder="请输入作者"
                maxlength="100"
                clearable
            />
            <van-field
                :model-value="statusLabel"
                label="阅读状态"
                placeholder="请选择"
                readonly
                is-link
                @click="showStatusPicker = true"
            />
            <van-field
                :model-value="formData.targetFinishDate"
                label="目标读完日期"
                placeholder="请选择日期"
                readonly
                clearable
                is-link
                @click="showDatePicker = true"
                @clear="formData.targetFinishDate = ''"
            />
        </van-cell-group>

        <van-cell-group inset class="m-cell-card">
            <van-field
                v-model="formData.description"
                label="简介"
                type="textarea"
                placeholder="写点这本书的简介吧..."
                maxlength="1000"
                show-word-limit
                rows="4"
                autosize
            />
        </van-cell-group>

        <!-- 元信息 -->
        <van-cell-group v-if="isEdit" inset class="m-cell-card book-edit-meta">
            <van-cell title="章节数" :value="(formData.chapterCount || 0) + ' 章'" />
            <van-cell v-if="formData.createTime" title="创建于" :value="formData.createTime" />
            <van-cell v-if="formData.lastReadTime" title="最近阅读" :value="formData.lastReadTime" />
        </van-cell-group>

        <!-- 删除 -->
        <div v-if="isEdit" class="book-edit-delete">
            <van-button block plain type="danger" @click="handleDelete">
                <van-icon name="delete-o" /> 删除书籍
            </van-button>
        </div>

        <!-- 底部保存 -->
        <div class="m-sticky-actions">
            <van-button type="primary" round block :loading="saving" @click="handleSave">保存</van-button>
        </div>

        <!-- 状态选择 -->
        <van-popup v-model:show="showStatusPicker" position="bottom" round teleport="body">
            <van-picker
                title="阅读状态"
                :columns="statusColumns"
                :model-value="statusValues"
                @confirm="onStatusConfirm"
                @cancel="showStatusPicker = false"
            />
        </van-popup>

        <!-- 日期选择 -->
        <van-popup v-model:show="showDatePicker" position="bottom" round teleport="body">
            <div class="book-edit-date">
                <div class="book-edit-date__header">
                    <span class="book-edit-date__clear" @click="clearTargetDate">清除</span>
                    <span class="book-edit-date__title">目标读完日期</span>
                    <span class="book-edit-date__close" @click="showDatePicker = false">取消</span>
                </div>
                <van-date-picker
                    v-model="dateParts"
                    title="选择日期"
                    :min-date="minDate"
                    :max-date="maxDate"
                    :columns-type="['year', 'month', 'day']"
                    @confirm="onDateConfirm"
                />
            </div>
        </van-popup>
    </div>
</template>

<script>
import { getBook, addBook, updateBook, deleteBooks } from '@/api/px/app/book'
import { mConfirm, mError, mLoading, closeToast, mSuccess, showToast } from '@/mobile/utils/ui'

export default {
    name: 'MBookEdit',
    data() {
        return {
            isEdit: false,
            saving: false,
            formData: {
                id: null,
                title: '',
                author: '',
                description: '',
                status: 'reading',
                targetFinishDate: ''
            },
            statusOptions: [
                { value: 'reading', label: '在读' },
                { value: 'finished', label: '已读完' },
                { value: 'shelved', label: '书架' }
            ],
            showStatusPicker: false,
            showDatePicker: false,
            dateParts: [],
            minDate: new Date(Date.now() - 5 * 365 * 24 * 3600 * 1000),
            maxDate: new Date(Date.now() + 5 * 365 * 24 * 3600 * 1000)
        }
    },
    computed: {
        coverScheme() {
            return String((this.formData.id || 0) % 5)
        },
        statusLabel() {
            const map = { reading: '在读', finished: '已读完', shelved: '书架' }
            return map[this.formData.status] || '在读'
        },
        statusColumns() {
            return this.statusOptions.map(opt => ({ text: opt.label, value: opt.value }))
        },
        statusValues() {
            return [this.formData.status || 'reading']
        }
    },
    watch: {
        showDatePicker(val) {
            if (val) this.syncDateParts()
        }
    },
    mounted() {
        const id = this.$route.query.id
        if (id && id !== 'undefined') {
            this.isEdit = true
            this.loadBook(id)
        }
    },
    methods: {
        async loadBook(id) {
            try {
                mLoading('加载中...')
                const res = await getBook(id)
                if (res.code === 200 && res.data) {
                    this.formData = { ...this.formData, ...res.data }
                    // 去掉可能带有的时间部分，保留 yyyy-MM-dd
                    if (this.formData.targetFinishDate) {
                        this.formData.targetFinishDate = String(this.formData.targetFinishDate).slice(0, 10)
                    }
                }
            } catch (e) {
                console.error('加载书籍失败:', e)
                showToast('加载失败')
            } finally {
                closeToast()
            }
        },

        onStatusConfirm({ selectedOptions }) {
            const option = selectedOptions && selectedOptions[0]
            if (option) this.formData.status = option.value
            this.showStatusPicker = false
        },

        syncDateParts() {
            const value = this.formData.targetFinishDate
            if (value && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
                this.dateParts = value.split('-')
            } else {
                const now = new Date()
                this.dateParts = [
                    String(now.getFullYear()),
                    String(now.getMonth() + 1).padStart(2, '0'),
                    String(now.getDate()).padStart(2, '0')
                ]
            }
        },

        onDateConfirm({ selectedValues }) {
            // van-date-picker columns-type=year/month/day 时 selectedValues 为 ['2024','01','31']
            const [y, m, d] = selectedValues
            this.formData.targetFinishDate = `${y}-${m}-${d}`
            this.showDatePicker = false
        },

        clearTargetDate() {
            this.formData.targetFinishDate = ''
            this.showDatePicker = false
        },

        validateForm() {
            if (!this.formData.title || !this.formData.title.trim()) {
                showToast('请输入书名')
                return false
            }
            return true
        },

        async handleSave() {
            if (this.saving || !this.validateForm()) return
            this.saving = true
            try {
                mLoading('保存中...')
                const payload = {
                    title: this.formData.title.trim(),
                    author: (this.formData.author || '').trim(),
                    description: this.formData.description || '',
                    status: this.formData.status || 'reading',
                    targetFinishDate: this.formData.targetFinishDate || null
                }
                if (this.isEdit) payload.id = this.formData.id
                const res = await (this.isEdit ? updateBook(payload) : addBook(payload))
                if (res.code === 200) {
                    mSuccess('保存成功')
                    setTimeout(() => this.$router.back(), 800)
                }
            } catch (e) {
                console.error('保存书籍失败:', e)
                mError('保存失败')
            } finally {
                closeToast()
                this.saving = false
            }
        },

        async handleDelete() {
            try {
                await mConfirm('确认删除', `确定要删除《${this.formData.title}》吗？所有章节将一并删除。`)
            } catch (e) {
                return
            }
            try {
                mLoading('删除中...')
                const r = await deleteBooks(this.formData.id)
                if (r.code === 200) {
                    mSuccess('删除成功')
                    setTimeout(() => this.$router.back(), 800)
                }
            } catch (e) {
                console.error('删除书籍失败:', e)
                mError('删除失败')
            } finally {
                closeToast()
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.m-book-edit {
    padding-top: 12px;
    padding-bottom: calc(88px + env(safe-area-inset-bottom));
}

.book-edit-hero {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 8px 0 16px;

    &__cover {
        position: relative;
        width: 70px;
        height: 95px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 10px;
        overflow: hidden;
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.16);

        &.scheme-0 { background: linear-gradient(135deg, var(--pnkx-primary), var(--pnkx-info)); }
        &.scheme-1 { background: linear-gradient(135deg, var(--pnkx-danger), var(--pnkx-warning)); }
        &.scheme-2 { background: linear-gradient(135deg, var(--pnkx-success), var(--pnkx-info)); }
        &.scheme-3 { background: linear-gradient(135deg, var(--pnkx-warning), var(--pnkx-danger)); }
        &.scheme-4 { background: linear-gradient(135deg, var(--pnkx-info), var(--pnkx-primary)); }
    }

    &__icon {
        color: rgba(255, 255, 255, 0.9);
    }

    &__spine {
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 5px;
        background: rgba(0, 0, 0, 0.14);
    }
}

.book-edit-meta {
    :deep(.van-cell__title) {
        color: var(--pnkx-text-secondary);
        font-size: 13px;
    }

    :deep(.van-cell__value) {
        color: var(--pnkx-text);
        font-size: 13px;
    }
}

.book-edit-delete {
    margin: 4px 16px 0;
}

.book-edit-date {
    background: var(--pnkx-surface);

    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 16px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__clear,
    &__close {
        font-size: 13px;
        color: var(--pnkx-text-muted);
    }

    &__clear {
        color: var(--pnkx-danger);
    }
}
</style>
