<template>
    <div class="obs-edit">
        <div class="obs-edit__body">
            <!-- 文件信息 -->
            <div class="m-card obs-info">
                <div class="obs-info__name">{{ fileName }}</div>
                <div class="obs-info__path">{{ filePath }}</div>
                <span v-if="modified" class="obs-info__modified">未保存</span>
            </div>

            <!-- 编辑/预览切换 -->
            <div class="obs-mode">
                <div class="obs-mode__item" :class="{ 'obs-mode__item--active': mode === 'edit' }" @click="mode = 'edit'">
                    <van-icon name="edit" size="14" />
                    <span>编辑</span>
                </div>
                <div class="obs-mode__item" :class="{ 'obs-mode__item--active': mode === 'preview' }" @click="mode = 'preview'">
                    <van-icon name="eye-o" size="14" />
                    <span>预览</span>
                </div>
            </div>

            <!-- 编辑区 -->
            <div v-if="mode === 'edit'" class="obs-editor">
                <van-field
                    v-model="content"
                    type="textarea"
                    placeholder="开始书写…"
                    :rows="16"
                    autosize
                    class="obs-editor__field"
                    @update:model-value="onContentChange"
                />
            </div>

            <!-- 预览区 -->
            <div v-else class="m-card obs-preview">
                <div v-if="isMarkdown" class="m-rich-content obs-preview__md" v-html="renderedHtml"></div>
                <pre v-else class="obs-preview__plain">{{ content }}</pre>
            </div>
        </div>

        <!-- 底部操作 -->
        <div class="m-sticky-actions">
            <van-button block type="primary" :loading="saving" :disabled="!modified" @click="handleSave">保存</van-button>
        </div>

        <!-- 未保存离开确认（自绘弹层：路由守卫挂起期间函数式弹窗会被自动关闭，van-dialog 模板用法在当前依赖下渲染为空，故用 van-popup 自绘） -->
        <van-popup :show="leaveDialogVisible" round teleport="body" :close-on-click-overlay="false" class="leave-dialog">
            <div class="leave-dialog__title">提示</div>
            <div class="leave-dialog__message">有未保存的修改，保存后离开？</div>
            <div class="leave-dialog__actions">
                <van-button block plain type="primary" @click="onLeaveDialogClose('cancel')">取消</van-button>
                <van-button block type="primary" @click="onLeaveDialogClose('confirm')">确定</van-button>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { readFile, writeFile } from '@/api/system/fileManager'
import { mSuccess, mError, mLoading, closeToast } from '@/mobile/utils/ui'

