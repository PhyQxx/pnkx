<!--
 * @File: kanban
 * @Author: PHY
 * @Date: 2026/07/02
 * @Description: 待办看板 - 三栏拖拽视图
-->
<template>
    <div class="kanban-container">
        <!-- 顶部工具栏 -->
        <div class="kanban-toolbar">
            <div class="toolbar-left">
                <h2 class="kanban-title">待办看板</h2>
                <el-tag size="small" effect="plain" class="count-tag">
                    共 {{ totalCount }} 项
                </el-tag>
            </div>
            <div class="toolbar-right">
                <el-input
                    v-model="searchText"
                    placeholder="搜索内容/标签..."
                    clearable
                    size="small"
                    style="width: 200px"
                >
                    <template #prefix>
                        <el-icon>
                            <Search/>
                        </el-icon>
                    </template>
                </el-input>
                <el-button size="small" @click="loadKanban" icon="Refresh">刷新</el-button>
            </div>
        </div>

        <!-- 三栏看板 -->
        <div v-loading="loading" class="kanban-board">
            <div v-for="col in columns" :key="col.key" class="kanban-column">
                <div class="column-header">
                    <span class="column-dot" :style="{background: col.color}"></span>
                    <span class="column-title">{{ col.title }}</span>
                    <el-tag size="small" round>{{ getColumnCount(col.key) }}</el-tag>
                    <!-- 已完成列折叠按钮 -->
                    <el-icon v-if="col.key === 'done' && getColumnData(col.key).length > 0"
                             class="collapse-icon" @click="collapsed.done = !collapsed.done">
                        <ArrowDown v-if="collapsed.done"/>
                        <ArrowUp v-else/>
                    </el-icon>
                </div>
                <draggable
                    v-show="col.key !== 'done' || !collapsed.done"
                    :list="getColumnData(col.key)"
                    :group="group"
                    item-key="id"
                    class="column-body"
                    ghost-class="card-ghost"
                    drag-class="card-drag"
                    animation="200"
                    @end="onDragEnd"
                    :data-col="col.key"
                >
                    <template #item="{ element }">
                        <div class="task-card" v-show="matchSearch(element)" @click="openDetail(element)">
                            <!-- 优先级色条 -->
                            <div class="priority-bar" :style="{background: priorityColor(element.priority)}"></div>
                            <div class="card-content">
                                <div class="card-header">
                                    <span class="card-title">{{ element.content }}</span>
                                    <el-tag v-if="element.priority > 0" size="small"
                                            :type="priorityTagType(element.priority)" effect="dark">
                                        {{ priorityLabel(element.priority) }}
                                    </el-tag>
                                </div>
                                <!-- 标签 -->
                                <div v-if="element.label" class="card-labels">
                                    <el-tag v-for="(lab, i) in parseLabels(element.label)" :key="i"
                                            size="small" type="info" effect="plain">
                                        {{ lab }}
                                    </el-tag>
                                </div>
                                <!-- 截止时间 -->
                                <div v-if="element.planEndTime" class="card-meta">
                                    <el-icon>
                                        <Clock/>
                                    </el-icon>
                                    <span :class="{'overdue': isOverdue(element.planEndTime)}">
                                        {{ formatTime(element.planEndTime) }}
                                    </span>
                                </div>
                                <!-- 执行者 -->
                                <div v-if="element.performer" class="card-meta">
                                    <el-icon>
                                        <User/>
                                    </el-icon>
                                    <span>{{ formatPerformer(element.performer) }}</span>
                                </div>
                                <!-- 子任务进度 -->
                                <div v-if="element.subtaskCount > 0" class="card-subtasks">
                                    <el-progress
                                        :percentage="element.subtaskProgress"
                                        :stroke-width="6"
                                        :show-text="false"
                                    />
                                    <span class="subtask-text">{{ element.subtaskDone }}/{{ element.subtaskCount }}</span>
                                </div>
                            </div>
                        </div>
                    </template>
                </draggable>

                <!-- 新增按钮 -->
                <div class="column-add" @click="handleAdd(col.key)">
                    <el-icon>
                        <Plus/>
                    </el-icon>
                    <span>新增任务</span>
                </div>
            </div>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" append-to-body>
            <el-form :model="form" label-width="80px" @submit.native.prevent>
                <el-form-item label="内容">
                    <el-input v-model="form.content" type="textarea" :rows="2"
                              placeholder="待办内容"/>
                </el-form-item>
                <el-form-item label="优先级">
                    <el-select v-model="form.priority" style="width: 100%">
                        <el-option :value="0" label="无"/>
                        <el-option :value="1" label="低"/>
                        <el-option :value="2" label="中"/>
                        <el-option :value="3" label="高"/>
                        <el-option :value="4" label="紧急"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="截止时间">
                    <el-date-picker v-model="form.planEndTime" type="datetime"
                                    format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss"
                                    style="width: 100%"/>
                </el-form-item>
                <el-form-item label="标签">
                    <el-input v-model="form.label" placeholder="多个标签用英文逗号分隔"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import draggable from 'vuedraggable'
