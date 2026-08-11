<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021/12/30 17:43
 * @Description: 笔记 - Modern UI/UX Refactored
-->
<template>
  <div class="note-page">
    <el-tabs v-model="activeTab" class="note-tabs">
      <el-tab-pane label="我的笔记" name="note">
        <div class="note-container">
    <!-- 左侧目录面板 -->
    <aside class="sidebar">
      <!-- 搜索栏 -->
      <div class="search-wrapper">
        <div class="back-btn" @click="handleBack">
          <svg-icon icon-class="back" />
        </div>
        <div class="search-box">
          <svg-icon icon-class="搜索" class="search-icon" />
          <input 
            v-model="searchCode" 
            placeholder="搜索笔记..." 
            @input="handleSearch"
            class="search-input"
          />
        </div>
      </div>

      <!-- 面包屑导航 -->
      <div class="breadcrumb-nav">
        <div 
          v-for="(item, index) in params" 
          :key="index"
          class="breadcrumb-item"
        >
          <svg-icon v-if="index > 0" icon-class="右箭头" class="separator" />
          <span 
            class="breadcrumb-text" 
            :class="{ active: index === params.length - 1 }"
            @click="handleBreadcrumbItem(index)"
          >
            {{ item.breadcrumb }}
          </span>
        </div>
      </div>

      <!-- 文件列表 -->
      <div 
        class="file-list" 
        v-loading="listLoading"
        @contextmenu.prevent.stop="handleEditNote($event, 'empty')"
      >
        <div v-if="list.length < 1" class="empty-state">
          <svg-icon icon-class="文件夹" class="empty-icon" />
          <p>暂无内容</p>
          <p class="hint">右键创建新笔记或文件夹</p>
        </div>
        
        <transition-group name="file-list" tag="div" v-else class="file-items">
          <div
            v-for="(one, index) in list"
            :key="one.id"
            class="file-card"
            :class="{ active: one.id === active.id }"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @contextmenu.prevent.stop="handleEditNote($event, one)"
          >
            <!-- 文件夹 -->
            <div 
              v-if="one.type === 'folder'"
              class="folder-item"
              :draggable="true"
              @dragstart="drag($event, one)"
              @dragover.prevent
              @drop="drop($event, one)"
              @click="handleOpen(one, false)"
              @dblclick="handleOpen(one, true)"
            >
              <div class="folder-icon">
                <svg-icon icon-class="文件夹" />
              </div>
              <div class="folder-info">
                <div class="folder-name">{{ one.name }}</div>
                <svg-icon v-if="one.password" icon-class="验证码" class="lock-icon" />
              </div>
            </div>

            <!-- 笔记 -->
            <div
              v-if="one.type === 'note'"
              class="note-item"
              :draggable="true"
              @dragstart="drag($event, one)"
              @click="handleOpen(one)"
            >
              <div class="note-header">
                <svg-icon icon-class="编辑02" class="note-icon" />
                <span class="note-title">{{ one.title }}</span>
              </div>
              <div class="note-preview">
                {{ one.content && one.content.replace(regex, '') }}
              </div>
              <div class="note-meta">
                <svg-icon icon-class="时间" class="time-icon" />
                <span>{{ one.updateTime || one.createTime }}</span>
              </div>
            </div>
          </div>
        </transition-group>
      </div>
    </aside>

    <!-- 右侧预览区域 -->
    <main class="preview-area" v-loading="loading">
      <div class="preview-content">
        <!-- 文件夹空状态 -->
        <div v-if="active.type === 'folder'" class="empty-preview">
          <svg-icon icon-class="文件夹" class="preview-empty-icon" />
          <p>选择一个笔记开始编辑</p>
        </div>

        <!-- 笔记编辑区 -->
        <template v-else-if="active.type === 'note' && editor">
          <div class="note-header-bar">
            <input
              v-model="note.title"
              placeholder="输入笔记标题..."
              class="title-input"
            />
            <input
              v-model="note.order"
              placeholder="排序"
              class="order-input"
              type="number"
            />
          </div>
          <div class="editor-wrapper">
            <editor 
              :key="note.id" 
              ref="editor" 
              height="100%" 
              v-model="note.richText"
            />
          </div>
          
          <!-- 浮动保存按钮 -->
          <div class="fab-save" @click="handleSaveNote" title="保存 (自动保存已启用)">
            <svg-icon icon-class="保存" />
          </div>
        </template>
      </div>
    </main>

    <!-- 新增/编辑文件夹对话框 -->
    <el-dialog
      :title="folderForm.id ? '编辑文件夹' : '新建文件夹'"
      v-model="folderVisible"
      width="480px"
      custom-class="modern-dialog"
      :modal-append-to-body="true"
    >
      <el-form 
        ref="folderForm" 
        :rules="folderRules" 
        :model="folderForm" 
        label-position="top"
        class="modern-form"
      >
        <el-form-item label="文件夹名称" prop="name">
          <el-input 
            v-model="folderForm.name" 
            placeholder="请输入文件夹名称"
            autofocus
          />
        </el-form-item>
        <el-form-item label="上级文件夹" prop="parentId">
          <el-cascader
            v-model="folderForm.parentId"
            :options="treeList"
            :show-all-levels="false"
            :props="props"
            placeholder="选择上级文件夹"
            clearable
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input 
            v-model="folderForm.order" 
            placeholder="数字越小越靠前" 
            type="number"
          />
        </el-form-item>
        <el-form-item label="阅读密码">
          <el-input 
            v-model="folderForm.password" 
            placeholder="设置访问密码（可选）"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="folderVisible = false" class="btn-cancel">
            取消
          </el-button>
          <el-button 
            type="primary" 
            @click="handleAddFolder" 
            :loading="folderSaveLoading"
            class="btn-confirm"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 移动笔记对话框 -->
    <el-dialog
      title="移动到"
      v-model="moveNoteVisible"
      width="480px"
      custom-class="modern-dialog"
    >
      <div class="move-source">
        <svg-icon 
          :icon-class="source.type === 'folder' ? '文件夹' : '编辑02'" 
          class="source-icon"
        />
        <span>{{ source.name || source.title }}</span>
        <svg-icon v-if="source.password" icon-class="验证码" class="lock-icon" />
      </div>
      <div class="move-target">
        <p class="move-label">选择目标文件夹：</p>
        <el-cascader-panel 
          :options="treeList"
          @change="handleChangMoveTarget"
          :props="props"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="moveNoteVisible = false" class="btn-cancel">
            取消
          </el-button>
          <el-button 
            type="primary" 
            @click="handleMoveNote" 
            :loading="moveNoteLoading"
            class="btn-confirm"
          >
            移动
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 右键菜单 -->
    <transition name="context-menu">
      <div 
        v-if="rightFlag" 
        class="context-menu"
        :style="rightStyle"
        v-clickOutSide="handleCloseEditNote"
      >
        <div
          v-for="item in rightFunctions"
          :key="item.id"
          class="menu-item"
          @click="handleRightClick(item)"
        >
          <svg-icon :icon-class="item.icon" class="menu-icon" />
          <span>{{ item.name }}</span>
        </div>
      </div>
    </transition>
        </div>
      </el-tab-pane>
      <el-tab-pane label="Obsidian 笔记" name="obsidian">
        <file-manager v-if="activeTab === 'obsidian'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import {
  addFolder,
  addNote,
  delFolder,
  delNote,
  getFolder,
  getNote,
  listFolder,
  treeList,
  updateFolder,
  updateNote
} from '@/api/px/life/note'
import FileManager from '@/views/system/fileManager/index.vue'

