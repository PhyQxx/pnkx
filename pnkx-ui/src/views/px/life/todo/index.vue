<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021/12/30 17:43
 * @Description: 待办事项 - Modern UI/UX Refactored
-->
<template>
  <div class="todo-page">
    <el-tabs v-model="activeTab" class="todo-tabs">
      <el-tab-pane label="待办列表" name="list">
        <div class="todo-container">
    <!-- 左侧导航面板 -->
    <todo-sidebar
      v-model="toDoSearch"
      :nav-list="navList"
      :active-nav="activeNav"
      :label-options="labelOptions"
      :active-tag="activeTag"
      :tag-types="tagTypes"
      :options="options"
      :search-loading="searchLoading"
      @back="handleBack"
      @search="handleSearch"
      @change="handleChange"
      @select-nav="selectNav"
      @select-tag="handleSearchByLabel"
    />

    <!-- 右侧主内容 -->
    <main class="main-area" v-loading="loading">
      <div class="main-content">
        <!-- 未选中详情时显示列表 -->
        <template v-if="!detailVisible">
          <todo-list-panel
            :to-do-form="toDoForm"
            :finish-params="finishParams"
            :no-finish-list="noFinishList"
            :finish-list="finishList"
            :total="total"
            :tag-types="tagTypes"
            :picker-options="pickerOptions"
            :add-button-loading="addButtonLoading"
            @add="addDo"
            @open="openToDoDetails"
            @contextmenu="handleRightClick"
            @change-status="changeStatus"
            @refresh-finish="getFinishToDoList"
          />
        </template>

        <!-- 详情编辑区 -->
        <template v-else>
          <todo-detail
            :to-do-details="toDoDetails"
            v-model:time="toDoDetailsTime"
            :label-options="labelOptions"
            :user-list="userList"
            :tag-types="tagTypes"
            :picker-options="pickerOptions"
            @close="detailVisible = false"
            @save="saveToDo"
            @delete="handleDelete(toDoDetails)"
            @open-reminder="$refs.todoReminder.open()"
          />
        </template>
      </div>
    </main>

    <!-- 右键菜单 -->
    <transition name="context-menu">
      <div
        v-if="rightFlag"
        class="context-menu"
        :style="rightStyle"
        v-clickOutSide="handleCloseRightClick"
      >
        <div
          v-for="item in rightFunctions"
          :key="item.id"
          class="menu-item"
          @click="handleRightAction(item)"
        >
          <svg-icon :icon-class="item.icon" class="menu-icon" />
          <span>{{ item.name }}</span>
        </div>
      </div>
    </transition>
    <ReminderSetting
      v-if="toDoDetails.id"
      ref="todoReminder"
      source-type="todo"
      :source-id="toDoDetails.id"
      :source-name="toDoDetails.content"
      :event-time="toDoDetails.planEndTime"
    />
        </div>
      </el-tab-pane>
      <el-tab-pane label="看板视图" name="kanban">
        <todo-kanban v-if="activeTab === 'kanban'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { listUser } from '@/api/system/user'
import { addDo, delDo, getDo, listDo, updateDo, getLabelList } from '@/api/px/life/todo'
import TodoKanban from './kanban.vue'
import TodoSidebar from './components/TodoSidebar.vue'
import TodoDetail from './components/TodoDetail.vue'
import TodoListPanel from './components/TodoListPanel.vue'
import ReminderSetting from '@/components/ReminderSetting'