import {Search, Refresh, Plus, Clock, User, ArrowUp, ArrowDown} from '@element-plus/icons-vue'
import {kanbanDo, sortDo, addDo, updateDo, subtaskDo} from '@/api/px/life/todo'

export default {
    name: 'TodoKanban',
    components: {draggable, Search, Refresh, Plus, Clock, User, ArrowUp, ArrowDown},
    data() {
        return {
            loading: false,
            saving: false,
            searchText: '',
            group: {name: 'kanban'},
            columns: [
                {key: 'todo', title: '待办', color: '#909399', status: 0},
                {key: 'doing', title: '进行中', color: '#e6a23c', status: 1},
                {key: 'done', title: '已完成', color: '#67c23a', status: 2}
            ],
            kanbanData: {todo: [], doing: [], done: []},
            // 已完成真实总数（后端截断后 done 列表可能只有 50 条）
            doneTotal: 0,
            // 已完成列默认折叠（数据量大时避免撑长页面）
            collapsed: {done: false},
            // 弹窗
            dialogVisible: false,
            dialogTitle: '新增任务',
            isEdit: false,
            form: {
                id: undefined,
                content: '',
                priority: 0,
                kanbanStatus: 0,
                planEndTime: '',
                label: ''
            }
        }
    },
    computed: {
        totalCount() {
            return this.kanbanData.todo.length + this.kanbanData.doing.length + this.kanbanData.done.length
        }
    },
    mounted() {
        this.loadKanban()
    },
    methods: {
        /**
         * 返回列原始数据（draggable 需要可变原数组才能拖拽排序）。
         * 搜索过滤通过卡片级 v-show 实现，不破坏拖拽。
         */
        getColumnData(key) {
            return this.kanbanData[key]
        },
        /**
         * 单张卡片是否匹配搜索关键词（用于 v-show）
         */
        matchSearch(element) {
            if (!this.searchText) return true
            const kw = this.searchText.toLowerCase()
            return (element.content && element.content.toLowerCase().includes(kw)) ||
                (element.label && element.label.toLowerCase().includes(kw)) ||
                (element.remark && element.remark.toLowerCase().includes(kw))
        },
        loadKanban() {
            this.loading = true
            kanbanDo().then(res => {
                const data = res.data || {}
                this.kanbanData = {
                    todo: data.todo || [],
                    doing: data.doing || [],
                    done: data.done || []
                }
                this.doneTotal = data.doneTotal != null ? data.doneTotal : this.kanbanData.done.length
                // 异步加载子任务进度
                this.loadSubtasks()
            }).finally(() => {
                this.loading = false
            })
        },
        /**
         * 列计数：已完成列显示真实总数（后端截断后 done 列表只有 50 条）
         */
        getColumnCount(key) {
            if (key === 'done') return this.doneTotal
            return this.kanbanData[key].length
        },
        /**
         * 为每张卡片加载子任务进度
         */
        async loadSubtasks() {
            const all = [...this.kanbanData.todo, ...this.kanbanData.doing, ...this.kanbanData.done]
            for (const task of all) {
                try {
                    const res = await subtaskDo(task.id)
                    const subs = res.data || []
                    if (subs.length > 0) {
                        this.$set(task, 'subtaskCount', subs.length)
                        const done = subs.filter(s => s.status).length
                        this.$set(task, 'subtaskDone', done)
                        this.$set(task, 'subtaskProgress', Math.round(done / subs.length * 100))
                    }
                } catch (e) {
                    /* ignore */
                }
            }
        },
        /**
         * 拖拽结束：更新状态+排序
         */
        onDragEnd() {
            const updates = []
            this.columns.forEach(col => {
                const list = this.kanbanData[col.key]
                list.forEach((task, index) => {
                    updates.push({
                        id: task.id,
                        kanbanStatus: col.status,
                        sortOrder: index
                    })
                })
            })
            // 有变更才提交
            if (updates.length > 0) {
                sortDo(updates).catch(() => {
                    this.$message.error('排序保存失败')
                    this.loadKanban()
                })
            }
        },
        handleAdd(colKey) {
            this.isEdit = false
            this.dialogTitle = '新增任务'
            const col = this.columns.find(c => c.key === colKey)
            this.form = {
                id: undefined,
                content: '',
                priority: 0,
                kanbanStatus: col.status,
                planEndTime: '',
                label: ''
            }
            this.dialogVisible = true
        },
        openDetail(task) {
            this.isEdit = true
            this.dialogTitle = '编辑任务'
            this.form = {
                id: task.id,
                content: task.content,
                priority: task.priority || 0,
                kanbanStatus: task.kanbanStatus,
                planEndTime: task.planEndTime || '',
                label: task.label || ''
            }
            this.dialogVisible = true
        },
        handleSave() {
            if (!this.form.content || !this.form.content.trim()) {
                this.$message.warning('请输入待办内容')
                return
            }
            this.saving = true
            const action = this.isEdit ? updateDo(this.form) : addDo(this.form)
            action.then(() => {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.loadKanban()
            }).finally(() => {
                this.saving = false
            })
        },
        // ===== 工具方法 =====
        priorityColor(p) {
            const map = {0: 'transparent', 1: '#909399', 2: '#409eff', 3: '#e6a23c', 4: '#f56c6c'}
            return map[p] || 'transparent'
        },
        priorityTagType(p) {
            const map = {0: 'info', 1: 'info', 2: '', 3: 'warning', 4: 'danger'}
            return map[p] || 'info'
        },
        priorityLabel(p) {
            const map = {0: '无', 1: '低', 2: '中', 3: '高', 4: '紧急'}
            return map[p] || '无'
        },
        parseLabels(labelStr) {
            if (!labelStr) return []
            return labelStr.split(',').filter(s => s.trim())
        },
        formatTime(t) {
            if (!t) return ''
            return String(t).replace('T', ' ').substring(5, 16)
        },
        isOverdue(t) {
            if (!t) return false
            return new Date(t) < new Date()
        },
        formatPerformer(p) {
            // performer 存的是 userId 列表，简化展示
            if (!p) return ''
            const ids = String(p).split(',')
            return ids.length > 1 ? `${ids.length}人` : `用户${ids[0]}`
        }
    }
}
</script>

