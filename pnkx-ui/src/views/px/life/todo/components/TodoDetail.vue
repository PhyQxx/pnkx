<!-- eslint-disable vue/no-mutating-props -->
<!--
 * @Author: PHY
 * @Description: 待办详情编辑面板（内容/属性/标签/备注/保存）—— 从 index.vue 拆出
 * 说明：toDoDetails 按引用共享，字段就地编辑；保存/删除等业务动作由父组件处理
-->
<template>
  <div class="detail-panel">
    <!-- 详情头部 -->
    <div class="detail-header">
      <div class="detail-back" @click="$emit('close')">
        <svg-icon icon-class="back" />
        <span>返回列表</span>
      </div>
      <div class="detail-actions">
        <el-button
          size="small"
          :disabled="!toDoDetails.planEndTime"
          @click="$emit('open-reminder')"
        >
          设置提醒
        </el-button>
        <el-button
          size="small"
          type="danger"
          @click="$emit('delete')"
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
            v-model="timeModel"
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
    <div class="fab-save" @click="$emit('save')" title="保存">
      <svg-icon icon-class="保存" />
    </div>
  </div>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
import Editor from '@/components/Editor'

export default {
  name: 'TodoDetail',
  components: { Editor },
  emits: ['close', 'save', 'delete', 'open-reminder', 'update:time'],
  props: {
    // 待办详情对象（按引用共享，字段就地编辑；保存动作由父组件触发）
    toDoDetails: { type: Object, required: true },
    // 计划时间区间 [start, end]（v-model:time）
    time: { type: Array, default: () => [] },
    labelOptions: { type: Array, default: () => [] },
    userList: { type: Array, default: () => [] },
    tagTypes: { type: Array, default: () => [] },
    pickerOptions: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      // 新增标签
      newLabel: ''
    }
  },
  watch: {
    // 切换到另一条待办时重置标签输入
    'toDoDetails.id'() {
      this.newLabel = ''
    }
  },
  computed: {
    timeModel: {
      get() {
        return this.time
      },
      set(value) {
        this.$emit('update:time', value)
      }
    }
  },
  methods: {
    /**
     * 新加待办标签
     */
    handleChangeLabel(value) {
      if (!this.toDoDetails.label) this.toDoDetails.label = []
      this.toDoDetails.label.push(value)
    },
    /**
     * 删除待办标签
     */
    handleDeleteLabel(tag) {
      this.toDoDetails.label.splice(this.toDoDetails.label.indexOf(tag), 1)
    }
  }
}
</script>

<style lang="scss" scoped>
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
</style>
