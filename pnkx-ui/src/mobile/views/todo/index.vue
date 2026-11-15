<template>
    <div class="todo-page">
        <!-- 固定头部：完成率 + 状态 Tab + 搜索 + 视图切换 + 标签 -->
        <div class="todo-header">
            <div class="todo-header__panel">
                <div class="todo-header__main">
                    <div class="todo-header__title">待办事项</div>
                    <div class="todo-header__subtitle">{{ headerDateText }}</div>
                </div>
                <div class="todo-header__progress">
                    <span class="todo-header__progress-num">{{ completionRate }}%</span>
                    <span class="todo-header__progress-label">完成率</span>
                </div>
            </div>

            <div class="todo-header__tabs">
                <div
                    v-for="tab in statusTabs"
                    :key="tab.key"
                    class="todo-tab"
                    :class="{ active: activeTab === tab.key }"
                    @click="handleTabClick(tab.key)"
                >
                    <span class="todo-tab__value">{{ tab.count }}</span>
                    <span class="todo-tab__label">{{ tab.label }}</span>
                </div>
            </div>

            <van-search
                v-model="searchKeyword"
                class="todo-header__search"
                placeholder="搜索待办..."
                shape="round"
                :clearable="true"
                @search="handleSearch"
                @clear="handleSearch"
                @update:model-value="onSearchInput"
            />

            <div class="todo-header__switch">
                <div class="switch-item" :class="{ active: viewMode === 'timeline' }" @click="switchView('timeline')">
                    <van-icon name="bars" size="15" />
                    <span>列表</span>
                </div>
                <div class="switch-item" :class="{ active: viewMode === 'calendar' }" @click="switchView('calendar')">
                    <van-icon name="calendar-o" size="15" />
                    <span>日历</span>
                </div>
            </div>

            <div v-if="labelList.length > 0" class="todo-header__labels">
                <div class="label-scroll">
                    <span class="label-chip" :class="{ active: selectedLabel === '' }" @click="handleLabelClick('')">全部</span>
                    <span
                        v-for="label in labelList"
                        :key="label"
                        class="label-chip"
                        :class="{ active: selectedLabel === label }"
                        @click="handleLabelClick(label)"
                    >{{ label }}</span>
                </div>
            </div>
        </div>

        <!-- 内容区域 -->
        <van-pull-refresh v-model="refreshing" class="todo-body m-pull" @refresh="onRefresh">
            <!-- 日历视图 -->
            <div v-if="viewMode === 'calendar'" class="calendar-view">
                <div class="calendar-card m-card">
                    <van-calendar
                        :key="'todo-cal-' + calendarKey"
                        :poppable="false"
                        :show-confirm="false"
                        :show-mark="false"
                        :show-title="false"
                        :lazy-render="false"
                        :row-height="52"
                        :min-date="minDate"
                        :max-date="maxDate"
                        :default-date="defaultCalendarDate"
                        :formatter="calendarFormatter"
                        style="height: 384px"
                        @select="onDateClick"
                    />
                </div>

                <template v-if="selectedDateTodos.length > 0">
                    <div class="section-title">
                        <span>{{ formatDateFull(selectedDate) }}</span>
                        <span class="section-count">{{ selectedDateTodos.length }} 项</span>
                    </div>
                    <TodoCard
                        v-for="todo in selectedDateTodos"
                        :key="todo.id"
                        :todo="todo"
                        @click="handleTodoClick"
                        @edit="handleTodoEdit"
                        @delete="handleTodoDelete"
                        @toggle-status="handleTodoToggle"
                    />
                </template>
                <div v-else class="m-empty">
                    <van-icon name="checked" size="42" color="var(--pnkx-primary)" />
                    <span>该日期暂无待办</span>
                    <span class="m-text-muted">换一天看看，或创建新的安排</span>
                </div>
            </div>

            <!-- 列表视图 -->
            <van-list
                v-else
                v-model:loading="listLoading"
                :finished="listFinished"
                finished-text="没有更多了"
                @load="loadMore"
            >
                <div v-if="todoList.length === 0 && !listLoading && !loading" class="m-empty">
                    <van-icon name="checked" size="42" color="var(--pnkx-primary)" />
                    <span>{{ activeTab === 'today' ? '今日暂无待办' : '暂无待办' }}</span>
                    <span class="m-text-muted">点击右下角按钮开始添加待办吧</span>
                </div>
                <TodoCard
                    v-for="todo in todoList"
                    :key="todo.id"
                    :todo="todo"
                    @click="handleTodoClick"
                    @edit="handleTodoEdit"
                    @delete="handleTodoDelete"
                    @toggle-status="handleTodoToggle"
                />
            </van-list>
        </van-pull-refresh>

        <button class="m-fab" @click="handleAdd">
            <van-icon name="plus" size="22" />
        </button>
    </div>
