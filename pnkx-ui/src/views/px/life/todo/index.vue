<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021/12/30 17:43
 * @Description: 待办事项 - Modern UI/UX Refactored
-->
<template>
  <div class="todo-container">
    <!-- 左侧导航面板 -->
    <aside class="sidebar">
      <!-- 搜索栏 -->
      <div class="search-wrapper">
        <div class="back-btn" @click="handleBack">
          <svg-icon icon-class="back" />
        </div>
        <div class="search-box">
          <svg-icon icon-class="搜索" class="search-icon" />
          <el-select
            v-model="toDoSearch"
            class="search-select"
            :remote-method="handleSearch"
            clearable
            :loading="searchLoading"
            filterable
            default-first-option
            remote
            placeholder="搜索待办..."
            @change="handleChange"
          >
            <el-option
              v-for="item in options"
              :key="item.id"
              :label="item.content"
              :value="item.id"
            />
          </el-select>
        </div>
      </div>

      <!-- 导航过滤 -->
      <div class="nav-list">
        <div
          v-for="(nav, index) in navList"
          :key="nav.key"
          class="nav-card"
          :class="{ active: activeNav === nav.key }"
          @click="selectNav(nav.key)"
        >
          <div class="nav-icon-wrapper" :style="{ background: nav.gradient }">
            <svg-icon :icon-class="nav.icon" class="nav-icon" />
          </div>
          <div class="nav-info">
            <span class="nav-label">{{ nav.label }}</span>
            <span class="nav-count">{{ nav.count }}</span>
          </div>
        </div>
      </div>

      <!-- 标签过滤 -->
      <div class="tag-section">
        <div class="section-title">标签</div>
        <div class="tag-cloud">
          <span
            v-for="(tag, index) in labelOptions"
            :key="tag"
            class="tag-item"
            :class="[
              tagTypes[index % tagTypes.length],
              { active: activeTag === tag }
            ]"
            @click="handleSearchByLabel(tag)"
          >
            {{ tag }}
          </span>
        </div>
      </div>
    </aside>

    <!-- 右侧主内容 -->
    <main class="main-area" v-loading="loading">
      <div class="main-content">
        <!-- 未选中详情时显示列表 -->
        <template v-if="!detailVisible">
          <!-- 新建待办输入区 -->
          <div class="input-area">
            <div class="input-row">
              <svg-icon icon-class="编辑02" class="input-icon" />
              <input
                v-model="toDoForm.content"
                placeholder="新建待办，例如：下班后去买菜..."
                class="todo-input"
                @focus="selectTime = true"
                @keyup.enter="addDo"
              />
            </div>
            <transition name="slide-down">
              <div v-if="selectTime" class="time-row">
                <el-date-picker
                  v-model="toDoForm.time"
                  type="datetimerange"
                  :picker-options="pickerOptions"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  align="right"
                  size="small"
                />
                <el-button
                  type="primary"
                  size="small"
                  icon="Check"
                  @click="addDo"
                  :loading="addButtonLoading"
                  :disabled="newToDoFlag"
                  class="add-btn"
                >
                  新建
                </el-button>
              </div>
            </transition>
          </div>

          <!-- 未完成列表 -->
          <div class="list-section">
            <div class="section-header">
              <svg-icon icon-class="时间" class="section-icon" />
              <span class="section-title-text">未完成</span>
              <span class="section-count">{{ noFinishList.length }}</span>
            </div>
            <transition-group name="todo-list" tag="div" class="todo-items">
              <div
                v-for="(item, index) in noFinishList"
                :key="item.id"
                class="todo-card"
                :style="{ animationDelay: `${index * 0.05}s` }"
                @click="openToDoDetails(item)"
                @contextmenu.prevent.stop="handleRightClick($event, item)"
              >
                <div
                  class="checkbox"
                  :class="{ checked: item.status }"
                  @click.stop="changeStatus(item)"
                >
                  <svg-icon v-if="item.status" icon-class="正确" class="check-icon" />
                </div>
                <div class="todo-body">
                  <div class="todo-content">
                    <span
                      v-if="item.priority > 0"
                      class="priority-dot"
                      :class="'priority-' + item.priority"
                      :title="priorityLabel(item.priority)"
                    ></span>
                    {{ item.content }}
                  </div>
                  <div class="todo-meta">
                    <template v-if="item.label">
                      <span
                        v-for="(tag, ti) in item.label.split(',')"
                        :key="ti"
                        class="mini-tag"
                        :class="tagTypes[ti % tagTypes.length]"
                      >{{ tag }}</span>
                    </template>
                    <span v-if="item.planStartTime" class="meta-time">
                      <svg-icon icon-class="时间" class="time-icon" />
                      {{ item.planStartTime && item.planStartTime.slice(5, 16) }}
                    </span>
                  </div>
                </div>
              </div>
            </transition-group>
            <div v-if="noFinishList.length === 0" class="empty-state">
              <svg-icon icon-class="编辑02" class="empty-icon" />
              <p>暂无未完成待办</p>
              <p class="hint">在上方输入框新建待办</p>
            </div>
          </div>

          <!-- 已完成列表 -->
          <div class="list-section finished-section">
            <div class="section-header">
              <svg-icon icon-class="正确" class="section-icon done-icon" />
              <span class="section-title-text">已完成</span>
              <span class="section-count">{{ total }}</span>
            </div>
            <transition-group name="todo-list" tag="div" class="todo-items">
              <div
                v-for="(item, index) in finishList"
                :key="'f-' + item.id"
                class="todo-card finished"
                :style="{ animationDelay: `${index * 0.05}s` }"
                @click="openToDoDetails(item)"
                @contextmenu.prevent.stop="handleRightClick($event, item)"
              >
                <div
                  class="checkbox checked"
                  @click.stop="changeStatus(item)"
                >
                  <svg-icon icon-class="正确" class="check-icon" />
                </div>
                <div class="todo-body">
                  <div class="todo-content done-text">
                    <span
                      v-if="item.priority > 0"
                      class="priority-dot"
                      :class="'priority-' + item.priority"
                    ></span>
                    {{ item.content }}
                  </div>
                  <div class="todo-meta">
                    <span v-if="item.finishTime" class="meta-time finish-time">
                      <svg-icon icon-class="时间" class="time-icon" />
                      {{ item.finishTime }}
                    </span>
                  </div>
                </div>
              </div>
            </transition-group>
            <pagination
              v-show="total > 0"
              v-model:limit="finishParams.pageSize"
              v-model:page="finishParams.pageNum"
              :total="total"
              @pagination="getFinishToDoList"
            />
          </div>
        </template>

        <!-- 详情编辑区 -->
        <template v-else>
          <div class="detail-panel">
            <!-- 详情头部 -->
            <div class="detail-header">
              <div class="detail-back" @click="closeDetail">
                <svg-icon icon-class="back" />
                <span>返回列表</span>
              </div>
              <div class="detail-actions">
                <el-button
                  size="small"
                  type="danger"
                                    @click="handleDelete(toDoDetails)"
                >
                  删除
                </el-button>
              </div>
            </div>

            <!-- 内容输入 -->
            <div class="detail-content-input">
              <input
                v-model="toDoDetails.content"
                placeholder="待办内容..."
                class="detail-title-input"
              />
            </div>

            <!-- 属性区域 -->
            <div class="detail-props">
              <!-- 状态 -->
              <div class="prop-row">
                <div class="prop-label">
                  <svg-icon icon-class="正确" class="prop-icon" />
                  <span>状态</span>
                </div>
                <el-switch
                  v-model="toDoDetails.status"
                  active-text="已完成"
                  inactive-text="未完成"
                />
              </div>

              <!-- 标签 -->
              <div class="prop-row">
                <div class="prop-label">
                  <svg-icon icon-class="验证码" class="prop-icon" />
                  <span>标签</span>
                </div>
                <div class="prop-content">
                  <span
                    v-for="(tag, index) in toDoDetails.label"
                    :key="tag"
                    class="detail-tag"
                    :class="tagTypes[index % tagTypes.length]"
                  >
                    {{ tag }}
                    <el-icon class="tag-close" @click="handleDeleteLabel(tag)"><Close /></el-icon>
                  </span>
                  <el-select
                    v-model="newLabel"
                    @change="handleChangeLabel"
                    filterable
                    allow-create
                    placeholder="添加标签"
                    size="small"
                    class="tag-select"
                  >
                    <el-option
                      v-for="item in labelOptions"
                      :key="item"
                      :label="item"
                      :value="item"
                    />
                  </el-select>
                </div>
              </div>

              <!-- 执行者 -->
              <div class="prop-row">
                <div class="prop-label">
                  <svg-icon icon-class="用户" class="prop-icon" />
                  <span>执行者</span>
                </div>
                <div class="prop-content">
                  <el-select
                    v-model="toDoDetails.performer"
                    multiple
                    placeholder="请选择执行者"
                    size="small"
                    class="full-select"
                  >
                    <el-option
                      v-for="item in userList"
                      :key="item.userId"
                      :label="item.nickName"
                      :value="item.userId"
                    />
                  </el-select>
                </div>
              </div>

              <!-- 时间 -->
              <div class="prop-row">
                <div class="prop-label">
                  <svg-icon icon-class="时间" class="prop-icon" />
                  <span>计划时间</span>
                </div>
                <div class="prop-content">
                  <el-date-picker
                    v-model="toDoDetailsTime"
                    type="datetimerange"
                    :picker-options="pickerOptions"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    align="right"
                    size="small"
                    class="full-select"
                  />
                </div>
              </div>
            </div>

            <!-- 备注 -->
            <div class="detail-editor">
              <editor
                ref="editor"
                :height="300"
                v-model="toDoDetails.remark"
              />
            </div>

            <!-- 浮动保存按钮 -->
            <div class="fab-save" @click="saveToDo" title="保存">
              <svg-icon icon-class="保存" />
            </div>
          </div>
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
  </div>
