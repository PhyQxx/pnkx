<!--
 * @File: index
 * @Author: PHY
 * @Date: 2025/03/09
 * @Description: 待办列表页
-->
<template>
  <view class="todo-page subpage-shell">
    <view class="fixed-header">
      <view class="overview-panel">
        <view class="overview-main">
          <text class="overview-title">待办事项</text>
          <text class="overview-subtitle">{{ headerDateText }}</text>
        </view>
        <view class="overview-progress">
          <text class="progress-number">{{ completionRate }}%</text>
          <text class="progress-label">完成率</text>
        </view>
      </view>

      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">{{ totalTodoCount }}</text>
          <text class="stat-label">全部</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ activeTodoCount }}</text>
          <text class="stat-label">进行中</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ todayTodoCount }}</text>
          <text class="stat-label">今日</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ completedTodoCount }}</text>
          <text class="stat-label">完成</text>
        </view>
      </view>

      <view class="search-bar">
        <uni-search-bar
          v-model="searchKeyword"
          placeholder="搜索待办..."
          @confirm="handleSearch"
          @clear="handleSearch"
          @input="onSearchInput"
          radius="100"
          bgColor="#F5F5F5"
          :focus="false"
          :show-action="false"
        />
      </view>

      <view class="view-switch">
        <view
          class="switch-item"
          :class="{ active: viewMode === 'timeline' }"
          @click="switchView('timeline')"
        >
          <uni-icons type="list" size="17" :color="viewMode === 'timeline' ? '#34D399' : '#8EA0B8'" />
          <text>列表</text>
        </view>
        <view
          class="switch-item"
          :class="{ active: viewMode === 'calendar' }"
          @click="switchView('calendar')"
        >
          <uni-icons type="calendar" size="17" :color="viewMode === 'calendar' ? '#34D399' : '#8EA0B8'" />
          <text>日历</text>
        </view>
      </view>

      <scroll-view class="label-scroll" scroll-x :show-scrollbar="false">
        <view class="label-list">
          <view
            class="label-item"
            :class="{ active: selectedLabel === '' }"
            @click="handleLabelClick('')"
          >
            <text>全部</text>
          </view>
          <view
            v-for="(label, index) in labelList"
            :key="index"
            class="label-item"
            :class="{ active: selectedLabel === label }"
            @click="handleLabelClick(label)"
          >
            <text>{{ label }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 内容区域 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      @scrolltolower="loadMore"
      lower-threshold="50"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 日历视图 -->
      <view v-if="viewMode === 'calendar'" class="calendar-view">
        <view class="calendar-card">
          <uni-calendar
            :selected="calendarDates"
            :insert="true"
            :lunar="false"
            :startDate="calendarStartDate"
            :endDate="calendarEndDate"
            @monthSwitch="onMonthSwitch"
            @change="onDateClick"
          />
        </view>

        <view v-if="selectedDateTodos.length > 0" class="selected-date-todos">
          <view class="section-title">
            <text>{{ formatDateFull(selectedDate) }}</text>
            <text class="section-count">{{ selectedDateTodos.length }} 项</text>
          </view>
          <TodoCard
            v-for="todo in selectedDateTodos"
            :key="todo.id"
            :todo="todo"
            @click="handleTodoClick"
            @edit="handleTodoEdit"
            @delete="handleTodoDelete"
            @toggle-status="handleTodoToggle"
          />
        </view>

        <view v-else-if="selectedDate" class="empty-state">
          <view class="empty-icon">
            <uni-icons type="checkbox" size="42" color="#34D399" />
          </view>
          <text class="empty-text">该日期暂无待办</text>
          <text class="empty-tip">换一天看看，或创建新的安排</text>
        </view>
      </view>

      <view v-else class="timeline-view">
        <view v-if="todoList.length === 0" class="empty-state">
          <view class="empty-icon">
            <uni-icons type="checkbox" size="42" color="#34D399" />
          </view>
          <text class="empty-text">暂无待办</text>
          <text class="empty-tip">点击右下角按钮开始添加待办吧</text>
        </view>
        <view v-else class="todo-list">
          <TodoCard
            v-for="todo in todoList"
            :key="todo.id"
            :todo="todo"
            @click="handleTodoClick"
            @edit="handleTodoEdit"
            @delete="handleTodoDelete"
            @toggle-status="handleTodoToggle"
          />
        </view>
      </view>

      <view v-if="viewMode === 'timeline' && todoList.length > 0" class="load-more">
        <uni-load-more :status="loadMoreStatus" />
      </view>
    </scroll-view>

    <view class="fab-button" @click="handleAdd">
      <uni-icons type="plus" size="24" color="#FFFFFF" />
    </view>
  </view>
</template>

<script>
import { listDo, updateDo, delDo, getLabelList } from '@/api/px/life/todo'
import { formatDate as _formatDate, formatDateFull as _formatDateFull } from '@/utils/pnkx'
import TodoCard from './components/TodoCard.vue'

export default {
  name: 'TodoIndex',
  components: {
    TodoCard
  },
  data() {
    return {
      searchKeyword: '',
      viewMode: 'timeline',
      selectedDate: '',
      selectedLabel: '',
      calendarStartDate: '',
      calendarEndDate: '',
      calendarDates: [],
      todoList: [],
      selectedDateTodos: [],
      allTodosMap: {},
      labelList: [],
      loading: false,
      isRefreshing: false,
      loadMoreStatus: 'more',
      pageNum: 1,
      pageSize: 10,
      total: 0,
      activeTotal: null,
      completedTotal: null,
      activePageNum: 1,
      completedPageNum: 1,
      activeFinished: false,
      completedFinished: false,
      completedBuffer: []
    }
  },
  computed: {
    activeTodoCount() {
      if (typeof this.activeTotal === 'number') {
        return this.activeTotal
      }
      return Math.max(this.totalTodoCount - this.completedTodoCount, 0)
    },
    totalTodoCount() {
      if (typeof this.activeTotal === 'number' && typeof this.completedTotal === 'number') {
        return this.activeTotal + this.completedTotal
      }
      return this.total || this.todoList.length
    },
    completedTodoCount() {
      if (typeof this.completedTotal === 'number') {
        return this.completedTotal
      }
      return this.todoList.filter(todo => this.isTodoCompleted(todo)).length
    },
    todayTodoCount() {
      const today = this.formatDate(new Date())
      return this.todoList.filter(todo => {
        const date = todo.planStartTime ? todo.planStartTime.substring(0, 10) : ''
        return date === today
      }).length
    },
    completionRate() {
      if (this.totalTodoCount === 0) return 0
      return Math.round((this.completedTodoCount / this.totalTodoCount) * 100)
    },
    headerDateText() {
      const now = new Date()
      const weekList = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return `${now.getMonth() + 1}月${now.getDate()}日 ${weekList[now.getDay()]}`
    }
  },
  onLoad() {
    this.initCalendarDates()
    this.loadLabelList()
    this.loadTodoList()
  },
  onShow() {
    this.refreshTodoList()
  },
  methods: {
    initCalendarDates() {
      const now = new Date()
      const year = now.getFullYear()

      this.calendarStartDate = `${year}-01-01`
      this.calendarEndDate = `${year + 1}-12-31`
      this.selectedDate = this.formatDate(now)
    },

    formatDate(date) {
      return _formatDate(date)
    },

    formatDateFull(date) {
      return _formatDateFull(date)
    },

    switchView(mode) {
      this.viewMode = mode
      if (mode === 'timeline' && this.todoList.length === 0) {
        this.loadTodoList()
      }
    },

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

    async loadTodoList(refresh = false) {
      if (this.loading) return

      this.loading = true

      if (refresh) {
        this.pageNum = 1
        this.todoList = []
        this.allTodosMap = {}
        this.activeTotal = null
        this.completedTotal = null
        this.activePageNum = 1
        this.completedPageNum = 1
        this.activeFinished = false
        this.completedFinished = false
        this.completedBuffer = []
      }

      try {
        if (refresh || this.activeTotal === null || this.completedTotal === null) {
          await this.loadTodoStats()
        }

        const newTodos = await this.loadNextTodoBatch()

        if (refresh) {
          this.todoList = this.sortTodoList(newTodos)
        } else {
          this.todoList = this.sortTodoList([...this.todoList, ...newTodos])
        }

        this.buildCalendarDates()
        this.loadMoreStatus = this.todoList.length >= this.totalTodoCount ? 'noMore' : 'more'

        if (this.selectedDate && this.viewMode === 'calendar') {
          this.loadSelectedDateTodos()
        }
      } catch (error) {
        console.error('加载待办列表失败:', error)
        this.loadMoreStatus = 'more'
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
        this.isRefreshing = false
      }
    },

    buildTodoQuery(extra = {}) {
      const params = { ...extra }

      if (this.searchKeyword) {
        params.content = this.searchKeyword
      }

      if (this.selectedLabel) {
        params.label = this.selectedLabel
      }

      return params
    },

    async loadTodoStats() {
      try {
        const [activeResponse, completedResponse] = await Promise.all([
          listDo(this.buildTodoQuery({
            pageNum: 1,
            pageSize: 1,
            status: false
          })),
          listDo(this.buildTodoQuery({
            pageNum: 1,
            pageSize: 1,
            status: true
          }))
        ])

        if (activeResponse.code === 200) {
          this.activeTotal = activeResponse.total || 0
        }

        if (completedResponse.code === 200) {
          this.completedTotal = completedResponse.total || 0
        }

        this.total = (this.activeTotal || 0) + (this.completedTotal || 0)
      } catch (error) {
        console.error('加载待办统计失败:', error)
        this.activeTotal = this.todoList.filter(todo => !this.isTodoCompleted(todo)).length
        this.completedTotal = this.todoList.filter(todo => this.isTodoCompleted(todo)).length
        this.total = this.activeTotal + this.completedTotal
      }
    },

    async loadNextTodoBatch() {
      const result = []

      if (!this.activeFinished) {
        const activeResponse = await listDo(this.buildTodoQuery({
          pageNum: this.activePageNum,
          pageSize: this.pageSize,
          status: false
        }))

        if (activeResponse.code === 200) {
          const rows = activeResponse.rows || []
          this.activeTotal = activeResponse.total || 0
          result.push(...rows)
          this.activePageNum++

          if (rows.length < this.pageSize || (this.activePageNum - 1) * this.pageSize >= this.activeTotal) {
            this.activeFinished = true
          }
        } else {
          this.activeFinished = true
        }
      }

      if (result.length < this.pageSize) {
        const completedRows = await this.loadCompletedTodos(this.pageSize - result.length)
        result.push(...completedRows)
      }

      return result
    },

    async loadCompletedTodos(count) {
      while (this.completedBuffer.length < count && !this.completedFinished) {
        const response = await listDo(this.buildTodoQuery({
          pageNum: this.completedPageNum,
          pageSize: this.pageSize,
          status: true
        }))

        if (response.code === 200) {
          const rows = response.rows || []
          this.completedTotal = response.total || 0
          this.completedBuffer.push(...rows)
          this.completedPageNum++

          if (rows.length < this.pageSize || (this.completedPageNum - 1) * this.pageSize >= this.completedTotal) {
            this.completedFinished = true
          }
        } else {
          this.completedFinished = true
        }
      }

      return this.completedBuffer.splice(0, count)
    },

    sortTodoList(list) {
      return [...list].sort((a, b) => {
        const aCompleted = this.isTodoCompleted(a)
        const bCompleted = this.isTodoCompleted(b)

        if (aCompleted !== bCompleted) {
          return aCompleted ? 1 : -1
        }

        const aTime = this.getTodoSortTime(a)
        const bTime = this.getTodoSortTime(b)
        return aTime - bTime
      })
    },

    getTodoSortTime(todo) {
      const time = todo.planEndTime || todo.planStartTime || todo.createTime || ''
      const value = new Date(time).getTime()
      return Number.isNaN(value) ? Number.MAX_SAFE_INTEGER : value
    },

    isTodoCompleted(todo) {
      const status = todo ? todo.status : false
      return status === true || status === 1 || status === '1' || status === 'true' || status === '已完成'
    },

    buildCalendarDates() {
      this.allTodosMap = {}
      this.calendarDates = []

      this.todoList.forEach(todo => {
        const date = todo.planStartTime ? todo.planStartTime.substring(0, 10) : ''
        if (date && !this.allTodosMap[date]) {
          this.allTodosMap[date] = []
          this.calendarDates.push({ date: date, info: 'dot' })
        }
        if (date) {
          this.allTodosMap[date].push(todo)
        }
      })
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

    loadMore() {
      if (this.loadMoreStatus !== 'more') return
      this.pageNum++
      this.loadMoreStatus = 'loading'
      this.loadTodoList()
    },

    onRefresh() {
      this.isRefreshing = true
      this.refreshTodoList()
    },

    refreshTodoList() {
      this.loadTodoList(true)
    },

    handleSearch() {
      this.loadTodoList(true)
    },

    onSearchInput(e) {
      if (e.value === '' || !e.value) {
        this.handleSearch()
      }
    },

    handleLabelClick(label) {
      this.selectedLabel = label
      if (this.viewMode === 'calendar') {
        this.loadSelectedDateTodos()
      } else {
        this.loadTodoList(true)
      }
    },

    onMonthSwitch(e) {
    },

    onDateClick(e) {
      this.selectedDate = e.fulldate
      this.loadSelectedDateTodos()
    },

    handleAdd() {
      uni.navigateTo({
        url: '/pages_life/todo/edit'
      })
    },

    handleTodoClick(todo) {
      uni.navigateTo({
        url: `/pages_life/todo/edit?id=${todo.id}`
      })
    },

    handleTodoEdit(todo) {
      uni.navigateTo({
        url: `/pages_life/todo/edit?id=${todo.id}`
      })
    },

    handleTodoDelete(todo) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这个待办吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              const response = await delDo(todo.id)
              if (response.code === 200) {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                })
                this.refreshTodoList()
              }
            } catch (error) {
              console.error('删除待办失败:', error)
              uni.showToast({
                title: '删除失败',
                icon: 'none'
              })
            }
          }
        }
      })
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
          uni.showToast({
            title: data.status ? '已完成' : '未完成',
            icon: 'success',
            duration: 1000
          })
          this.refreshTodoList()
        }
      } catch (error) {
        console.error('切换待办状态失败:', error)
        uni.showToast({
          title: '操作失败',
          icon: 'none'
        })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.todo-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba($todo, 0.12) 0%, $bg-page 360rpx);

  .fixed-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: linear-gradient(180deg, rgba($todo, 0.16) 0%, rgba(244, 246, 249, 0.96) 100%);
    padding: 24rpx $page-padding 18rpx;
    box-shadow: 0 8rpx 24rpx rgba(74, 85, 104, 0.06);

    .overview-panel {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: $spacing-md;

      .overview-main {
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .overview-title {
          font-size: 40rpx;
          font-weight: 700;
          color: $text-primary;
          line-height: 1.2;
        }

        .overview-subtitle {
          font-size: $font-caption;
          color: $text-secondary;
        }
      }

      .overview-progress {
        width: 124rpx;
        height: 124rpx;
        border-radius: $radius-full;
        background: linear-gradient(135deg, $todo 0%, $success-dark 100%);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        box-shadow: 0 12rpx 28rpx rgba($todo, 0.28);

        .progress-number {
          font-size: 30rpx;
          font-weight: 700;
          color: $text-inverse;
          line-height: 1.1;
        }

        .progress-label {
          margin-top: 4rpx;
          font-size: $font-mini;
          color: rgba(255, 255, 255, 0.86);
        }
      }
    }

    .stats-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12rpx;
      margin-bottom: 18rpx;

      .stat-item {
        min-width: 0;
        padding: 14rpx 10rpx;
        border: 1rpx solid rgba($todo, 0.16);
        border-radius: $radius-md;
        background-color: rgba(255, 255, 255, 0.76);
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4rpx;

        .stat-value {
          font-size: 30rpx;
          font-weight: 700;
          color: $text-primary;
          line-height: 1.1;
        }

        .stat-label {
          font-size: $font-mini;
          color: $text-tertiary;
          line-height: 1.2;
        }
      }
    }

    .search-bar {
      margin: 0 -16rpx;
    }

    .view-switch {
      display: flex;
      padding: 6rpx;
      margin-top: 14rpx;
      background-color: rgba(255, 255, 255, 0.82);
      border-radius: $radius-full;
      border: 1rpx solid rgba($todo, 0.14);
      gap: 6rpx;

      .switch-item {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10rpx;
        height: 64rpx;
        font-size: $font-caption;
        color: $text-tertiary;
        border-radius: $radius-full;
        transition: all 0.3s;

        &.active {
          color: $todo;
          background-color: #FFFFFF;
          box-shadow: $shadow-sm;
          font-weight: $font-weight-semibold;
        }
      }
    }

    .label-scroll {
      margin-top: 18rpx;
      white-space: nowrap;

      .label-list {
        display: flex;
        gap: 14rpx;
        padding-right: $page-padding;

        .label-item {
          display: inline-flex;
          align-items: center;
          height: 58rpx;
          padding: 0 26rpx;
          background-color: rgba(255, 255, 255, 0.78);
          border: 1rpx solid rgba($todo, 0.12);
          border-radius: $radius-full;
          font-size: $font-caption;
          color: $text-secondary;
          transition: all 0.3s;

          &.active {
            background-color: $todo;
            color: $text-inverse;
            border-color: $todo;
            box-shadow: 0 8rpx 18rpx rgba($todo, 0.22);
          }
        }
      }
    }
  }

  .content-scroll {
    flex: 1;
    height: 0;
    padding: 22rpx $page-padding 160rpx;
  }

  .calendar-view {
    .calendar-card {
      overflow: hidden;
      background-color: $bg-card;
      border-radius: $radius-lg;
      box-shadow: $shadow-card;
      margin-bottom: 24rpx;

      ::v-deep .uni-calendar {
        background-color: transparent;
        padding: 18rpx;
      }
    }
  }

  .timeline-view {
    .todo-list {
      padding-bottom: $spacing-md;
    }
  }

  .selected-date-todos {
    margin-top: $spacing-lg;

    .section-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      margin-bottom: $spacing-md;

      .section-count {
        font-size: $font-caption;
        font-weight: $font-weight-normal;
        color: $text-tertiary;
      }
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 430rpx;
    padding: $spacing-3xl 0;

    .empty-icon {
      width: 132rpx;
      height: 132rpx;
      border-radius: $radius-full;
      background-color: rgba($todo, 0.12);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .empty-text {
      font-size: $font-body;
      color: $text-secondary;
      margin-top: $spacing-lg;
      font-weight: $font-weight-semibold;
    }

    .empty-tip {
      font-size: $font-caption;
      color: $text-tertiary;
      margin-top: $spacing-xs;
    }
  }

  .load-more {
    padding: $spacing-md 0;
  }

  .fab-button {
    position: fixed;
    right: $page-padding;
    bottom: 92rpx;
    width: 104rpx;
    height: 104rpx;
    background: linear-gradient(135deg, $todo 0%, #22C55E 100%);
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 12rpx 28rpx rgba($todo, 0.38);
    z-index: 999;
    transition: transform 0.2s;

    &:active {
      transform: scale(0.92);
    }
  }
}

/* Glacier morning visual alignment */
.todo-page {
  background: transparent;

  .fixed-header {
    padding: 26rpx $page-padding 22rpx;
    background: rgba(248, 251, 255, 0.78);
    border-bottom: 1rpx solid rgba(214, 229, 247, 0.72);
    box-shadow: 0 10rpx 28rpx rgba(64, 111, 176, 0.08);
    backdrop-filter: blur(28rpx);

    .overview-panel {
      margin-bottom: 24rpx;
      padding: 24rpx 26rpx;
      border: 1rpx solid rgba(255, 255, 255, 0.94);
      border-radius: $radius-2xl;
      background: rgba(255, 255, 255, 0.86);
      box-shadow: $shadow-card;

      .overview-progress {
        width: 112rpx;
        height: 112rpx;
        background: linear-gradient(135deg, #6EA2FF 0%, $primary 58%, #8589FF 100%);
        box-shadow: 0 12rpx 28rpx rgba(79, 134, 247, 0.24);
      }
    }

    .stats-row {
      gap: 14rpx;

      .stat-item {
        padding: 16rpx 8rpx;
        border-color: rgba(188, 210, 239, 0.3);
        border-radius: $radius-lg;
        background: rgba(255, 255, 255, 0.82);
      }
    }

    .search-bar { margin: 0; }

    .view-switch {
      border-color: rgba(188, 210, 239, 0.3);
      background: rgba(255, 255, 255, 0.7);

      .switch-item.active {
        color: $primary;
        background: rgba(255, 255, 255, 0.96);
      }
    }

    .label-scroll .label-list .label-item {
      flex-shrink: 0;
      white-space: nowrap;
      border-color: rgba(188, 210, 239, 0.34);

      &.active {
        color: $text-inverse;
        border-color: $primary;
        background: $primary;
        box-shadow: 0 8rpx 18rpx rgba(79, 134, 247, 0.2);
      }
    }
  }

  .content-scroll { padding-top: 28rpx; }

  .empty-state .empty-icon { background: rgba(79, 134, 247, 0.1); }

  .fab-button {
    background: linear-gradient(135deg, #6EA2FF 0%, $primary 58%, #8589FF 100%);
    box-shadow: 0 14rpx 34rpx rgba(79, 134, 247, 0.3);
  }
}
</style>
