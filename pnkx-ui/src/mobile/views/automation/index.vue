<template>
    <div class="m-page automation-page">
        <!-- 模板市场 -->
        <div class="m-section-title">模板市场</div>
        <div class="templates-scroll">
            <div class="templates-inner">
                <div v-for="item in templates" :key="item.code" class="template" @click="useTemplate(item)">
                    <span class="template__name">{{ item.name }}</span>
                    <span class="template__desc">{{ item.description }}</span>
                </div>
                <div v-if="templates.length === 0" class="m-empty templates-empty">
                    <span>暂无模板</span>
                </div>
            </div>
        </div>

        <!-- 规则列表 -->
        <div class="m-section-title">我的规则</div>
        <div v-if="rules.length === 0" class="m-empty">
            <van-icon name="setting-o" size="40" color="var(--pnkx-primary)" />
            <span>暂无自动化规则</span>
            <span class="m-text-muted">选择上方模板或点右下角按钮创建第一条规则</span>
        </div>

        <div v-for="rule in rules" :key="rule.id" class="m-card rule-card">
            <div class="rule-card__top">
                <div class="rule-card__info">
                    <span class="rule-card__name">{{ rule.name }}</span>
                    <span class="rule-card__meta">{{ triggerLabel(rule.triggerType) }} → {{ actionLabel(rule.actionType) }}</span>
                </div>
                <van-switch :model-value="rule.enabled" size="20" @change="value => toggle(rule, value)" />
            </div>
            <div class="rule-card__actions">
                <button @click="run(rule, true)">试运行</button>
                <button @click="run(rule, false)">立即执行</button>
                <button @click="openHistory(rule)">记录</button>
                <button @click="edit(rule)">编辑</button>
                <button class="rule-card__danger" @click="remove(rule)">删除</button>
            </div>
        </div>

        <button class="m-fab m-fab--no-tabbar" @click="createBlank">
            <van-icon name="plus" size="22" />
        </button>

        <!-- 规则编辑器 -->
        <van-popup v-model:show="editorShow" position="bottom" round safe-area-inset-bottom teleport="body">
            <div class="editor">
                <div class="editor__title">{{ form.id ? '编辑规则' : '新建规则' }}</div>
                <van-cell-group inset :border="false">
                    <van-field v-model="form.name" label="规则名称" placeholder="请输入规则名称" />
                    <van-field
                        :model-value="triggerLabel(form.triggerType)"
                        label="触发器"
                        readonly
                        is-link
                        @click="triggerPickerShow = true"
                    />
                    <van-field
                        :model-value="actionLabel(form.actionType)"
                        label="动作"
                        readonly
                        is-link
                        @click="actionPickerShow = true"
                    />
                    <van-field
                        v-if="form.triggerType === 'schedule'"
                        :model-value="form.nextRunTime || '请选择首次运行时间'"
                        label="首次运行"
                        readonly
                        is-link
                        placeholder="请选择首次运行时间"
                        @click="timePickerShow = true"
                    />
                </van-cell-group>

                <template v-if="form.triggerType === 'schedule'">
                    <div class="json-block">
                        <div class="json-block__head">
                            <span class="json-block__label">触发配置 JSON</span>
                            <span class="json-block__ops">
                                <button @click="validateJson('triggerConfig')">校验</button>
                                <button @click="formatJson('triggerConfig')">格式化</button>
                            </span>
                        </div>
                        <van-field
                            v-model="form.triggerConfig"
                            type="textarea"
                            rows="2"
                            autosize
                            class="json-block__field"
                            :placeholder="placeholders.triggerConfig"
                        />
                    </div>
                </template>

                <div class="json-block">
                    <div class="json-block__head">
                        <span class="json-block__label">条件 JSON</span>
                        <span class="json-block__ops">
                            <button @click="validateJson('conditionJson')">校验</button>
                            <button @click="formatJson('conditionJson')">格式化</button>
                        </span>
                    </div>
                    <van-field
                        v-model="form.conditionJson"
                        type="textarea"
                        rows="2"
                        autosize
                        class="json-block__field"
                        :placeholder="placeholders.conditionJson"
                    />
                </div>

                <div class="json-block">
                    <div class="json-block__head">
                        <span class="json-block__label">动作配置 JSON</span>
                        <span class="json-block__ops">
                            <button @click="validateJson('actionConfig')">校验</button>
                            <button @click="formatJson('actionConfig')">格式化</button>
                        </span>
                    </div>
                    <van-field
                        v-model="form.actionConfig"
                        type="textarea"
                        rows="2"
                        autosize
                        class="json-block__field"
                        :placeholder="placeholders.actionConfig"
                    />
                </div>

                <van-button block round type="primary" :loading="saving" @click="submit">保存规则</van-button>
            </div>
        </van-popup>

        <!-- 执行记录 -->
        <van-popup v-model:show="historyShow" position="bottom" round safe-area-inset-bottom teleport="body">
            <div class="history">
                <div class="editor__title">执行记录</div>
                <div v-if="executions.length === 0" class="m-empty">
                    <span>暂无执行记录</span>
                </div>
                <div v-for="item in executions" :key="item.id" class="execution">
                    <div class="execution__top">
                        <span class="execution__status" :class="'execution__status--' + item.status">{{ statusLabel(item.status) }}</span>
                        <span class="execution__time">{{ item.createTime }}</span>
                    </div>
                    <div v-if="item.errorMsg" class="execution__error">{{ item.errorMsg }}</div>
                    <div v-if="item.status === 'failed'" class="execution__retry" @click="retry(item)">重放</div>
                </div>
            </div>
        </van-popup>

        <!-- 触发器选择 -->
        <van-popup v-model:show="triggerPickerShow" position="bottom" round teleport="body">
            <van-picker
                title="触发器"
                :columns="triggerColumns"
                :model-value="[form.triggerType]"
                @cancel="triggerPickerShow = false"
                @confirm="onTriggerConfirm"
            />
        </van-popup>

        <!-- 动作选择 -->
        <van-popup v-model:show="actionPickerShow" position="bottom" round teleport="body">
            <van-picker
                title="动作"
                :columns="actionColumns"
                :model-value="[form.actionType]"
                @cancel="actionPickerShow = false"
                @confirm="onActionConfirm"
            />
        </van-popup>

        <!-- 首次运行时间 -->
        <van-popup v-model:show="timePickerShow" position="bottom" round teleport="body">
            <van-date-picker
                title="选择时间"
                :columns-type="['year', 'month', 'day', 'hour', 'minute']"
                :min-date="timeMinDate"
                :max-date="timeMaxDate"
                :model-value="timeValues"
                @cancel="timePickerShow = false"
                @confirm="onTimeConfirm"
            />
        </van-popup>
    </div>