export default {
  name: 'Note',
  components: { FileManager },
  data() {
    return {
      // 当前激活的 tab
      activeTab: 'note',
      // 右键窗口样式
      rightStyle: '',
      // 右键窗口标志
      rightFlag: false,
      // 右键功能
      rightFunctions: [],
      // 移动笔记弹框标志
      moveNoteVisible: false,
      // 移动笔记loading标志
      moveNoteLoading: false,
      // 来源
      source: {},
      // 目标
      target: undefined,
      // 文件列表加载标志
      listLoading: false,
      // 文件夹弹框标志
      folderVisible: false,
      // 文件夹保存按钮
      folderSaveLoading: false,
      // 笔记获取标志
      loading: false,
      // 文件夹表单
      folderForm: {},
      // 文件夹规则
      folderRules: {
        name: [
          { required: true, message: '请输入文件夹名称', trigger: 'blur' }
        ]
      },
      // 树形文件夹
      treeList: [],
      // 树形对应
      props: {
        checkStrictly: true,
        value: 'id',
        label: 'name'
      },
      // 去除html正则
      regex: /(<([^>]+)>)/ig,
      // 搜索关键字
      searchCode: '',
      // 文件夹列表
      list: [],
      // 笔记
      note: {
        title: '',
        content: '',
        richText: ''
      },
      // 查询列表参数
      params: this.getLocal('noteFolder') || [{ parentId: 0, breadcrumb: '我的笔记' }],
      // 选择ID
      active: {},
      // 搜索延时任务
      searchTimer: undefined,
      // 拖动目标对象
      dragObject: {},
      // 右键选中对象
      rightObject: {},
      // 首次获取
      firstGet: true,
      // 编辑器
      editor: true,
      // 笔记缓存
      noteCache: undefined
    }
  },
  mounted() {
    if (this.$route.query.noteId) {
      this.params = [{ parentId: 0, breadcrumb: '我的笔记' }]
      this.handleOpenNote(this.$route.query.noteId)
    } else {
      this.getFolderList()
    }
  },
  watch: {
    params(newDate) {
      this.setLocal('noteFolder', newDate)
    },
    note: {
      handler(newValue) {
        if (this.firstGet || JSON.stringify(this.noteCache) === JSON.stringify(newValue)) return
        this.$debounce(() => {
          this.handleSaveNote()
        }, 2000)()
      },
      deep: true
    }
  },
  methods: {
    /**
     * 点击面包屑
     */
    handleBreadcrumbItem(index) {
      this.params = this.params.slice(0, index + 1)
      this.getFolderList()
    },
    /**
     * 关闭右键弹框
     */
    handleCloseEditNote() {
      this.rightFlag = false
    },
    /**
     * 点击右键功能
     */
    handleRightClick(item) {
      switch (item.id) {
        case 1:
          this.handleAdd(false)
          break
        case 2:
          this.handleAdd(true)
          break
        case 3:
          this.handleEdit(this.rightObject)
          break
        case 4:
          this.loading = true
          this.$nextTick(() => {
            this.loading = false
          })
          break
        case 5:
          this.source = this.rightObject
          this.moveNoteVisible = true
          break
        case 6:
          this.handleDeleteFolder(this.rightObject)
          break
        case 7:
          this.handleDeleteNote(this.rightObject)
          break
      }
      this.rightFlag = false
    },
    /**
     * 移动笔记或文件夹
     */
    handleMoveNote() {
      if (!this.target) {
        this.$notify.warning('请选择移动目标文件夹')
      } else if (this.source.folder === this.target) {
        this.$notify.warning('请勿选择原来的文件夹')
      } else {
        this.moveNoteLoading = true
        if (this.source.type === 'folder') {
          this.source.parentId = this.target
          updateFolder(this.source).then(res => {
            this.moveNoteLoading = false
            this.moveNoteVisible = false
            this.getFolderList()
          })
        } else if (this.source.type === 'note') {
          this.source.folder = this.target
          updateNote(this.source).then(res => {
            this.moveNoteLoading = false
            this.moveNoteVisible = false
            this.getFolderList(res.data)
          })
        }
      }
    },
    /**
     * 选中移动目标
     */
    handleChangMoveTarget(target) {
      this.target = target[target.length - 1]
    },
    /**
     * 右键编辑笔记
     */
    handleEditNote(event, note) {
      if (note === 'empty') {
        this.rightFunctions = [
          { id: 1, name: '新建笔记', icon: '编辑02' },
          { id: 2, name: '新建文件夹', icon: '文件夹' }
        ]
      } else {
        this.active = note
        if (note.type === 'folder') {
          this.rightFunctions = [
            { id: 1, name: '新建笔记', icon: '编辑02' },
            { id: 2, name: '新建文件夹', icon: '文件夹' },
            { id: 3, name: '编辑', icon: '编辑' },
            { id: 5, name: '移动到', icon: '移动' },
            { id: 6, name: '删除', icon: '删除' }
          ]
        } else if (note.type === 'note') {
          this.rightFunctions = [
            { id: 1, name: '新建笔记', icon: '编辑02' },
            { id: 2, name: '新建文件夹', icon: '文件夹' },
            { id: 4, name: '编辑', icon: '编辑' },
            { id: 5, name: '移动到', icon: '移动' },
            { id: 7, name: '删除', icon: '删除' }
          ]
        }
        this.rightObject = note
      }
      this.rightFlag = true
      this.rightStyle = `top: ${Math.min(event.y, window.innerHeight - this.rightFunctions.length * 48)}px; left: ${Math.min(event.x - 180, window.innerWidth - 200)}px;`
    },
    /**
     * 拖动结束
     */
    drop(event, data) {
      if (data.name) {
        if (this.dragObject.type === 'folder') {
          this.listLoading = true
          this.dragObject.parentId = data.id
          updateFolder(this.dragObject).then(res => {
            this.listLoading = false
            this.getFolderList()
          })
        } else if (this.dragObject.type === 'note') {
          this.listLoading = true
          this.dragObject.folder = data.id
          updateNote(this.dragObject).then(res => {
            this.listLoading = false
            this.getFolderList()
          })
        }
      }
    },
    /**
     * 拖动开始
     */
    drag(event, data) {
      this.dragObject = data
    },
    /**
     * 搜索
     */
    handleSearch() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = setTimeout(() => {
          if (this.searchCode) {
            this.params.push({ name: this.searchCode })
            this.getFolderList()
          } else {
            this.params = [{ parentId: 0, breadcrumb: '我的笔记' }]
            this.getFolderList()
          }
        }, 500)
      } else {
        this.searchTimer = setTimeout(() => {
          if (this.searchCode) {
            this.params.push({ name: this.searchCode })
            this.getFolderList()
          } else {
            this.params = [{ parentId: 0, breadcrumb: '我的笔记' }]
            this.getFolderList()
          }
        }, 500)
      }
    },
    /**
     * 删除笔记
     */
    handleDeleteNote(note) {
      this.$confirm(`确认删除《${note.title}》笔记?`, '删除', {
        type: 'warning'
      }).then(() => {
        return delNote(note.id)
      }).then(() => {
        this.getFolderList()
        this.$notify.success('删除成功')
      })
    },
    /**
     * 保存笔记
     */
    handleSaveNote() {
      const note = JSON.parse(JSON.stringify(this.note))
      note.content = this.$refs.editor.value
      updateNote(note).then(() => {
        this.noteCache = JSON.parse(JSON.stringify(this.note))
        this.$notify.success('保存成功')
        this.$set(this.list, this.list.findIndex(item => item.id === this.note.id), { type: 'note', ...this.note })
      })
    },
    /**
     * 删除文件夹
     */
    handleDeleteFolder(folder) {
      this.$confirm(`确认删除《${folder.name}》文件夹?`, '删除', {
        type: 'warning'
      }).then(() => {
        return delFolder(folder.id)
      }).then(() => {
        this.getFolderList()
        this.$notify.success('删除成功')
      })
    },
    /**
     * 编辑文件夹
     */
    handleEdit(one) {
      getFolder(one.id).then(res => {
        this.folderForm = res.data
        this.folderVisible = true
      })
    },
    /**
     * 新增文件夹或者文件
     */
    handleAdd(flag) {
      if (flag) {
        this.folderForm = {}
        this.folderForm.parentId = this.params[this.params.length - 1].parentId
        this.folderVisible = true
      } else {
        addNote({
          title: '无标题笔记',
          content: '笔记内容···',
          richText: '笔记内容···',
          folder: this.params[this.params.length - 1].parentId
        }).then(res => {
          this.note = res.data
          this.getFolderList(res.data)
        })
      }
    },
    /**
     * 新增文件夹
     */
    handleAddFolder() {
      this.folderSaveLoading = true
      this.$refs['folderForm'].validate(valid => {
        if (valid) {
          if (Array.isArray(this.folderForm.parentId)) {
            this.folderForm.parentId = this.folderForm.parentId[this.folderForm.parentId.length - 1]
          }
          if (this.folderForm.id) {
            updateFolder(this.folderForm).then(() => {
              this.$notify.success('修改文件夹成功')
              this.folderVisible = false
              this.folderSaveLoading = false
              this.getFolderList()
            })
          } else {
            addFolder(this.folderForm).then(res => {
              this.$notify.success('新增文件夹成功')
              this.folderVisible = false
              this.folderSaveLoading = false
              this.getFolderList()
            })
          }
        }
      })
    },
    /**
     * 返回上级
     */
    handleBack() {
      if (this.params.length > 1) {
        this.params.pop()
        this.getFolderList()
      } else {
        this.params = [{ parentId: 0, breadcrumb: '我的笔记' }]
        this.getFolderList()
      }
    },
    /**
     * 打开文件或文件夹
     */
    handleOpen(one, clickFlag) {
      if (!clickFlag && this.active.id === one.id) return
      this.firstGet = true
      this.active = one
      if (one.type === 'folder') {
        if (clickFlag) {
          if (one.password) {
            this.$prompt('请输入阅读密码', '提示', {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              inputPattern: /\S/,
              inputValidator: (value) => {
                return one.password === value
              },
              inputErrorMessage: '阅读密码不正确'
            }).then(({ value }) => {
              this.params.push({ parentId: one.id, breadcrumb: one.name })
              this.getFolderList()
            }).catch(() => {})
          } else {
            this.params.push({ parentId: one.id, breadcrumb: one.name })
            this.getFolderList()
          }
        }
      } else if (one.type === 'note') {
        this.handleOpenNote(one.id)
      }
    },
    /**
     * 获取文件目录
     */
    getFolderList(data) {
      this.listLoading = true
      listFolder(this.params[this.params.length - 1]).then(res => {
        this.list = res.rows.map(item => {
          if (item.title) {
            item.type = 'note'
          } else {
            item.type = 'folder'
          }
          return item
        })
        if (this.list[0]) {
          if (data) {
            this.active = data
          } else {
            this.active = this.list[0]
          }
          if (this.active.type === 'note') {
            this.loading = true
            this.handleOpenNote(this.list[0].id)
          } else if (this.active.type === 'folder') {
            this.note = {}
          }
        } else {
          this.note = {}
        }
        this.listLoading = false
      })
      treeList().then(res => {
        this.treeList = res.data
      })
    },
    /**
     * 打开笔记
     */
    handleOpenNote(id) {
      this.firstGet = true
      this.loading = true
      this.editor = false
      getNote(id).then(res => {
        this.note = res.data
        this.noteCache = JSON.parse(JSON.stringify(res.data))
        this.loading = false
        this.editor = true
        setTimeout(() => this.firstGet = false, 1000)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
// ==================== 容器 ====================

.note-page {
  height: calc(100vh - 84px);
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.note-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__content) {
    flex: 1;
    overflow: hidden;
  }
}

.note-container {
  display: flex;
  height: 100%;
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
    display: flex;
    align-items: center;

    .search-icon {
      position: absolute;
      left: 14px;
      font-size: var(--text-base);
      color: var(--text-tertiary);
      pointer-events: none;
    }

    .search-input {
      width: 100%;
      height: 40px;
      padding: 0 var(--space-4) 0 42px;
      border: none;
      border-radius: var(--radius-md);
      background: var(--bg-hover);
      font-size: var(--text-sm);
      color: var(--text-primary);
      box-shadow: var(--shadow-sm);
      transition: all var(--duration-normal) var(--ease-default);

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

// 面包屑导航
.breadcrumb-nav {
  padding: var(--space-4) var(--space-5);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  border-bottom: 1px solid var(--border-primary);
  min-height: 52px;

  .breadcrumb-item {
    display: flex;
    align-items: center;
    gap: var(--space-2);

    .separator {
      font-size: var(--text-xs);
      color: var(--text-tertiary);
    }

    .breadcrumb-text {
      font-size: var(--text-sm);
      color: var(--text-secondary);
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 6px;
      transition: all var(--duration-normal) var(--ease-default);

      &:hover {
        background: var(--bg-hover);
        color: var(--color-primary);
      }

      &.active {
        color: var(--color-primary);
        font-weight: var(--font-semibold);
      }
    }
  }
}

// 文件列表
.file-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);

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

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
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

  .file-items {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }
}

// 文件卡片
.file-card {
  animation: fadeSlideIn 0.4s var(--ease-default) forwards;
  opacity: 0;

  &.active {
    .folder-item, .note-item {
      background: var(--bg-hover);
      border-left: 3px solid var(--color-primary);
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

// 文件夹项
.folder-item {
  display: flex;
  align-items: center;
  padding: var(--space-4);
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

  .folder-icon {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #ffd89b 0%, #f2994a 100%);
    border-radius: var(--radius-sm);
    margin-right: var(--space-3);

    .svg-icon {
      font-size: var(--text-lg);
      color: white;
    }
  }

  .folder-info {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .folder-name {
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }

    .lock-icon {
      font-size: var(--text-sm);
      color: var(--text-tertiary);
    }
  }
}

// 笔记项
.note-item {
  padding: var(--space-4);
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

  .note-header {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-2);

    .note-icon {
      font-size: var(--text-base);
      color: var(--color-primary);
    }

    .note-title {
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .note-preview {
    font-size: var(--text-sm);
    color: var(--text-secondary);
    line-height: 1.5;
    margin-bottom: var(--space-2);
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }

  .note-meta {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: var(--text-xs);
    color: var(--text-tertiary);

    .time-icon {
      font-size: var(--text-xs);
    }
  }
}

// 右侧预览区域
.preview-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.empty-preview {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);

  .preview-empty-icon {
    font-size: 80px;
    opacity: 0.2;
    margin-bottom: var(--space-5);
  }

  p {
    font-size: var(--text-base);
  }
}

.note-header-bar {
  display: flex;
  gap: var(--space-4);
  padding: 24px 32px var(--space-4);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-primary);

  .title-input {
    flex: 1;
    font-size: var(--text-2xl);
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

  .order-input {
    width: 80px;
    font-size: var(--text-sm);
    color: var(--text-secondary);
    border: none;
    background: var(--bg-hover);
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-sm);
    text-align: center;

    &:focus {
      outline: none;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }
}

.editor-wrapper {
  flex: 1;
  padding: var(--space-4) 32px 32px;
  background: var(--bg-card);
  overflow: hidden;
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

// 文件列表动画
.file-list-enter-active,
.file-list-leave-active {
  transition: all var(--duration-normal) var(--ease-default);
}

.file-list-enter,
.file-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

// 对话框样式覆盖
::v-deep .modern-dialog {
  border-radius: var(--radius-lg) !important;
  overflow: hidden;
  box-shadow: var(--shadow-lg) !important;

  .el-dialog__header {
    padding: var(--space-5) 24px var(--space-4);
    border-bottom: 1px solid var(--border-primary);
    background: var(--bg-card);

    .el-dialog__title {
      font-size: var(--text-lg);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    padding: var(--space-4) 24px var(--space-5);
    border-top: 1px solid var(--border-primary);
    background: var(--bg-body);
  }
}

// 表单样式
.modern-form {
  ::v-deep .el-form-item {
    margin-bottom: var(--space-5);

    .el-form-item__label {
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--text-secondary);
      padding-bottom: var(--space-2);
    }

    .el-input__inner {
      border-radius: var(--radius-sm);
      border-color: var(--border-primary);
      transition: all var(--duration-normal) var(--ease-default);

      &:focus {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
      }
    }

    .el-cascader {
      width: 100%;
    }
  }
}

// 对话框按钮
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);

  .btn-cancel {
    border-radius: var(--radius-sm);
    padding: 10px 20px;
    transition: all var(--duration-normal) var(--ease-default);

    &:hover {
      background: var(--bg-hover);
    }
  }

  .btn-confirm {
    border-radius: var(--radius-sm);
    padding: 10px 24px;
    background: var(--color-primary);
    border: none;
    transition: all var(--duration-normal) var(--ease-default);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }
}

// 移动对话框
.move-source {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);

  .source-icon {
    font-size: var(--text-xl);
    color: var(--color-primary);
  }

  span {
    flex: 1;
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--text-primary);
  }

  .lock-icon {
    font-size: var(--text-sm);
    color: var(--text-tertiary);
  }
}

.move-target {
  .move-label {
    font-size: var(--text-sm);
    color: var(--text-secondary);
    margin-bottom: var(--space-3);
  }

  ::v-deep .el-cascader-panel {
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-sm);
    overflow: hidden;
  }
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
</style>