</template>

<script>
import { listUser } from '@/api/system/user'
import { addDo, delDo, getDo, listDo, updateDo, getLabelList } from '@/api/px/life/todo'
import Editor from '@/components/Editor'
import Pagination from '@/components/Pagination'

export default {
  name: 'index',
  components: { Editor, Pagination },
  data() {
    return {
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
      // 新增标签
      newLabel: '',
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
  computed: {
    // 新增待办
    newToDoFlag() {
      return this.toDoForm.content.length <= 0
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
     * 优先级标签
     */
    priorityLabel(p) {
      const map = {0: '无', 1: '低', 2: '中', 3: '高', 4: '紧急'}
      return map[p] || '无'
    },
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
     * 新加待办标签
     */
    handleChangeLabel(value) {
      if (!this.toDoDetails.label) this.toDoDetails.label = []
      this.toDoDetails.label.push(value)
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
     * 删除待办标签
     */
    handleDeleteLabel(tag) {
      this.toDoDetails.label.splice(this.toDoDetails.label.indexOf(tag), 1)
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
        this.newLabel = ''
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

.todo-container {
  display: flex;
  height: calc(100vh - 84px);
  background: var(--bg-body);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

// 左侧边栏
.sidebar {
  width: 320px;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--border-primary);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-sm);
  position: relative;
  z-index: 10;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-primary);
    border-radius: 3px;

    &:hover {
      background: var(--text-tertiary);
    }
  }
}

// 搜索栏
.search-wrapper {
  padding: var(--space-5);
  display: flex;
  gap: var(--space-3);
  align-items: center;
  border-bottom: 1px solid var(--border-primary);

  .back-btn {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-sm);
    background: var(--bg-card);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);

    &:hover {
      background: var(--color-primary);
      color: white;
      transform: translateX(-2px);
    }

    .svg-icon {
      font-size: 18px;
    }
  }

  .search-box {
    flex: 1;
    position: relative;

    .search-icon {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      font-size: var(--text-base);
      color: var(--text-tertiary);
      pointer-events: none;
      z-index: 1;
    }

    .search-select {
      width: 100%;

      ::v-deep .el-input__inner {
        height: 40px;
        padding: 0 var(--space-4) 0 42px;
        border: none;
        border-radius: var(--radius-md);
        background: var(--bg-card);
        font-size: var(--text-sm);
        color: var(--text-primary);
        box-shadow: var(--shadow-sm);

        &::placeholder {
          color: var(--text-tertiary);
        }

        &:focus {
          outline: none;
          box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12), var(--shadow-md);
        }
      }
    }
  }
}

// 导航列表
.nav-list {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  border-bottom: 1px solid var(--border-primary);
}

.nav-card {
  display: flex;
  align-items: center;
  padding: 14px var(--space-4);
  background: var(--bg-card);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  box-shadow: var(--shadow-sm);
  border-left: 3px solid transparent;

  &:hover {
    transform: translateX(4px);
    box-shadow: var(--shadow-md);
    background: var(--bg-hover);
  }

  &.active {
    background: var(--bg-hover);
    border-left-color: var(--color-primary);
  }

  .nav-icon-wrapper {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    margin-right: 14px;

    .nav-icon {
      font-size: 18px;
      color: white;
    }
  }

  .nav-info {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .nav-label {
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }

    .nav-count {
      font-size: var(--text-sm);
      color: var(--text-secondary);
      background: var(--bg-hover);
      padding: 2px 10px;
      border-radius: 12px;
      min-width: 28px;
      text-align: center;
    }
  }
}

// 标签区域
.tag-section {
  padding: var(--space-5) var(--space-4);

  .section-title {
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 14px;
  }

  .tag-cloud {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }
}

.tag-item {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 16px;
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  border: 1px solid transparent;

  &:hover {
    transform: translateY(-1px);
  }

  &.active {
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.25);
    transform: scale(1.05);
  }

  &.success {
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
    border-color: rgba(103, 194, 58, 0.2);
  }

  &.warning {
    background: rgba(230, 162, 60, 0.1);
    color: #e6a23c;
    border-color: rgba(230, 162, 60, 0.2);
  }

  &.danger {
    background: rgba(245, 108, 108, 0.1);
    color: #f56c6c;
    border-color: rgba(245, 108, 108, 0.2);
  }

  &.info {
    background: rgba(144, 147, 153, 0.1);
    color: #909399;
    border-color: rgba(144, 147, 153, 0.2);
  }

  // default
  background: rgba(64, 158, 255, 0.1);
  color: var(--color-primary);
  border-color: rgba(64, 158, 255, 0.2);

  &.success {
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
    border-color: rgba(103, 194, 58, 0.2);
  }

  &.active {
    background: var(--color-primary);
    color: white;
    border-color: var(--color-primary);
  }
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

// 输入区域
.input-area {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-5) 24px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;

  .input-row {
    display: flex;
    align-items: center;
    gap: var(--space-3);

    .input-icon {
      font-size: var(--text-lg);
      color: var(--color-primary);
    }

    .todo-input {
      flex: 1;
      font-size: var(--text-base);
      color: var(--text-primary);
      border: none;
      background: transparent;
      padding: var(--space-2) 0;
      border-bottom: 2px solid transparent;
      transition: all var(--duration-normal) var(--ease-default);

      &::placeholder {
        color: var(--text-tertiary);
      }

      &:focus {
        outline: none;
        border-bottom-color: var(--color-primary);
      }
    }
  }

  .time-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: var(--space-4);
    padding-top: var(--space-4);
    border-top: 1px solid var(--border-primary);

    .add-btn {
      border-radius: var(--radius-sm);
      margin-left: var(--space-3);
    }
  }
}