export default {
    name: 'MNoteObsidianEdit',
    data() {
        return {
            filePath: '',
            content: '',
            originalContent: '',
            modified: false,
            saving: false,
            mode: 'edit',
            leaveDialogVisible: false,
            leaveNext: null,
            leaving: false
        }
    },
    computed: {
        fileName() {
            if (!this.filePath) return ''
            return this.filePath.split('/').pop()
        },
        isMarkdown() {
            return (this.fileName || '').toLowerCase().endsWith('.md')
        },
        renderedHtml() {
            if (!this.isMarkdown) return ''
            try {
                const html = marked.parse(this.content || '', { gfm: true, breaks: true })
                return DOMPurify.sanitize(String(html))
            } catch (e) {
                console.error('Markdown 渲染失败:', e)
                return ''
            }
        }
    },
    mounted() {
        this.filePath = decodeURIComponent(String(this.$route.query.path || ''))
        this.loadFile()
    },
    beforeRouteLeave(to, from, next) {
        if (!this.modified) {
            next()
            return
        }
        // 有未保存修改：确定=保存后离开，取消=放弃修改离开
        // 常驻 van-dialog 承接确认（见模板注释），守卫先挂起
        this.leaveNext = next
        this.leaveDialogVisible = true
    },
    methods: {
        /**
         * 离开确认弹层按钮回调：action='confirm' 保存后离开；'cancel' 放弃修改离开
         */
        async onLeaveDialogClose(action) {
            if (this.leaving) return
            this.leaving = true
            try {
                if (action === 'confirm') {
                    const ok = await this.saveFile()
                    this.leaveDialogVisible = false
                    const next = this.leaveNext
                    this.leaveNext = null
                    if (next) next(ok !== false)
                    return
                }
                this.modified = false
                this.leaveDialogVisible = false
                const next = this.leaveNext
                this.leaveNext = null
                if (next) next()
            } finally {
                this.leaving = false
            }
        },
        loadFile() {
            mLoading('加载中')
            readFile(this.filePath).then(res => {
                const data = res.data || {}
                this.content = data.content || ''
                this.originalContent = this.content
            }).catch(() => {
                mError('读取失败')
            }).finally(() => {
                closeToast()
            })
        },
        onContentChange() {
            this.modified = this.content !== this.originalContent
        },
        async handleSave() {
            if (!this.modified || this.saving) return
            await this.saveFile()
        },
        async saveFile() {
            if (this.saving) return false
            this.saving = true
            try {
                await writeFile({ path: this.filePath, content: this.content })
                this.originalContent = this.content
                this.modified = false
                mSuccess('已保存')
                return true
            } catch (e) {
                console.error('保存失败:', e)
                mError('保存失败')
                return false
            } finally {
                this.saving = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.obs-edit {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);

    &__body {
        padding: 12px 12px calc(80px + env(safe-area-inset-bottom));
    }
}

.obs-info {
    position: relative;

    &__name {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        padding-right: 56px;
    }

    &__path {
        margin-top: 5px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__modified {
        position: absolute;
        top: 14px;
        right: 14px;
        padding: 2px 8px;
        border-radius: 999px;
        background: var(--pnkx-warning-soft);
        color: var(--pnkx-warning);
        font-size: 11px;
    }
}

.obs-mode {
    display: flex;
    gap: 4px;
    padding: 3px;
    margin-bottom: 12px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 999px;

    &__item {
        flex: 1;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 5px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
        border-radius: 999px;
        transition: all 0.25s;

        &--active {
            color: var(--pnkx-primary);
            background: var(--pnkx-primary-soft);
            font-weight: 600;
        }
    }
}

.obs-editor {
    &__field {
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 12px;
        background: var(--pnkx-surface);

        :deep(.van-field__control) {
            min-height: 380px;
            font-family: Menlo, Monaco, Consolas, monospace;
            font-size: 13px;
            line-height: 1.8;
            color: var(--pnkx-text);
        }
    }
}

.obs-preview {
    min-height: 200px;

    &__md {
        :deep(h1, h2, h3, h4) {
            margin: 12px 0 8px;
            line-height: 1.3;
        }

        :deep(h1) { font-size: 20px; }
        :deep(h2) { font-size: 17px; }
        :deep(h3) { font-size: 15px; }

        :deep(p) {
            margin: 8px 0;
        }

        :deep(ul), :deep(ol) {
            padding-left: 20px;
            margin: 8px 0;
        }

        :deep(blockquote) {
            margin: 8px 0;
            padding: 6px 10px;
            border-left: 3px solid var(--pnkx-primary);
            background: var(--pnkx-surface-muted);
            border-radius: 4px;
        }

        :deep(code) {
            background: var(--pnkx-surface-muted);
            border-radius: 4px;
            padding: 1px 5px;
        }

        :deep(a) {
            color: var(--pnkx-primary);
        }
    }

    &__plain {
        font-family: Menlo, Monaco, Consolas, monospace;
        font-size: 13px;
        line-height: 1.8;
        color: var(--pnkx-text);
        white-space: pre-wrap;
        word-break: break-word;
        margin: 0;
    }
}

// 离开确认弹层（teleport 到 body，样式非 scoped 生效）
.leave-dialog {
    width: calc(100vw - 64px);

    &__title {
        text-align: center;
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        padding: 20px 20px 0;
    }

    &__message {
        padding: 12px 20px 20px;
        font-size: 14px;
        line-height: 1.6;
        color: var(--pnkx-text-secondary);
        text-align: center;
    }

    &__actions {
        display: flex;
        gap: 10px;
        padding: 0 16px 16px;
    }
}
</style>