</template>

<script>
import { listDo, updateDo, delDo, getLabelList } from '@/api/px/life/todo'
import { showToast, mSuccess, mError, mConfirm } from '@/mobile/utils/ui'
import TodoCard from './components/TodoCard.vue'

const WEEK_LIST = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export default {
    name: 'MTodo',
    components: {
        TodoCard
    },
    data() {
        return {
            activeTab: 'all',
            viewMode: 'timeline',
            searchKeyword: '',
            searchTimer: null,
            selectedLabel: '',
            labelList: [],
            todoList: [],
            selectedDate: '',
            selectedDateTodos: [],
            allTodosMap: {},
            calendarKey: 0,
            loading: false,
            listLoading: false,
            listFinished: false,
            refreshing: false,
            pageSize: 10,
            activeTotal: null,
            completedTotal: null,
            todayTotal: null,
            sources: {
                active: { params: { status: false }, pageNum: 1, finished: false, total: 0, buffer: [] },
                done: { params: { status: true }, pageNum: 1, finished: false, total: 0, buffer: [] },
                today: { params: {}, pageNum: 1, finished: false, total: 0, buffer: [] }
            }
        }
    },
    computed: {
        headerDateText() {
            const now = new Date()
            return `${now.getMonth() + 1}月${now.getDate()}日 ${WEEK_LIST[now.getDay()]}`
        },
        todayRange() {
            const today = this.formatDate(new Date())
            return {
                planStartTime: `${today} 00:00:00`,
                planEndTime: `${today} 23:59:59`
            }
        },
        tabSources() {
            if (this.activeTab === 'all') return ['active', 'done']
            if (this.activeTab === 'active') return ['active']
            if (this.activeTab === 'done') return ['done']
            return ['today']
        },
        totalTodoCount() {
            if (typeof this.activeTotal === 'number' && typeof this.completedTotal === 'number') {
                return this.activeTotal + this.completedTotal
            }
            return this.todoList.length
        },
        completedTodoCount() {
            if (typeof this.completedTotal === 'number') return this.completedTotal
            return this.todoList.filter(todo => this.isTodoCompleted(todo)).length
        },
        activeTodoCount() {
            if (typeof this.activeTotal === 'number') return this.activeTotal
            return Math.max(this.totalTodoCount - this.completedTodoCount, 0)
        },
        todayTodoCount() {
            if (typeof this.todayTotal === 'number') return this.todayTotal
            return this.todoList.filter(todo => {
                const date = todo.planStartTime ? todo.planStartTime.substring(0, 10) : ''
                return date === this.formatDate(new Date())
            }).length
        },
        completionRate() {
            if (this.totalTodoCount === 0) return 0
            return Math.round((this.completedTodoCount / this.totalTodoCount) * 100)
        },
        statusTabs() {
            return [
                { key: 'all', label: '全部', count: this.totalTodoCount },
                { key: 'active', label: '进行中', count: this.activeTodoCount },
                { key: 'today', label: '今日', count: this.todayTodoCount },
                { key: 'done', label: '完成', count: this.completedTodoCount }
            ]
        },
        minDate() {
            const year = new Date().getFullYear()
            return new Date(year, 0, 1)
        },
        maxDate() {
            const year = new Date().getFullYear()
            return new Date(year + 1, 11, 31)
        },
        defaultCalendarDate() {
            if (!this.selectedDate) return new Date()
            const date = new Date(this.selectedDate.replace(/-/g, '/'))
            return Number.isNaN(date.getTime()) ? new Date() : date
        }
    },
    mounted() {
        this.selectedDate = this.formatDate(new Date())
        this.sources.today.params = this.todayRange
        this.loadLabelList()
        this.refreshTodoList()
    },
    activated() {
        this.refreshTodoList()
    },
    methods: {
        formatDate(date) {
            if (!date) return ''
            const d = new Date(date)
            if (Number.isNaN(d.getTime())) return ''
            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
        },
        formatDateFull(date) {
            if (!date) return ''
            const d = new Date(String(date).replace(/-/g, '/'))
            if (Number.isNaN(d.getTime())) return ''
            return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
        },
        isTodoCompleted(todo) {
            const status = todo ? todo.status : false
            return status === true || status === 1 || status === '1' || status === 'true' || status === '已完成'
        },

        // ---- 视图与筛选 ----
        switchView(mode) {
            this.viewMode = mode
            if (mode === 'calendar') {
                this.loadSelectedDateTodos()
            } else if (this.todoList.length === 0) {
                this.refreshTodoList()
            }
        },
        handleTabClick(key) {
            if (this.activeTab === key) return
            this.activeTab = key
            this.refreshTodoList()
        },
        handleLabelClick(label) {
            if (this.selectedLabel === label) return
            this.selectedLabel = label
            if (this.viewMode === 'calendar') {
                this.loadSelectedDateTodos()
            } else {
                this.refreshTodoList()
            }
        },
        handleSearch() {
            this.refreshTodoList()
        },
        onSearchInput() {
            if (this.searchTimer) clearTimeout(this.searchTimer)
            this.searchTimer = setTimeout(() => {
                this.handleSearch()
            }, 400)
        },
        onRefresh() {
            this.refreshTodoList()
        },

        // ---- 数据加载 ----
        async loadLabelList() {
            try {
                const response = await getLabelList()
                if (response.code === 200) {
                    this.labelList = response.data || []
                }
            } catch (error) {
                console.error('加载标签列表失败:', error)
            }
        },
        buildTodoQuery(extra = {}) {
            const params = { ...extra }
            if (this.searchKeyword) params.content = this.searchKeyword
            if (this.selectedLabel) params.label = this.selectedLabel
            return params
        },
        sourceParams(key) {
            if (key === 'today') {
                return { ...this.todayRange }
            }
            return { status: key === 'done' }
        },
        resetSources() {
            Object.keys(this.sources).forEach(key => {
                const source = this.sources[key]
                source.pageNum = 1
                source.finished = false
                source.total = 0
                source.buffer = []
            })
        },
        allSourcesFinished() {
            return this.tabSources.every(key => this.sources[key].finished)
        },
        async refreshTodoList() {
            if (this.loading) return
            this.loading = true
            this.todoList = []
            this.allTodosMap = {}
            this.resetSources()

            try {
                await this.loadTodoStats()
                const newTodos = await this.loadNextTodoBatch()
                this.todoList = this.sortTodoList(newTodos)
                this.buildCalendarDates()
                this.listFinished = this.allSourcesFinished()
                if (this.selectedDate && this.viewMode === 'calendar') {
                    this.loadSelectedDateTodos()
                }
            } catch (error) {
                console.error('加载待办列表失败:', error)
                this.listFinished = false
                showToast('加载失败')
            } finally {
                this.loading = false
                this.refreshing = false
                this.listLoading = false
            }
        },
        async loadMore() {
            if (this.loading || this.listFinished) {
                this.listLoading = false
                return
            }
            this.loading = true
            try {
                const newTodos = await this.loadNextTodoBatch()
                this.todoList = this.sortTodoList([...this.todoList, ...newTodos])
                this.buildCalendarDates()
                this.listFinished = this.allSourcesFinished()
            } catch (error) {
                console.error('加载待办列表失败:', error)
                this.listFinished = false
            } finally {
                this.loading = false
                this.listLoading = false
            }
        },
        async loadTodoStats() {
            const today = this.formatDate(new Date())
            try {
                const [activeResponse, completedResponse, todayResponse] = await Promise.all([
                    listDo(this.buildTodoQuery({ pageNum: 1, pageSize: 1, status: false })),
                    listDo(this.buildTodoQuery({ pageNum: 1, pageSize: 1, status: true })),
                    listDo(this.buildTodoQuery({
                        pageNum: 1,
                        pageSize: 1,
                        planStartTime: `${today} 00:00:00`,
                        planEndTime: `${today} 23:59:59`
                    }))
                ])
                if (activeResponse.code === 200) this.activeTotal = activeResponse.total || 0
                if (completedResponse.code === 200) this.completedTotal = completedResponse.total || 0
                if (todayResponse.code === 200) this.todayTotal = todayResponse.total || 0
            } catch (error) {
                console.error('加载待办统计失败:', error)
                this.activeTotal = null
                this.completedTotal = null
                this.todayTotal = null
            }
        },
        async loadNextTodoBatch() {
            const result = []
            for (const key of this.tabSources) {
                if (result.length >= this.pageSize) break
                const rows = await this.takeFromSource(key, this.pageSize - result.length)
                result.push(...rows)
            }
            return result
        },
        async takeFromSource(key, count) {
            const source = this.sources[key]
            while (source.buffer.length < count && !source.finished) {
                const response = await listDo(this.buildTodoQuery({
                    ...this.sourceParams(key),
                    pageNum: source.pageNum,
                    pageSize: this.pageSize
                }))
                if (response.code === 200) {
                    const rows = response.rows || []
                    source.total = response.total || 0
                    source.buffer.push(...rows)
                    source.pageNum++
                    if (rows.length < this.pageSize || (source.pageNum - 1) * this.pageSize >= source.total) {
                        source.finished = true
                    }
                } else {
                    source.finished = true
                }
            }
            return source.buffer.splice(0, count)
        },
        sortTodoList(list) {
            return [...list].sort((a, b) => {
                const aCompleted = this.isTodoCompleted(a)
                const bCompleted = this.isTodoCompleted(b)
                if (aCompleted !== bCompleted) {
                    return aCompleted ? 1 : -1
                }
                return this.getTodoSortTime(a) - this.getTodoSortTime(b)
            })
        },
        getTodoSortTime(todo) {
            const time = todo.planEndTime || todo.planStartTime || todo.createTime || ''
            const value = new Date(String(time).replace(/-/g, '/')).getTime()
            return Number.isNaN(value) ? Number.MAX_SAFE_INTEGER : value
        },

        // ---- 日历 ----
        calendarFormatter(day) {
            const date = `${day.date.getFullYear()}-${String(day.date.getMonth() + 1).padStart(2, '0')}-${String(day.date.getDate()).padStart(2, '0')}`
            if (this.allTodosMap[date]) {
                day.bottomInfo = '•'
            }
            return day
        },
        buildCalendarDates() {
            this.allTodosMap = {}
            this.todoList.forEach(todo => {
                const date = todo.planStartTime ? todo.planStartTime.substring(0, 10) : ''
                if (date) {
                    if (!this.allTodosMap[date]) this.allTodosMap[date] = []
                    this.allTodosMap[date].push(todo)
                }
            })
            this.calendarKey++
        },
        loadSelectedDateTodos() {
            const todos = this.allTodosMap[this.selectedDate] || []
            if (this.selectedLabel) {
                this.selectedDateTodos = todos.filter(todo => {
                    const labels = todo.label ? todo.label.split(',').filter(l => l.trim()) : []
                    return labels.includes(this.selectedLabel)
                })
            } else {
                this.selectedDateTodos = todos
            }
        },
        onDateClick(date) {
            this.selectedDate = this.formatDate(date)
            this.loadSelectedDateTodos()
        },

        // ---- 操作 ----
        goEdit(query) {
            const path = `/m/todo/edit${query ? '?' + query : ''}`
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        handleAdd() {
            this.goEdit()
        },
        handleTodoClick(todo) {
            this.goEdit(`id=${todo.id}`)
        },
        handleTodoEdit(todo) {
            this.goEdit(`id=${todo.id}`)
        },
        async handleTodoDelete(todo) {
            try {
                await mConfirm('确认删除', '确定要删除这个待办吗？')
            } catch (e) {
                return
            }
            try {
                const response = await delDo(todo.id)
                if (response.code === 200) {
                    mSuccess('删除成功')
                    this.refreshTodoList()
                }
            } catch (error) {
                console.error('删除待办失败:', error)
                mError('删除失败')
            }
        },
        async handleTodoToggle(todo) {
            try {
                const completed = this.isTodoCompleted(todo)
                const data = {
                    ...todo,
                    status: !completed,
                    finishTime: !completed ? new Date().toISOString() : '',
                    finishBy: !completed ? '' : ''
                }
                const response = await updateDo(data)
                if (response.code === 200) {
                    mSuccess(data.status ? '已完成' : '未完成')
                    this.refreshTodoList()
                }
            } catch (error) {
                console.error('切换待办状态失败:', error)
                mError('操作失败')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.todo-page {
    min-height: calc(100vh - 46px);
    padding: 12px 12px 80px;
    box-sizing: border-box;
    background: var(--pnkx-bg);
}

.todo-header {
    margin-bottom: 10px;

    &__panel {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 14px;
        margin-bottom: 10px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 12px;
    }

    &__title {
        font-size: 20px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.2;
    }

    &__subtitle {
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__progress {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: var(--pnkx-primary);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 12px rgba(91, 141, 239, 0.35);
    }

    &__progress-num {
        font-size: 14px;
        font-weight: 700;
        color: #fff;
        line-height: 1.1;
    }

    &__progress-label {
        margin-top: 2px;
        font-size: 9px;
        color: rgba(255, 255, 255, 0.86);
    }

    &__tabs {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 6px;
        margin-bottom: 10px;
    }

    &__search {
        padding: 0;
        background: transparent;

        :deep(.van-search__content) {
            background: var(--pnkx-surface);
            border: 1px solid var(--pnkx-border-soft);
        }
    }

    &__switch {
        display: flex;
        gap: 4px;
        padding: 3px;
        margin-top: 10px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 999px;

        .switch-item {
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

            &.active {
                color: var(--pnkx-primary);
                background: var(--pnkx-primary-soft);
                font-weight: 600;
            }
        }
    }

    &__labels {
        margin-top: 10px;
        overflow: hidden;
    }
}

.todo-tab {
    min-width: 0;
    padding: 8px 4px;
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 10px;
    background: var(--pnkx-surface);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;

    &.active {
        border-color: var(--pnkx-primary);
        background: var(--pnkx-primary-soft);
    }

    &__value {
        font-size: 15px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.1;
    }

    &__label {
        font-size: 10px;
        color: var(--pnkx-text-muted);
        line-height: 1.2;
    }
}

.label-scroll {
    display: flex;
    gap: 7px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 2px;

    &::-webkit-scrollbar {
        display: none;
    }

    .label-chip {
        flex-shrink: 0;
        display: inline-flex;
        align-items: center;
        height: 28px;
        padding: 0 13px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border);
        border-radius: 999px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        transition: all 0.25s;

        &.active {
            background: var(--pnkx-primary);
            color: #fff;
            border-color: var(--pnkx-primary);
            font-weight: 600;
        }
    }
}

.calendar-card {
    padding: 8px 4px 4px;

    :deep(.van-calendar) {
        background: transparent;
    }

    :deep(.van-calendar__bottom-info) {
        bottom: 2px;
        font-size: 10px;
        color: var(--pnkx-primary);
    }

    :deep(.van-calendar__day) {
        height: 52px;
    }

    :deep(.van-calendar__header-subtitle),
    :deep(.van-calendar__month-title) {
        color: var(--pnkx-text);
    }
}

.section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    font-weight: 600;
    color: var(--pnkx-text);
    margin: 14px 2px 10px;

    .section-count {
        font-size: 12px;
        font-weight: 400;
        color: var(--pnkx-text-muted);
    }
}
</style>
