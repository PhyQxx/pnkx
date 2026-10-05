<!-- eslint-disable vue/no-mutating-props -->
<!--
 * @Author: PHY
 * @Description: 待办列表面板（新建输入区/未完成列表/已完成分页列表）—— 从 index.vue 拆出
 * 说明：toDoForm/finishParams 按引用与父组件共享就地编辑；业务动作经事件上抛
-->
<template>
  <div>
    <!-- 新建待办输入区 -->
    <div class="input-area">
      <div class="input-row">
        <svg-icon icon-class="编辑02" class="input-icon" />
        <input
          v-model="toDoForm.content"
          placeholder="新建待办，例如：下班后去买菜..."
          class="todo-input"
          @focus="selectTime = true"
          @keyup.enter="$emit('add')"
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
            @click="$emit('add')"
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
          @click="$emit('open', item)"
          @contextmenu.prevent.stop="$emit('contextmenu', $event, item)"
        >
          <div
            class="checkbox"
            :class="{ checked: item.status }"
            @click.stop="$emit('change-status', item)"
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
          @click="$emit('open', item)"
          @contextmenu.prevent.stop="$emit('contextmenu', $event, item)"
        >
          <div
            class="checkbox checked"
            @click.stop="$emit('change-status', item)"
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
        @pagination="$emit('refresh-finish')"
      />
    </div>
  </div>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
import Pagination from '@/components/Pagination'

export default {
  name: 'TodoListPanel',
  components: { Pagination },
  emits: ['add', 'open', 'contextmenu', 'change-status', 'refresh-finish'],
  props: {
    // 新建表单（按引用共享：content/time 就地编辑）
    toDoForm: { type: Object, required: true },
    // 已完成分页参数（按引用共享：pageNum/pageSize 就地编辑）
    finishParams: { type: Object, required: true },
    noFinishList: { type: Array, default: () => [] },
    finishList: { type: Array, default: () => [] },
    total: { type: Number, default: 0 },
    tagTypes: { type: Array, default: () => [] },
    pickerOptions: { type: Object, default: () => ({}) },
    addButtonLoading: { type: Boolean, default: false }
  },
  data() {
    return {
      // 时间选择行展开标志
      selectTime: false
    }
  },
  computed: {
    // 新增待办
    newToDoFlag() {
      return this.toDoForm.content.length <= 0
    }
  },
  methods: {
    /**
     * 优先级标签
     */
    priorityLabel(p) {
      const map = {0: '无', 1: '低', 2: '中', 3: '高', 4: '紧急'}
      return map[p] || '无'
    }
  }
}
</script>

<style lang="scss" scoped>
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
</style>