<style lang="scss" scoped>
.kanban-container {
    padding: 12px;
    /* 用视口高度约束整个看板，避免被大量卡片撑长 */
    height: 100%;
    display: flex;
    flex-direction: column;
}

.kanban-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    flex-shrink: 0;

    .toolbar-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .kanban-title {
            margin: 0;
            font-size: 18px;
            font-weight: 600;
            color: var(--pnkx-text);
        }
    }

    .toolbar-right {
        display: flex;
        gap: 8px;
    }
}

.kanban-board {
    flex: 1;
    display: flex;
    gap: 12px;
    overflow: hidden;
    min-height: 0; /* 关键：让 flex 子项能正确收缩 */
}

.kanban-column {
    flex: 1;
    min-width: 260px;
    /* 列高度限死在容器内，内部滚动 */
    height: 100%;
    background: var(--pnkx-surface-muted);
    border-radius: var(--pnkx-radius-md);
    display: flex;
    flex-direction: column;
    border: 1px solid var(--pnkx-border);
    overflow: hidden;

    .column-header {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 12px 14px;
        font-weight: 600;
        color: var(--pnkx-text);
        flex-shrink: 0;
        border-bottom: 1px solid var(--pnkx-border);

        .column-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
        }

        .column-title {
            flex: 1;
        }

        .collapse-icon {
            cursor: pointer;
            color: var(--pnkx-text-secondary);
            transition: color 0.16s;

            &:hover {
                color: var(--pnkx-primary);
            }
        }
    }

    .column-body {
        flex: 1;
        padding: 8px 10px;
        overflow-y: auto;
        min-height: 60px; /* 折叠时留一点高度 */

        /* 美化滚动条 */
        &::-webkit-scrollbar {
            width: 6px;
        }
        &::-webkit-scrollbar-thumb {
            background: var(--pnkx-text-placeholder);
            border-radius: 3px;
        }
        &::-webkit-scrollbar-track {
            background: transparent;
        }
    }

    .column-add {
        padding: 10px 14px;
        color: var(--pnkx-text-secondary);
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 6px;
        border-top: 1px dashed var(--pnkx-border);
        flex-shrink: 0;
        transition: background 0.16s;

        &:hover {
            background: var(--pnkx-surface);
            color: var(--pnkx-primary);
        }
    }
}

.task-card {
    background: var(--pnkx-surface);
    border-radius: var(--pnkx-radius-md);
    margin-bottom: 8px;
    box-shadow: var(--pnkx-shadow-1);
    cursor: pointer;
    overflow: hidden;
    display: flex;
    transition: box-shadow 0.16s, transform 0.16s;

    &:hover {
        box-shadow: var(--pnkx-shadow-2);
    }

    .priority-bar {
        width: 4px;
        flex-shrink: 0;
    }

    .card-content {
        flex: 1;
        padding: 10px 12px;
        min-width: 0;
    }

    .card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 8px;
        margin-bottom: 6px;

        .card-title {
            font-size: 14px;
            color: var(--pnkx-text);
            line-height: 1.5;
            word-break: break-word;
        }
    }

    .card-labels {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
        margin-bottom: 6px;
    }

    .card-meta {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        margin-top: 4px;

        .overdue {
            color: var(--pnkx-danger, #f56c6c);
        }
    }

    .card-subtasks {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 6px;

        .el-progress {
            flex: 1;
        }

        .subtask-text {
            font-size: 11px;
            color: var(--pnkx-text-secondary);
        }
    }
}

.card-ghost {
    opacity: 0.4;
    background: var(--pnkx-primary-soft);
}

.card-drag {
    transform: rotate(2deg);
}
</style>