export default {
  name: 'index',
  components: { TodoKanban, TodoSidebar, TodoDetail, TodoListPanel, ReminderSetting },
  data() {
    return {
      // 当前激活的 tab
      activeTab: 'list',
      // 新增按钮loading
      addButtonLoading: false,
      // 主区域loading
      loading: false,
      // 详情面板可见
      detailVisible: false,
      // 待办详情
      toDoDetails: {},
      // 待办详情时间
      toDoDetailsTime: [],
      // 导航数字
      navNumber: {
        all: 0,
        today: 0,
        charge: 0,
        started: 0
      },
      // 当前激活导航
      activeNav: '1',
      // 当前激活标签
      activeTag: '',
      // 导航列表配置
      navList: [
        { key: '1', label: '全部', icon: '编辑02', gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', count: 0 },
        { key: '2', label: '今天', icon: '时间', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', count: 0 },
        { key: '3', label: '我负责的', icon: '用户', gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', count: 0 },
        { key: '4', label: '我发起的', icon: '编辑', gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', count: 0 }
      ],
      // 标签颜色
      tagTypes: ['', 'success', 'warning', 'danger', 'info'],
      // 未完成待办列表
      noFinishList: [],
      // 已完成待办列表
      finishList: [],
      // 已完成待办数量
      total: 0,
      // 选择时间标志
      selectTime: false,
      // 待办内容
      toDoForm: {
        content: '',
        time: ''
      },
      // 待办搜索关键字
      toDoSearch: '',
      // 搜索加载标志
      searchLoading: false,
      // 待办事项选择项
      options: [],
      // 用户列表
      userList: [],
      // 时间选择快捷选项
      pickerOptions: {
        shortcuts: [
          {
            text: ' 一天',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              end.setTime(end.getTime() + 3600 * 1000 * 24)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: ' 一周',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              end.setTime(end.getTime() + 3600 * 1000 * 24 * 7)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: ' 一个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              end.setTime(end.getTime() + 3600 * 1000 * 24 * 30)
              picker.$emit('pick', [start, end])
            }
          }
        ]
      },
      // 获取待办列表参数
      noFinishParams: {
        status: '0'
      },
      finishParams: {
        status: '1',
        pageNum: 1,
        pageSize: 10
      },
      // 待选择标签
      labelOptions: [],
      // 右键菜单标志
      rightFlag: false,
      // 右键菜单样式
      rightStyle: '',
      // 右键功能
      rightFunctions: [],
      // 右键选中对象
      rightObject: {}
    }
  },
  mounted() {
    if (this.$route.params.id) {
      this.openToDoDetails({ id: this.$route.params.id })
    }
    this.listUser()
    this.getLabelList()
    this.listDo()
    this.getFinishToDoList()
  },
  methods: {
    /**
     * 返回
     */
    handleBack() {
      this.detailVisible = false
      this.activeTag = ''
      this.activeNav = '1'
      this.noFinishParams = { status: '0' }
      this.listDo()
    },
    /**
     * 关闭详情
     */
    closeDetail() {
      this.detailVisible = false
    },
    /**
     * 获取待办标签
     */
    getLabelList() {
      getLabelList().then(res => {
        this.labelOptions = res.data
      })
    },
    /**
     * 选择待办
     */
    handleChange(value) {
      if (!value) return
      this.openToDoDetails({ id: value })
    },
    /**
     * 根据label搜索待办
     */
    handleSearchByLabel(value) {
      if (this.activeTag === value) {
        this.activeTag = ''
        this.noFinishParams = { status: '0' }
      } else {
        this.activeTag = value
        this.toDoSearch = value
        this.noFinishParams = { status: '0', label: value }
      }
      this.listDo()
    },
    /**
     * 搜索待办
     */
    handleSearch(value) {
      this.searchLoading = true
      listDo({ searchValue: value }).then(res => {
        this.searchLoading = false
        this.options = res.rows
      })
    },
    /**
     * 删除待办
     */
    handleDelete(todo) {
      this.$confirm('确认删除该待办？', '删除提示', {
        type: 'warning'
      }).then(() => {
        delDo(todo.id).then(() => {
          this.$notify.success('删除成功')
          this.detailVisible = false
          this.listDo()
          this.getFinishToDoList()
        })
      }).catch(() => {})
    },
    /**
     * 选择菜单
     */
    selectNav(index) {
      this.activeNav = index
      this.activeTag = ''
      if (index === '1') {
        this.noFinishParams = { status: '0' }
      } else if (index === '2') {
        this.noFinishParams = {
          status: '0',
          planStartTime: this.getNow().slice(0, 10) + ' 00:00:00',
          planEndTime: this.getNow().slice(0, 10) + ' 23:59:59'
        }
      } else if (index === '3') {
        this.noFinishParams = {
          status: '0',
          performer: this.$store.state.user.id
        }
      } else if (index === '4') {
        this.noFinishParams = {
          status: '0',
          createBy: this.$store.state.user.id
        }
      }
      listDo(this.noFinishParams).then(res => {
        this.noFinishList = res.rows
      })
    },
    /**
     * 保存待办
     */
    saveToDo() {
      this.toDoDetails.performer = this.toDoDetails.performer.join(',')
      this.toDoDetails.planStartTime = this.parseTime(this.toDoDetailsTime[0])
      this.toDoDetails.planEndTime = this.parseTime(this.toDoDetailsTime[1])
      if (this.toDoDetails.status) {
        this.toDoDetails.finishTime = this.getNow()
        this.toDoDetails.finishBy = this.$store.state.user.id
      }
      if (Array.isArray(this.toDoDetails.label) && this.toDoDetails.label.length > 0) {
        this.toDoDetails.label = this.toDoDetails.label.join(',')
      }
      updateDo(this.toDoDetails).then(res => {
        if (res.code === 200) {
          this.listDo()
          this.getFinishToDoList()
          this.$notify.success('保存成功')
          this.detailVisible = false
          this.getLabelList()
        }
      })
    },
    /**
     * 打开待办详情
     */
    openToDoDetails(toDo) {
      this.toDoFlag = false
      getDo(toDo.id).then(res => {
        this.toDoDetails = res.data
        this.toDoDetails.performer = this.toDoDetails.performer
          ? this.toDoDetails.performer.split(',').map(item => Number(item))
          : []
        this.toDoDetailsTime = [
          this.toDoDetails.planStartTime ? new Date(this.toDoDetails.planStartTime) : '',
          this.toDoDetails.planEndTime ? new Date(this.toDoDetails.planEndTime) : ''
        ]
        this.toDoDetails.label = this.toDoDetails.label
          ? this.toDoDetails.label.split(',')
          : []
        this.detailVisible = true
      })
    },
    /**
     * 新建待办
     */
    addDo() {
      this.addButtonLoading = true
      const params = {
        content: this.toDoForm.content,
        planStartTime: this.toDoForm.time ? this.parseTime(this.toDoForm.time[0]) : '',
        planEndTime: this.toDoForm.time ? this.parseTime(this.toDoForm.time[1]) : ''
      }
      addDo(params).then(res => {
        if (res.code === 200) {
          this.$notify.success('新增成功')
          this.addButtonLoading = false
          this.listDo()
          this.getFinishToDoList()
          this.toDoForm = {
            content: '',
            time: ''
          }
          this.selectTime = false
        }
      })
    },
    /**
     * 获取人员列表
     */
    listUser() {
      listUser().then(res => {
        this.userList = res.rows
      })
    },
    /**
     * 获取完成待办列表
     */
    getFinishToDoList() {
      listDo(this.finishParams).then(res => {
        this.finishList = res.rows
        this.total = res.total
      })
    },
    /**
     * 获取未完成待办列表
     */
    listDo() {
      this.navNumber = {
        all: 0,
        today: 0,
        charge: 0,
        started: 0
      }
      listDo(this.noFinishParams).then(res => {
        this.noFinishList = res.rows
        res.rows.forEach(item => {
          if (!item.status) {
            this.navNumber.all++
            if (item.planStartTime && item.planEndTime && item.planStartTime < this.getNow() && item.planEndTime > this.getNow()) {
              this.navNumber.today++
            }
            if (item.performer && item.performer.split(',').indexOf(this.$store.state.user.id + '') !== -1) {
              this.navNumber.charge++
            }
            if (item.createBy && item.createBy === this.$store.state.user.id + '') {
              this.navNumber.started++
            }
          }
        })
        // 更新导航计数
        this.navList[0].count = this.navNumber.all
        this.navList[1].count = this.navNumber.today
        this.navList[2].count = this.navNumber.charge
        this.navList[3].count = this.navNumber.started
      })
    },
    /**
     * 改变状态
     */
    changeStatus(item) {
      const params = {
        id: item.id,
        status: !item.status
      }
      if (!item.status) {
        params.finishTime = this.getNow()
        params.finishBy = this.$store.state.user.id
      }
      updateDo(params).then(res => {
        if (res.code === 200) {
          item.status = !item.status
          this.listDo()
          this.getFinishToDoList()
        }
      })
    },
    /**
     * 右键点击
     */
    handleRightClick(event, item) {
      this.rightObject = item
      this.rightFunctions = [
        { id: 1, name: '编辑', icon: '编辑' },
        { id: 2, name: item.status ? '标记未完成' : '标记完成', icon: '正确' },
        { id: 3, name: '删除', icon: '删除' }
      ]
      this.rightFlag = true
      this.rightStyle = `top: ${Math.min(event.y, window.innerHeight - this.rightFunctions.length * 48)}px; left: ${Math.min(event.x - 180, window.innerWidth - 200)}px;`
    },
    /**
     * 右键操作
     */
    handleRightAction(item) {
      this.rightFlag = false
      switch (item.id) {
        case 1:
          this.openToDoDetails(this.rightObject)
          break
        case 2:
          this.changeStatus(this.rightObject)
          break
        case 3:
          this.handleDelete(this.rightObject)
          break
      }
    },
    /**
     * 关闭右键菜单
     */
    handleCloseRightClick() {
      this.rightFlag = false
    }
  }
}
</script>

<style lang="scss" scoped>
// ==================== 容器 ====================

.todo-page {
  height: calc(100vh - 84px);
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.todo-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__content) {
    flex: 1;
    overflow: hidden;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }
}

.todo-container {
  display: flex;
  height: 100%;
  background: var(--bg-body);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

// 右侧主区域
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  padding: 32px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-primary);
    border-radius: 3px;
  }
}

// 右键菜单
.context-menu {
  position: fixed;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: var(--space-2) 0;
  min-width: 180px;
  z-index: 9999;
  border: 1px solid var(--border-primary);

  .menu-item {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    font-size: var(--text-sm);
    color: var(--text-primary);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);

    .menu-icon {
      font-size: var(--text-base);
      color: var(--text-secondary);
    }

    &:hover {
      background: var(--bg-hover);
      color: var(--color-primary);

      .menu-icon {
        color: var(--color-primary);
      }
    }
  }
}

// 右键菜单动画
.context-menu-enter-active,
.context-menu-leave-active {
  transition: all var(--duration-fast) var(--ease-default);
}

.context-menu-enter,
.context-menu-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(-8px);
}

// Loading 美化
::v-deep .el-loading-mask {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

// 通知美化
::v-deep .el-notification {
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: none;
}

// 分页美化
::v-deep .pagination-container {
  padding: var(--space-4) 0 0;
}
</style>