</template>

<script>
import {
    listRules,
    listTemplates,
    saveRule,
    deleteRule,
    runRule,
    dryRunRule,
    listExecutions,
    retryExecution
} from '@/api/px/life/automation'
import { mConfirm, mSuccess, mError, mLoading, closeToast, showToast } from '@/mobile/utils/ui'

const emptyForm = () => ({
    name: '',
    templateCode: '',
    triggerType: 'manual',
    triggerConfig: '{}',
    conditionJson: '{}',
    actionType: 'create_todo',
    actionConfig: '{}',
    nextRunTime: '',
    enabled: true
})

export default {
    name: 'MAutomation',
    data() {
        return {
            rules: [],
            templates: [],
            executions: [],
            form: emptyForm(),
            editorShow: false,
            historyShow: false,
            triggerPickerShow: false,
            actionPickerShow: false,
            timePickerShow: false,
            saving: false,
            placeholders: {
                triggerConfig: '触发配置 JSON，例如 {"intervalMinutes":1440}',
                conditionJson: '条件 JSON，例如 {"daysBefore":7}',
                actionConfig: '动作配置 JSON，例如 {"content":"准备礼物"}'
            },
            triggerOptions: [
                { label: '手动', value: 'manual' },
                { label: '定时', value: 'schedule' },
                { label: '订阅到期', value: 'subscription_due' },
                { label: '待办完成', value: 'todo_completed' }
            ],
            actionOptions: [
                { label: '订阅扣费', value: 'subscription_charge' },
                { label: '餐饮转购物', value: 'meal_to_shopping' },
                { label: '创建待办', value: 'create_todo' },
                { label: '创建阅读待办', value: 'create_reading_todo' },
                { label: '待办转日记', value: 'todo_to_diary' }
            ]
        }
    },
    computed: {
        triggerColumns() {
            return this.triggerOptions.map(item => ({ text: item.label, value: item.value }))
        },
        actionColumns() {
            return this.actionOptions.map(item => ({ text: item.label, value: item.value }))
        },
        timeMinDate() {
            return new Date()
        },
        timeMaxDate() {
            const now = new Date()
            return new Date(now.getFullYear() + 2, 11, 31)
        },
        timeValues() {
            if (this.form.nextRunTime) {
                const value = String(this.form.nextRunTime).replace(/-/g, '/').replace(' ', 'T')
                const date = new Date(value)
                if (!Number.isNaN(date.getTime())) {
                    return [
                        String(date.getFullYear()),
                        String(date.getMonth() + 1).padStart(2, '0'),
                        String(date.getDate()).padStart(2, '0'),
                        String(date.getHours()).padStart(2, '0'),
                        String(date.getMinutes()).padStart(2, '0')
                    ]
                }
            }
            const now = new Date()
            return [
                String(now.getFullYear()),
                String(now.getMonth() + 1).padStart(2, '0'),
                String(now.getDate()).padStart(2, '0'),
                String(now.getHours()).padStart(2, '0'),
                String(now.getMinutes()).padStart(2, '0')
            ]
        }
    },
    mounted() {
        this.load()
    },
    methods: {
        async load() {
            mLoading('加载中...')
            try {
                const [rulesResponse, templatesResponse] = await Promise.all([listRules(), listTemplates()])
                this.rules = rulesResponse.data || []
                this.templates = templatesResponse.data || []
                closeToast()
            } catch (error) {
                mError('加载失败')
            }
        },
        triggerLabel(value) {
            const item = this.triggerOptions.find(i => i.value === value)
            return item ? item.label : (value || '')
        },
        actionLabel(value) {
            const item = this.actionOptions.find(i => i.value === value)
            return item ? item.label : (value || '')
        },
        statusLabel(status) {
            return { success: '成功', failed: '失败', preview: '试运行', running: '执行中' }[status] || status
        },
        createBlank() {
            this.form = emptyForm()
            this.editorShow = true
        },
        useTemplate(item) {
            this.form = {
                ...emptyForm(),
                name: item.name,
                templateCode: item.code,
                triggerType: item.triggerType,
                actionType: item.actionType
            }
            this.editorShow = true
        },
        edit(rule) {
            this.form = {
                ...rule,
                conditionJson: rule.conditionJson || '{}',
                actionConfig: rule.actionConfig || '{}',
                triggerConfig: rule.triggerConfig || '{}',
                nextRunTime: rule.nextRunTime || ''
            }
            this.editorShow = true
        },
        onTriggerConfirm({ selectedOptions }) {
            const option = selectedOptions && selectedOptions[0]
            if (option) this.form.triggerType = option.value
            this.triggerPickerShow = false
        },
        onActionConfirm({ selectedOptions }) {
            const option = selectedOptions && selectedOptions[0]
            if (option) this.form.actionType = option.value
            this.actionPickerShow = false
        },
        onTimeConfirm({ selectedValues }) {
            const [year, month, day, hour, minute] = selectedValues
            this.form.nextRunTime = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00`
            this.timePickerShow = false
        },
        validateJson(key) {
            try {
                JSON.parse(this.form[key] || '{}')
                mSuccess('JSON 格式正确')
            } catch (e) {
                mError('JSON 格式错误')
            }
        },
        formatJson(key) {
            try {
                this.form[key] = JSON.stringify(JSON.parse(this.form[key] || '{}'), null, 2)
                showToast('已格式化')
            } catch (e) {
                mError('JSON 格式错误')
            }
        },
        parseJson(key) {
            return JSON.parse(this.form[key] || '{}')
        },
        async submit() {
            if (!this.form.name.trim()) {
                showToast('请输入规则名称')
                return
            }
            let parsed = {}
            try {
                parsed = {
                    triggerConfig: this.parseJson('triggerConfig'),
                    conditionJson: this.parseJson('conditionJson'),
                    actionConfig: this.parseJson('actionConfig')
                }
            } catch (e) {
                showToast('JSON 配置格式错误')
                return
            }
            if (this.form.triggerType === 'schedule' && !this.form.nextRunTime) {
                showToast('请选择首次运行时间')
                return
            }

            this.saving = true
            mLoading('保存中...')
            try {
                await saveRule({
                    ...this.form,
                    triggerConfig: JSON.stringify(parsed.triggerConfig),
                    conditionJson: JSON.stringify(parsed.conditionJson),
                    actionConfig: JSON.stringify(parsed.actionConfig)
                })
                this.editorShow = false
                mSuccess('已保存')
                this.load()
            } catch (error) {
                mError('保存失败')
            } finally {
                this.saving = false
            }
        },
        async toggle(rule, enabled) {
            try {
                await saveRule({ ...rule, enabled })
                this.load()
            } catch (error) {
                mError('操作失败')
            }
        },
        async run(rule, dry) {
            mLoading(dry ? '试运行中...' : '执行中...')
            try {
                const response = dry ? await dryRunRule(rule.id) : await runRule(rule.id)
                const status = response.data && response.data.status
                const ok = ['success', 'preview'].includes(status)
                if (ok) {
                    mSuccess(dry ? '试运行完成' : '执行成功')
                } else {
                    mError('执行失败')
                }
            } catch (error) {
                mError('执行失败')
            }
        },
        remove(rule) {
            mConfirm('删除规则', `确定删除“${rule.name}”吗？`).then(async () => {
                try {
                    await deleteRule(rule.id)
                    mSuccess('已删除')
                    this.load()
                } catch (error) {
                    mError('删除失败')
                }
            }).catch(() => {})
        },
        async openHistory(rule) {
            try {
                const response = await listExecutions(rule.id)
                this.executions = response.data || []
                this.historyShow = true
            } catch (error) {
                mError('加载失败')
            }
        },
        async retry(item) {
            try {
                await retryExecution(item.id)
                mSuccess('已重放')
                const response = await listExecutions(item.ruleId)
                this.executions = response.data || []
            } catch (error) {
                mError('重放失败')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.automation-page {
    padding-bottom: 88px;
}

.templates-scroll {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    margin: 0 -12px 4px;
    padding: 0 12px;
}

.templates-inner {
    display: inline-flex;
    gap: 8px;
    padding-bottom: 4px;
}

.templates-empty {
    padding: 16px 0;
    min-width: 200px;
}

.template {
    width: 150px;
    padding: 12px;
    border-radius: 12px;
    background: linear-gradient(135deg, var(--pnkx-primary-soft), var(--pnkx-surface));
    border: 1px solid var(--pnkx-border-soft);
    flex-shrink: 0;

    &__name {
        display: block;
        color: var(--pnkx-text);
        font-weight: 600;
        font-size: 13px;
    }

    &__desc {
        display: block;
        margin-top: 4px;
        color: var(--pnkx-text-muted);
        font-size: 11px;
        line-height: 1.4;
    }

    &:active {
        opacity: 0.85;
    }
}

.rule-card {
    &__top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 10px;
    }

    &__info {
        flex: 1;
        min-width: 0;
    }

    &__name {
        display: block;
        color: var(--pnkx-text);
        font-weight: 600;
        font-size: 14px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__meta {
        display: block;
        margin-top: 4px;
        color: var(--pnkx-text-muted);
        font-size: 11px;
    }

    &__actions {
        display: flex;
        gap: 12px;
        margin-top: 11px;

        button {
            background: transparent;
            border: none;
            padding: 0;
            color: var(--pnkx-primary);
            font-size: 12px;

            &:active {
                opacity: 0.7;
            }
        }
    }

    &__danger {
        color: var(--pnkx-danger) !important;
    }
}

.editor,
.history {
    max-height: 75vh;
    overflow-y: auto;
    padding: 16px 16px calc(16px + env(safe-area-inset-bottom));
    background: var(--pnkx-surface);
    border-radius: 14px 14px 0 0;
}

.editor {
    display: flex;
    flex-direction: column;
    gap: 10px;

    &__title {
        color: var(--pnkx-text);
        font-size: 16px;
        font-weight: 700;
    }
}

.json-block {
    &__head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 6px;
    }

    &__label {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        font-weight: 600;
    }

    &__ops {
        display: flex;
        gap: 10px;

        button {
            background: transparent;
            border: none;
            padding: 0;
            font-size: 12px;
            color: var(--pnkx-primary);

            &:active {
                opacity: 0.7;
            }
        }
    }

    &__field {
        background: var(--pnkx-surface-muted);
        border-radius: 8px;

        :deep(textarea) {
            font-family: monospace;
        }
    }
}

.execution {
    position: relative;
    padding: 10px 0;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-child {
        border-bottom: none;
    }

    &__top {
        display: flex;
        align-items: center;
    }

    &__status {
        color: var(--pnkx-text-secondary);
        font-size: 13px;

        &--success {
            color: var(--pnkx-success);
        }

        &--failed {
            color: var(--pnkx-danger);
        }

        &--preview {
            color: var(--pnkx-info);
        }
    }

    &__time {
        margin-left: 8px;
        color: var(--pnkx-text-muted);
        font-size: 11px;
    }

    &__error {
        margin-top: 4px;
        color: var(--pnkx-danger);
        font-size: 11px;
        word-break: break-word;
    }

    &__retry {
        margin-top: 4px;
        color: var(--pnkx-primary);
        font-size: 12px;
    }
}
</style>
