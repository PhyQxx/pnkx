<template>
    <section class="chat-plan-card" aria-label="操作确认卡片" :aria-busy="busy">
        <p v-if="busy">正在同步计划…</p>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <template v-if="plan">
            <header>
                <strong>{{ plan.title }}</strong
                ><el-tag>{{ label(plan.status) }}</el-tag>
            </header>
            <p>{{ hint(plan.status) }}</p>
            <div v-for="(step, index) in plan.steps" :key="index" class="card-step">
                <strong>{{ tool(step.tool_code) }}</strong>
                <dl>
                    <template v-for="(field, i) in fields(step.input_json)" :key="i"
                        ><dt>{{ field.label }}</dt>
                        <dd>{{ field.value }}</dd></template
                    >
                </dl>
                <p v-if="step.error_msg" role="alert">{{ step.error_msg }}</p>
            </div>
            <PlanDraftEditor :plan="plan" :disabled="locked" @editing="editing = $event" @updated="revised" />
            <div v-if="!editing" class="card-actions">
                <el-button v-if="plan.replacement_id" :disabled="locked" @click="load(plan.replacement_id)"
                    >查看修改后的预览</el-button
                >
                <el-button
                    v-if="plan.status === 'awaiting_confirmation'"
                    type="primary"
                    :disabled="locked"
                    @click="run('confirm')"
                    >确认执行以上内容</el-button
                >
                <el-button
                    v-if="['failed', 'partially_succeeded'].includes(plan.status)"
                    type="primary"
                    :disabled="locked"
                    @click="run('retry')"
                    >重试未完成步骤</el-button
                >
                <el-button v-if="plan.status === 'awaiting_confirmation'" :disabled="locked" @click="run('cancel')"
                    >取消计划</el-button
                >
            </div>
        </template>
        <el-button v-if="!editing" text :disabled="locked" @click="load(selectedId || planId)">刷新状态</el-button>
    </section>
</template>
<script>
import request from '@/utils/request';
import PlanDraftEditor from './PlanDraftEditor.vue';
import { planLabels, toolLabels, planFields, planHint } from '../utils/operationPlanPresentation';
export default {
    components: { PlanDraftEditor },
    props: { planId: String, disabled: Boolean },
    data() {
        return { plan: null, busy: false, editing: false, error: '', requestVersion: 0, selectedId: null };
    },
    computed: {
        locked() {
            return this.busy || this.disabled || this.editing;
        }
    },
    watch: {
        planId: {
            immediate: true,
            handler(id) {
                this.load(id);
            }
        }
    },
    beforeUnmount() {
        this.requestVersion++;
    },
    methods: {
        label: (status) => planLabels[status] || status,
        tool: (code) => toolLabels[code] || code,
        fields: planFields,
        hint: planHint,
        async load(id) {
            if (!id || this.editing || this.busy) return;
            const version = ++this.requestVersion;
            this.selectedId = id;
            this.busy = true;
            this.error = '';
            this.plan = null;
            try {
                const r = await request({ url: `/operation-plans/${id}` });
                if (version === this.requestVersion) this.plan = r.data;
            } catch (e) {
                if (version === this.requestVersion) this.error = '预览加载失败，请重试。';
            } finally {
                if (version === this.requestVersion) this.busy = false;
            }
        },
        revised(plan) {
            this.plan = plan;
            this.selectedId = plan.id;
            this.editing = false;
        },
        async run(action) {
            if (this.locked || !this.plan) return;
            this.busy = true;
            this.error = '';
            const version = ++this.requestVersion;
            try {
                const r = await request({ url: `/operation-plans/${this.plan.id}/${action}`, method: 'post' });
                if (version === this.requestVersion) this.plan = r.data;
            } catch (e) {
                if (version === this.requestVersion) this.error = e?.message || '操作未完成，请刷新状态后重试。';
            } finally {
                if (version === this.requestVersion) this.busy = false;
            }
        }
    }
};
</script>
<style scoped>
.chat-plan-card {
    margin-top: 12px;
    padding: 16px;
    border: 1px solid #cbd5e1;
    border-radius: 12px;
    background: #f8fafc;
    color: #334155;
}
header {
    display: flex;
    justify-content: space-between;
    gap: 12px;
}
p {
    font-size: 13px;
    line-height: 1.6;
}
.card-step {
    margin-top: 12px;
}
dl {
    display: grid;
    grid-template-columns: 90px minmax(0, 1fr);
    gap: 8px;
    font-size: 14px;
}
dd {
    margin: 0;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}
dt {
    color: #64748b;
}
.card-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}
</style>