// 展开/收起动画
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all var(--duration-normal) var(--ease-default);
  max-height: 200px;
  overflow: hidden;
}

.slide-down-enter,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
  margin-top: 0;
  padding-top: 0;
}

// 列表区域
.list-section {
  margin-bottom: 32px;

  .section-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: var(--space-4);

    .section-icon {
      font-size: 18px;
      color: var(--color-primary);
    }

    .done-icon {
      color: #67c23a;
    }

    .section-title-text {
      font-size: var(--text-base);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }

    .section-count {
      font-size: var(--text-sm);
      color: var(--text-secondary);
      background: var(--bg-hover);
      padding: 2px 12px;
      border-radius: 12px;
    }
  }
}

.finished-section {
  border-top: 1px solid var(--border-primary);
  padding-top: 24px;
}

// 待办卡片
.todo-items {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.todo-card {
  display: flex;
  align-items: flex-start;
  padding: var(--space-4);
  background: var(--bg-card);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  box-shadow: var(--shadow-sm);
  border-left: 3px solid transparent;
  animation: fadeSlideIn 0.4s var(--ease-default) forwards;
  opacity: 0;

  &:hover {
    transform: translateX(4px);
    box-shadow: var(--shadow-md);
    background: var(--bg-hover);
  }

  &.finished {
    opacity: 0.7;

    .done-text {
      text-decoration: line-through;
      color: var(--text-tertiary);
    }
  }
}

@keyframes fadeSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 复选框
.checkbox {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid var(--border-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  margin-right: 14px;
  margin-top: 2px;
  flex-shrink: 0;

  &:hover {
    border-color: var(--color-primary);
    background: rgba(64, 158, 255, 0.05);
  }

  &.checked {
    background: #67c23a;
    border-color: #67c23a;

    .check-icon {
      font-size: var(--text-sm);
      color: white;
    }
  }
}

.todo-body {
  flex: 1;
  min-width: 0;

  .todo-content {
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--text-primary);
    line-height: 1.5;
    word-break: break-all;

    .priority-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-right: 6px;
      vertical-align: middle;
    }

    .priority-1 { background: #909399; }
    .priority-2 { background: #409eff; }
    .priority-3 { background: #e6a23c; }
    .priority-4 { background: #f56c6c; }
  }

  .todo-meta {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-top: var(--space-2);
    flex-wrap: wrap;

    .mini-tag {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 11px;

      &.success {
        background: rgba(103, 194, 58, 0.1);
        color: #67c23a;
      }

      &.warning {
        background: rgba(230, 162, 60, 0.1);
        color: #e6a23c;
      }

      &.danger {
        background: rgba(245, 108, 108, 0.1);
        color: #f56c6c;
      }

      &.info {
        background: rgba(144, 147, 153, 0.1);
        color: #909399;
      }

      background: rgba(64, 158, 255, 0.1);
      color: var(--color-primary);
    }

    .meta-time {
      display: flex;
      align-items: center;
      gap: var(--space-1);
      font-size: var(--text-xs);
      color: var(--text-tertiary);

      .time-icon {
        font-size: var(--text-xs);
      }
    }

    .finish-time {
      color: #67c23a;
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: var(--text-tertiary);

  .empty-icon {
    font-size: 64px;
    opacity: 0.3;
    margin-bottom: var(--space-4);
  }

  p {
    margin: var(--space-1) 0;
  }

  .hint {
    font-size: var(--text-xs);
    opacity: 0.7;
  }
}

// ===== 详情面板 =====
.detail-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) 24px;
  border-bottom: 1px solid var(--border-primary);
  background: var(--bg-card);

  .detail-back {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
    color: var(--text-secondary);
    cursor: pointer;
    padding: 6px 12px;
    border-radius: var(--radius-sm);
    transition: all var(--duration-normal) var(--ease-default);

    &:hover {
      background: var(--bg-hover);
      color: var(--color-primary);
    }
  }
}

.detail-content-input {
  padding: var(--space-5) 24px 8px;

  .detail-title-input {
    width: 100%;
    font-size: var(--text-xl);
    font-weight: var(--font-semibold);
    color: var(--text-primary);
    border: none;
    background: transparent;
    padding: var(--space-2) 0;
    border-bottom: 2px solid transparent;
    transition: all var(--duration-normal) var(--ease-default);

    &::placeholder {
      color: var(--text-tertiary);
    }

    &:focus {
      outline: none;
      border-bottom-color: var(--color-primary);
    }
  }
}

// 属性区域
.detail-props {
  padding: var(--space-4) 24px;

  .prop-row {
    display: flex;
    align-items: center;
    padding: var(--space-3) 0;
    border-bottom: 1px solid var(--border-primary);

    &:last-child {
      border-bottom: none;
    }

    .prop-label {
      width: 100px;
      display: flex;
      align-items: center;
      gap: var(--space-2);
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--text-secondary);
      flex-shrink: 0;

      .prop-icon {
        font-size: var(--text-base);
      }
    }

    .prop-content {
      flex: 1;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: var(--space-2);

      .detail-tag {
        display: inline-flex;
        align-items: center;
        gap: var(--space-1);
        padding: 4px 12px;
        border-radius: 14px;
        font-size: var(--text-xs);

        &.success {
          background: rgba(103, 194, 58, 0.1);
          color: #67c23a;
        }

        &.warning {
          background: rgba(230, 162, 60, 0.1);
          color: #e6a23c;
        }

        &.danger {
          background: rgba(245, 108, 108, 0.1);
          color: #f56c6c;
        }

        &.info {
          background: rgba(144, 147, 153, 0.1);
          color: #909399;
        }

        background: rgba(64, 158, 255, 0.1);
        color: var(--color-primary);

        .tag-close {
          cursor: pointer;
          font-size: var(--text-xs);
          margin-left: 2px;

          &:hover {
            color: #f56c6c;
          }
        }
      }

      .tag-select {
        width: 140px;
      }

      .full-select {
        width: 100%;
      }
    }
  }
}

// 编辑器
.detail-editor {
  flex: 1;
  padding: 0 24px 24px;
  min-height: 300px;
}

// 浮动保存按钮
.fab-save {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-lg);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  z-index: 100;

  .svg-icon {
    font-size: var(--text-xl);
  }

  &:hover {
    transform: scale(1.1) rotate(5deg);
    box-shadow: var(--shadow-lg);
  }

  &:active {
    transform: scale(0.95);
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

// 列表动画
.todo-list-enter-active,
.todo-list-leave-active {
  transition: all var(--duration-normal) var(--ease-default);
}

.todo-list-enter,
.todo-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
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
