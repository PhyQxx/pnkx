<template>
    <div v-if="schema.length" class="draft-editor">
        <van-button
            v-if="!editing"
            size="small"
            plain
            round
            icon="edit"
            :disabled="disabled"
            @click="begin"
        >
            修改草稿
        </van-button>
        <template v-else>
            <div class="draft-editor__notice">保存后生成新预览，旧草稿失效。请再次核对后确认。</div>
            <div v-for="f in schema" :key="f.key" class="draft-editor__field">
                <div class="draft-editor__label">{{ f.label }}</div>
                <van-switch
                    v-if="f.boolean"
                    :model-value="!!values[f.key]"
                    size="20px"
                    :disabled="saving"
                    @update:model-value="values[f.key] = $event"
                />
                <van-field
                    v-else-if="f.date || f.datetime"
                    :model-value="values[f.key]"
                    class="draft-editor__input"
                    readonly
                    is-link
                    :disabled="saving"
                    placeholder="请选择时间"
                    @click="openPicker(f)"
                />
                <van-field
                    v-else-if="f.multiline"
                    v-model="values[f.key]"
                    class="draft-editor__input"
                    type="textarea"
                    rows="2"
                    autosize
                    :disabled="saving"
                    placeholder="请输入内容"
                />
                <van-field
                    v-else
                    v-model="values[f.key]"
                    class="draft-editor__input"
                    :disabled="saving"
                    placeholder="请输入内容"
                />
            </div>
            <div v-if="error" class="draft-editor__error">{{ error }}</div>
            <div class="draft-editor__actions">
                <van-button size="small" type="primary" :loading="saving" :disabled="saving" @click="save">
                    保存并重新预览
                </van-button>
                <van-button size="small" plain :disabled="saving" @click="finish">取消修改</van-button>
            </div>
        </template>

        <!-- 日期/时间选择（对齐 uniapp uni-datetime-picker 行为） -->
        <van-popup v-model:show="pickerShow" position="bottom" round>
            <div class="draft-editor__picker">
                <van-date-picker
                    v-model="dateValues"
                    title="选择日期"
                    :min-date="minPickDate"
                    :max-date="maxPickDate"
                    :columns-type="['year', 'month', 'day']"
                />
                <van-time-picker
                    v-if="pickerIsDatetime"
                    v-model="timeValues"
                    title="选择时间"
                    :columns-type="['hour', 'minute']"
                />
                <div class="draft-editor__picker-actions">
                    <van-button block plain @click="pickerShow = false">取消</van-button>
                    <van-button block type="primary" @click="confirmPicker">确定</van-button>
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import request from '@/utils/request'
import { editableFields, draftValues, revisionBody } from '@/utils/planEditing'

export default {
    name: 'MPlanDraftEditor',
    props: { plan: Object, disabled: Boolean },
    emits: ['updated', 'editing'],
    data() {
        return {
            editing: false,
            saving: false,
            values: {},
            error: '',
            pickerShow: false,
            pickerField: null,
            pickerIsDatetime: false,
            dateValues: [],
            timeValues: []
        }
    },
    computed: {
        schema() {
            return editableFields(this.plan)
        },
        minPickDate() {
            return new Date(new Date().getFullYear() - 10, 0, 1)
        },
        maxPickDate() {
            return new Date(new Date().getFullYear() + 10, 11, 31)
        }
    },
    watch: {
        'plan.id'() {
            this.finish()
        }
    },
    methods: {
        begin() {
            this.values = draftValues(this.plan)
            this.editing = true
            this.error = ''
            this.$emit('editing', true)
        },
        finish() {
            this.editing = false
            this.$emit('editing', false)
        },
        openPicker(field) {
            if (this.saving) return
            this.pickerField = field
            this.pickerIsDatetime = !!field.datetime
            const parsed = this.parseDateTime(this.values[field.key])
            const now = new Date()
            this.dateValues = parsed
                ? parsed.date
                : [
                      String(now.getFullYear()),
                      String(now.getMonth() + 1).padStart(2, '0'),
                      String(now.getDate()).padStart(2, '0')
                  ]
            this.timeValues = parsed
                ? parsed.time
                : [String(now.getHours()).padStart(2, '0'), String(now.getMinutes()).padStart(2, '0')]
            this.pickerShow = true
        },
        parseDateTime(value) {
            const m = /^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2}))?/.exec(String(value || '').trim())
            if (!m) return null
            return {
                date: [m[1], m[2], m[3]],
                time: [m[4] || '00', m[5] || '00']
            }
        },
        confirmPicker() {
            if (!this.pickerField) return
            const date = this.dateValues.join('-')
            this.values[this.pickerField.key] = this.pickerIsDatetime
                ? `${date} ${this.timeValues.join(':')}:00`
                : date
            this.pickerShow = false
        },
        async save() {
            if (this.saving) return
            this.saving = true
            this.error = ''
            try {
                const r = await request({
                    url: `/operation-plans/${this.plan.id}/revise`,
                    method: 'post',
                    data: revisionBody(this.plan, this.values)
                })
                this.finish()
                this.$emit('updated', r.data)
            } catch (e) {
                this.error = (e && e.message) || '保存失败，草稿已保留，请重试'
            } finally {
                this.saving = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.draft-editor {
    padding: 12px;
    background: var(--pnkx-surface-muted);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 10px;
    margin: 12px 0;
    font-size: 13px;
}

.draft-editor__notice {
    color: var(--pnkx-warning);
    line-height: 1.6;
    margin-bottom: 4px;
}

.draft-editor__field {
    margin: 12px 0;
}

.draft-editor__label {
    margin-bottom: 6px;
    color: var(--pnkx-text-secondary);
}

.draft-editor__input {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 8px;
    padding: 4px 6px;
}

.draft-editor__error {
    color: var(--pnkx-danger);
    background: var(--pnkx-danger-soft);
    border-radius: 8px;
    padding: 8px 10px;
    margin: 8px 0;
    line-height: 1.6;
}

.draft-editor__actions {
    display: flex;
    gap: 8px;
    margin-top: 12px;
}

.draft-editor__picker {
    padding-bottom: 12px;
}

.draft-editor__picker-actions {
    display: flex;
    gap: 12px;
    padding: 12px 16px 0;
}
</style>
