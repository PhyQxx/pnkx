<!--
 * @File: index
 * @Author: PHY
 * @Date: 2026/4/18
 * @Description: AI模型配置管理
-->
<template>
  <div class="ai-model-container">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div class="header-left">
        <el-icon><Menu /></el-icon>
        <span class="header-title">AI模型配置</span>
      </div>
      <div class="header-right">
        <el-button type="primary" size="small" @click="openAdd">
          <el-icon><Plus /></el-icon> 新增模型
        </el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="page-content">
      <!-- 默认模型提示 -->
      <div v-if="defaultModel" class="default-tip">
        <el-tag type="success" size="default">默认</el-tag>
        <span class="default-name">{{ defaultModel.modelName }}</span>
        <span class="default-key">{{ defaultModel.modelKey }}</span>
        <span class="default-url">{{ defaultModel.baseUrl }}</span>
      </div>

      <!-- 模型列表 -->
      <div v-if="list.length > 0" class="model-grid">
        <div
          v-for="item in list"
          :key="item.id"
          class="model-card"
        >
          <div class="card-header">
            <div class="card-info">
              <div class="model-name">{{ item.modelName }}</div>
              <div class="model-key">{{ item.modelKey }}</div>
            </div>
            <div class="card-tags">
              <el-tag v-if="item.isDefault === '1'" type="success" size="small">默认</el-tag>
              <el-tag v-if="item.isEnabled === '0'" type="warning" size="small" effect="plain">已禁用</el-tag>
            </div>
          </div>
          <div class="card-url">{{ item.baseUrl }}</div>
          <div class="card-footer">
            <el-button size="small" plain @click="handleEdit(item)">编辑</el-button>
            <el-button
              v-if="item.isDefault !== '1'"
              size="small"
              type="primary"
                            @click="handleSetDefault(item)"
            >设为默认</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(item)">删除</el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无配置，点击右上角新增" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="isEdit ? '编辑模型' : '新增模型'"
      v-model="showDialog"
      width="520px"
      :close-on-click-modal="false"
      custom-class="ai-model-dialog"
    >
      <el-form
        ref="form"
        :model="form"
        :rules="formRules"
        label-position="top"
      >
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如：DeepSeek" />
        </el-form-item>
        <el-form-item label="模型标识" prop="modelKey">
          <el-input v-model="form.modelKey" placeholder="如：deepseek-chat" />
        </el-form-item>
        <el-form-item label="API地址" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="如：https://api.deepseek.com" />
        </el-form-item>
        <el-form-item label="API Key" :prop="isEdit ? '' : 'apiKey'">
          <el-input v-model="form.apiKey" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入API Key'" show-password />
        </el-form-item>
        <el-form-item label="温度参数" prop="temperature">
          <el-input-number
            v-model="form.temperature"
            :min="0"
            :max="2"
            :step="0.1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="isDefaultChecked" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="isEnabledChecked" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="isEdit" :loading="testing" @click="handleTest">测试</el-button>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  listAiModel,
  getAiModel,
  addAiModel,
  updateAiModel,
  delAiModel,
  setDefaultAiModel,
  testAiModel
} from '@/api/px/life/aiModel'

export default {
  name: 'AiModel',
  data() {
    return {
      list: [],
      defaultModel: null,
      showDialog: false,
      isEdit: false,
      testing: false,
      isDefaultChecked: false,
      isEnabledChecked: true,
      form: {
        id: null,
        modelName: '',
        modelKey: '',
        baseUrl: '',
        apiKey: '',
        temperature: 0.7
      },
      formRules: {
        modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
        modelKey: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
        baseUrl: [{ required: true, message: '请输入API地址', trigger: 'blur' }],
        apiKey: [{ required: true, message: '请输入API Key', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    getList() {
      listAiModel().then(res => {
        this.list = res.rows || []
        this.defaultModel = this.list.find(i => i.isDefault === '1') || null
      })
    },
    openAdd() {
      this.isEdit = false
      this.form = {
        id: null,
        modelName: '',
        modelKey: '',
        baseUrl: '',
        apiKey: '',
        temperature: 0.7
      }
      this.isDefaultChecked = false
      this.isEnabledChecked = true
      this.showDialog = true
    },
    handleEdit(item) {
      this.isEdit = true
      this.form = {
        id: item.id,
        modelName: item.modelName,
        modelKey: item.modelKey,
        baseUrl: item.baseUrl,
        apiKey: '',
        temperature: item.temperature
      }
      this.isDefaultChecked = item.isDefault === '1'
      this.isEnabledChecked = item.isEnabled === '1'
      this.showDialog = true
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const data = {
          ...this.form,
          isDefault: this.isDefaultChecked ? '1' : '0',
          isEnabled: this.isEnabledChecked ? '1' : '0'
        }
        // 编辑时未填写apiKey则不传，避免用脱敏值覆盖真实密钥
        if (this.isEdit && !data.apiKey) {
          delete data.apiKey
        }
        const action = this.isEdit ? updateAiModel(data) : addAiModel(data)
        action.then(res => {
          this.$notify.success(this.isEdit ? '修改成功' : '新增成功')
          this.showDialog = false
          this.getList()
        })
      })
    },
    handleTest() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const data = { ...this.form }
        if (!data.apiKey) {
          delete data.apiKey
        }
        this.testing = true
        testAiModel(data).then(res => {
          const duration = res.data == null ? '' : `（${res.data}ms）`
          this.$notify.success(`连接测试成功${duration}`)
        }).finally(() => {
          this.testing = false
        })
      })
    },
    handleSetDefault(item) {
      this.$confirm(`确认将「${item.modelName}」设为默认模型？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(() => {
        setDefaultAiModel(item.id).then(res => {
          this.$notify.success('设置成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleDelete(item) {
      this.$confirm(`确认删除「${item.modelName}」？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delAiModel(item.id).then(res => {
          this.$notify.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-model-container {
  padding: 0;
  min-height: calc(100vh - 84px);
  background: var(--bg-body);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-primary);

  .header-left {
    display: flex;
    align-items: center;
    gap: var(--space-2);

    .el-icon-menu {
      font-size: var(--text-lg);
      color: var(--color-primary);
    }

    .header-title {
      font-size: var(--text-base);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }
  }
}

.page-content {
  padding: var(--space-5) var(--space-6);
}

.default-tip {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
  border: 1px solid var(--border-primary);

  .default-name {
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--color-primary);
  }

  .default-key {
    font-size: var(--text-xs);
    color: var(--text-secondary);
  }

  .default-url {
    font-size: var(--text-xs);
    color: var(--text-tertiary);
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: var(--space-4);
}

.model-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: var(--space-4) var(--space-5);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-primary);
  transition: all var(--duration-normal) var(--ease-default);

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: var(--space-3);

    .model-name {
      font-size: var(--text-base);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }

    .model-key {
      font-size: var(--text-xs);
      color: var(--text-secondary);
      margin-top: var(--space-1);
    }

    .card-tags {
      display: flex;
      gap: var(--space-2);
    }
  }

  .card-url {
    font-size: var(--text-xs);
    color: var(--text-secondary);
    margin-bottom: var(--space-3);
    word-break: break-all;
  }

  .card-footer {
    display: flex;
    gap: var(--space-2);
    justify-content: flex-end;
  }
}

.ai-model-dialog {
  .el-input-number {
    width: 100%;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}
</style>
