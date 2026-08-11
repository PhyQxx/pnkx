<template>
  <div class="file-manager-container">
    <!-- 左侧：文件树 -->
    <div class="file-tree">
      <div class="tree-header">
        <span>📁 文件管理器</span>
        <el-dropdown trigger="click" size="small">
          <el-button size="small" type="primary">+ 新建</el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item @click.native="showCreateFileDialog()">📄 新建文件</el-dropdown-item>
            <el-dropdown-item @click.native="showMkdirDialog()">📁 新建目录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-button size="small" icon="Search" title="搜索" @click="showSearch = !showSearch" style="margin-left:4px"/>
      </div>

      <!-- 搜索框 -->
      <div v-if="showSearch" class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="搜索文件名或内容..."
          size="small"
          clearable
          @keyup.enter.native="doSearch"
        >
          <el-button slot="append" icon="Search" @click="doSearch"/>
        </el-input>
      </div>

      <!-- 搜索结果 -->
      <div v-if="searchResults.length > 0" class="search-results">
        <div class="search-results-header">
          <span>搜索结果 ({{ searchResults.length }})</span>
          <el-button size="small" type="text" @click="clearSearch">清除</el-button>
        </div>
        <div
          v-for="item in searchResults"
          :key="item.path"
          class="search-result-item"
          @click="openFile(item.path)"
        >
          <el-icon><component :is="item.isDirectory ? 'Folder' : 'Document'" /></el-icon>
          <span>{{ item.name }}</span>
          <span class="match-indicator" v-if="item.matchedContent">内容匹配</span>
        </div>
      </div>

      <!-- 面包屑 -->
      <div class="breadcrumb" v-if="!showSearch || searchResults.length === 0">
        <span class="crumb-root" @click="navigateTo('')">根目录</span>
        <template v-for="(crumb, i) in breadcrumbs" :key="i">
          <span class="crumb-sep"> / </span>
          <span class="crumb-item" @click="navigateTo(crumb.path)">{{ crumb.name }}</span>
        </template>
      </div>

      <!-- 文件列表 -->
      <div class="file-list" v-if="!showSearch || searchResults.length === 0">
        <div v-if="loadingFiles" class="loading-state">
          <el-icon><Loading /></el-icon> 加载中...
        </div>
        <div v-else-if="fileList.length === 0" class="empty-state">
          <p>📂 空目录</p>
        </div>
        <div
          v-for="item in fileList"
          :key="item.path"
          class="file-item"
          :class="{ active: currentFile === item.path }"
          @click="onFileClick(item)"
          @contextmenu.prevent="showContextMenu($event, item)"
        >
          <el-icon><component :is="item.isDirectory ? 'Folder' : getFileIcon(item.extension)" /></el-icon>
          <span class="file-name">{{ item.name }}</span>
          <span class="file-size" v-if="!item.isDirectory">{{ formatSize(item.size) }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧：编辑器 -->
    <div class="file-editor">
      <div class="editor-toolbar" v-if="currentFile">
        <div class="editor-title">
          <el-icon><component :is="currentIsDirectory ? 'Folder' : getFileIcon(currentExtension)" /></el-icon>
          {{ currentFileName }}
        </div>
        <div class="editor-actions">
          <el-button size="small" type="text" v-if="isModified" style="color:#e6a23c">⚠ 未保存</el-button>
          <el-button size="small" type="primary" @click="saveFile" :loading="saving">💾 保存</el-button>
          <el-button size="small" type="text" @click="renameFile">✏ 重命名</el-button>
          <el-button size="small" type="text" style="color:#f56c6c" @click="deleteCurrentFile">🗑 删除</el-button>
        </div>
      </div>

      <!-- Markdown 编辑器：Cherry Markdown -->
      <div v-if="currentFile && isMarkdown" class="markdown-editor">
        <CherryMarkdownEditor
          ref="cherryMarkdownEditor"
          height="100%"
          v-model="fileContent"
          @input="onContentChange"
        />
      </div>

      <!-- 图片预览 -->
      <div v-else-if="currentFile && isImage" class="image-preview">
        <img :src="imageUrl" :alt="currentFileName" />
      </div>

      <!-- 纯文本/其他文件 -->
      <div v-else-if="currentFile" class="text-preview">
        <textarea
          v-model="fileContent"
          class="plain-textarea"
          @input="onContentChange"
          readonly
        ></textarea>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">📂</div>
        <p>👈 从左侧选择一个文件开始编辑</p>
        <p class="empty-tip">支持 .md .txt .png .jpg .gif .pdf 文件</p>
      </div>
    </div>

    <!-- 新建文件对话框 -->
    <el-dialog title="新建文件" v-model="createFileDialogVisible" width="400px">
      <el-form :model="createFileForm" ref="createFileForm" label-width="80px">
        <el-form-item label="文件名" prop="name">
          <el-input v-model="createFileForm.name" placeholder="例如：我的笔记.md" />
        </el-form-item>
        <el-form-item label="目录" prop="path">
          <el-input v-model="createFileForm.path" placeholder="留空则在当前目录创建" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="createFileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreateFile" :loading="creating">创建</el-button>
      </span>
    </el-dialog>

    <!-- 新建目录对话框 -->
    <el-dialog title="新建目录" v-model="mkdirDialogVisible" width="400px">
      <el-form :model="mkdirForm" ref="mkdirForm" label-width="80px">
        <el-form-item label="目录名" prop="name">
          <el-input v-model="mkdirForm.name" placeholder="目录名称" />
        </el-form-item>
        <el-form-item label="父目录" prop="path">
          <el-input v-model="mkdirForm.path" placeholder="留空则在根目录创建" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="mkdirDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doMkdir" :loading="creating">创建</el-button>
      </span>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog title="重命名" v-model="renameDialogVisible" width="400px">
      <el-form :model="renameForm" ref="renameForm" label-width="80px">
        <el-form-item label="新名称" prop="newName">
          <el-input v-model="renameForm.newName" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doRename">确定</el-button>
      </span>
    </el-dialog>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
      @click.stop
    >
      <div class="context-item" @click="openFile(contextMenu.item.path)">📂 打开</div>
      <div class="context-item" v-if="!contextMenu.item.isDirectory" @click="startEdit(contextMenu.item)">✏ 编辑</div>
      <div class="context-item" @click="startRename(contextMenu.item)">🔤 重命名</div>
      <div class="context-item context-item-danger" @click="deleteItem(contextMenu.item)">🗑 删除</div>
    </div>
  </div>
</template>

<script>
import { listFiles, readFile, writeFile, createFile, deleteFile, moveFile, mkdirFile, searchFiles } from '@/api/system/fileManager'
import CherryMarkdownEditor from '@/components/CherryMarkdownEditor'

export default {
  name: 'FileManager',
  components: {
    CherryMarkdownEditor
  },
  data() {
    return {
      // 文件列表
      fileList: [],
      currentPath: '',
      currentFile: null,
      currentIsDirectory: false,
      currentExtension: '',
      currentFileName: '',
      fileContent: '',
      originalContent: '',
      imageUrl: '',
      saving: false,
      loadingFiles: false,
      isModified: false,

      // 搜索
      showSearch: false,
      searchQuery: '',
      searchResults: [],

      // 新建文件
      createFileDialogVisible: false,
      createFileForm: { name: '', path: '' },
      creating: false,

      // 新建目录
      mkdirDialogVisible: false,
      mkdirForm: { name: '', path: '' },

      // 重命名
      renameDialogVisible: false,
      renameForm: { newName: '' },

      // 右键菜单
      contextMenu: {
        visible: false,
        x: 0,
        y: 0,
        item: {}
      }
    }
  },
  computed: {
    breadcrumbs() {
      if (!this.currentPath) return []
      const parts = this.currentPath.split('/').filter(Boolean)
      const crumbs = []
      let path = ''
      for (const part of parts) {
        path += part + '/'
        crumbs.push({ name: part, path: path.replace(/\/$/, '') })
      }
      return crumbs
    },
    isMarkdown() {
      const ext = this.currentExtension.toLowerCase()
      return ext === '.md' || ext === '.txt'
    },
    isImage() {
      const ext = this.currentExtension.toLowerCase()
      return ['.png', '.jpg', '.jpeg', '.gif'].includes(ext)
    },

  },
  mounted() {
    // 恢复上次访问的路径
    const savedPath = localStorage.getItem('fileManager:currentPath') || ''
    if (savedPath) {
      this.navigateTo(savedPath, true)
    } else {
      this.loadFiles()
    }
    // 点击其他地方关闭右键菜单
    document.addEventListener('click', () => { this.contextMenu.visible = false })
  },
  // beforeUnmount钩子已不需要，因为使用组件生命周期管理
  methods: {
    getFileIcon(extension) {
      const ext = (extension || '').toLowerCase()
      if (['.md', '.txt'].includes(ext)) return 'Document'
      if (['.png', '.jpg', '.jpeg', '.gif'].includes(ext)) return 'Picture'
      if (['.pdf'].includes(ext)) return 'CopyDocument'
      return 'Document'
    },
    formatSize(bytes) {
      if (!bytes) return ''
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },
    async loadFiles(path) {
      this.loadingFiles = true
      try {
        const res = await listFiles(path || this.currentPath)
        if (res.code === 200) {
          this.fileList = res.data || []
        }
      } catch (e) {
        this.$message.error('加载文件列表失败')
      } finally {
        this.loadingFiles = false
      }
    },
    navigateTo(crumbPath, skipCache = false) {
      this.currentPath = crumbPath
      this.currentFile = null
      this.currentIsDirectory = false
      this.fileContent = ''
      this.originalContent = ''
      this.isModified = false
      if (!skipCache) {
        localStorage.setItem('fileManager:currentPath', crumbPath)
      }
      this.loadFiles()
    },
    onFileClick(item) {
      if (item.isDirectory) {
        this.navigateTo(item.path)
      } else {
        this.openFile(item.path)
      }
    },
    async openFile(path) {
      this.contextMenu.visible = false
      // 如果有未保存的修改，先提示
      if (this.isModified) {
        await this.$confirm('当前文件有未保存的修改，是否保存？', '提示', {
          confirmButtonText: '保存',
          cancelButtonText: '不保存',
          type: 'warning'
        }).then(() => this.saveFile()).catch(() => {})
      }
      try {
        const res = await readFile(path)
        if (res.code === 200) {
          this.currentFile = path
          this.currentIsDirectory = false
          this.currentFileName = path.split('/').pop()
          this.currentExtension = this.getExtension(this.currentFileName)
          if (res.data.isBase64) {
            this.imageUrl = `data:${res.data.mimeType};base64,${res.data.content}`
            this.fileContent = ''
          } else {
            this.fileContent = res.data.content || ''
            this.originalContent = this.fileContent
          }
          this.isModified = false
        } else {
          this.$message.error(res.msg || '读取文件失败')
        }
      } catch (e) {
        this.$message.error('读取文件失败')
      }
    },
    getExtension(fileName) {
      const idx = fileName.lastIndexOf('.')
      return idx > 0 ? fileName.substring(idx) : ''
    },
    onContentChange() {
      this.isModified = this.fileContent !== this.originalContent
    },

    async saveFile() {
      if (!this.currentFile) return
      this.saving = true
      try {
        const content = (this.isMarkdown && this.$refs.cherryMarkdownEditor)
          ? this.$refs.cherryMarkdownEditor.getCherryContent()
          : this.fileContent
        const res = await writeFile({ path: this.currentFile, content })
        if (res.code === 200) {
          this.fileContent = content
          this.originalContent = content
          this.isModified = false
          this.$message.success('保存成功')
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      } catch (e) {
        this.$message.error('保存失败')
      } finally {
        this.saving = false
      }
    },
    showCreateFileDialog() {
      this.createFileForm = { name: '', path: this.currentPath }
      this.createFileDialogVisible = true
    },
    async doCreateFile() {
      const { name, path } = this.createFileForm
      if (!name) {
        this.$message.warning('文件名不能为空')
        return
      }
      this.creating = true
      try {
        const res = await createFile({ name, path, content: '' })
        if (res.code === 200) {
          this.$message.success('创建成功')
          this.createFileDialogVisible = false
          this.loadFiles()
          // 打开新文件
          if (res.data) {
            this.openFile(res.data)
          }
        } else {
          this.$message.error(res.msg || '创建失败')
        }
      } catch (e) {
        this.$message.error('创建失败')
      } finally {
        this.creating = false
      }
    },
    showMkdirDialog() {
      this.mkdirForm = { name: '', path: this.currentPath }
      this.mkdirDialogVisible = true
    },
    async doMkdir() {
      if (!this.mkdirForm.name) {
        this.$message.warning('目录名不能为空')
        return
      }
      this.creating = true
      try {
        const res = await mkdirFile(this.mkdirForm)
        if (res.code === 200) {
          this.$message.success('创建成功')
          this.mkdirDialogVisible = false
          this.loadFiles()
        } else {
          this.$message.error(res.msg || '创建失败')
        }
      } catch (e) {
        this.$message.error('创建失败')
      } finally {
        this.creating = false
      }
    },
    startRename(item) {
      this.contextMenu.visible = false
      this.renameForm.newName = item.name
      this.renameItem = item
      this.renameDialogVisible = true
    },
    renameFile() {
      if (!this.currentFile) return
      this.renameForm.newName = this.currentFileName
      this.renameItem = { path: this.currentFile, name: this.currentFileName }
      this.renameDialogVisible = true
    },
    async doRename() {
      const newName = this.renameForm.newName
      if (!newName) return
      const oldPath = this.renameItem.path
      const parentPath = oldPath.substring(0, oldPath.lastIndexOf('/'))
      const newPath = parentPath ? parentPath + '/' + newName : newName
      try {
        const res = await moveFile({ oldPath, newPath })
        if (res.code === 200) {
          this.$message.success('重命名成功')
          this.renameDialogVisible = false
          if (this.currentFile === oldPath) {
            this.currentFile = newPath
            this.currentFileName = newName
          }
          this.loadFiles()
        } else {
          this.$message.error(res.msg || '重命名失败')
        }
      } catch (e) {
        this.$message.error('重命名失败')
      }
    },
    deleteCurrentFile() {
      if (!this.currentFile) return
      this.deleteItem({ path: this.currentFile, name: this.currentFileName, isDirectory: false })
    },
    async deleteItem(item) {
      this.contextMenu.visible = false
      await this.$confirm(`确定要删除 "${item.name}" 吗？`, '删除确认', {
        type: 'warning'
      })
      try {
        const res = await deleteFile(item.path)
        if (res.code === 200) {
          this.$message.success('删除成功')
          if (this.currentFile === item.path) {
            this.currentFile = null
            this.fileContent = ''
            this.isModified = false
          }
          this.loadFiles()
        } else {
          this.$message.error(res.msg || '删除失败')
        }
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败')
      }
    },
    showContextMenu(event, item) {
      this.contextMenu = {
        visible: true,
        x: event.clientX,
        y: event.clientY,
        item
      }
    },
    startEdit(item) {
      this.contextMenu.visible = false
      if (!item.isDirectory) {
        this.openFile(item.path)
      }
    },
    async doSearch() {
      if (!this.searchQuery.trim()) return
      try {
        const res = await searchFiles(this.searchQuery)
        if (res.code === 200) {
          this.searchResults = res.data || []
        }
      } catch (e) {
        this.$message.error('搜索失败')
      }
    },
    clearSearch() {
      this.searchQuery = ''
      this.searchResults = []
      this.showSearch = false
    }
  }
}
</script>

<style lang="scss" scoped>
.file-manager-container {
  display: flex;
  height: 100%;
  overflow: hidden;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.file-tree {
  width: 280px;
  min-width: 280px;
  border-right: 1px solid var(--border-primary);
  background: var(--bg-body);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: var(--radius-lg) 0 0 var(--radius-lg);
}

.tree-header {
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--border-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: var(--font-semibold);
  font-size: var(--text-sm);
  color: var(--text-primary);
  flex-shrink: 0;
}

.search-box {
  padding: var(--space-2) var(--space-3);
  border-bottom: 1px solid var(--border-primary);
  flex-shrink: 0;
}

.search-results {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2) 0;
}

.search-results-header {
  padding: var(--space-1) var(--space-4) var(--space-2);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  display: flex;
  justify-content: space-between;
}

.search-result-item {
  padding: var(--space-2) var(--space-4);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  transition: background-color var(--duration-fast) var(--ease-default);
}

.search-result-item:hover {
  background: var(--bg-hover);
}

.match-indicator {
  font-size: var(--text-xs);
  background: var(--bg-hover);
  color: var(--text-tertiary);
  padding: 1px var(--space-2);
  border-radius: var(--radius-sm);
  margin-left: auto;
}

.breadcrumb {
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  border-bottom: 1px solid var(--border-primary);
  flex-shrink: 0;
}

.crumb-root,
.crumb-item {
  cursor: pointer;
  color: var(--color-primary);
  transition: opacity var(--duration-fast) var(--ease-default);

  &:hover {
    opacity: 0.8;
  }
}

.crumb-sep {
  color: var(--border-primary);
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2) 0;
}

.file-item {
  padding: var(--space-2) var(--space-4);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  transition: background-color var(--duration-fast) var(--ease-default),
              color var(--duration-fast) var(--ease-default);
}

.file-item:hover {
  background: var(--bg-hover);
}

.file-item.active {
  background: var(--color-primary-600);
  color: var(--color-primary);
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.loading-state,
.empty-state {
  padding: var(--space-6);
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.file-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-toolbar {
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--border-primary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  background: var(--bg-card);
}

.editor-title {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.markdown-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.image-preview {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  overflow: auto;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  box-shadow: var(--shadow-md);
  border-radius: var(--radius-md);
}

.text-preview {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.plain-textarea {
  flex: 1;
  border: none;
  resize: none;
  padding: var(--space-4);
  font-family: 'Courier New', monospace;
  font-size: var(--text-sm);
  overflow-y: auto;
  background: var(--bg-body);
  color: var(--text-primary);
  outline: none;
  line-height: 1.6;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  color: var(--text-tertiary);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: var(--space-4);
}

.empty-tip {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-2);
}

.context-menu {
  position: fixed;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 9999;
  min-width: 140px;
  padding: var(--space-1) 0;
}

.context-item {
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.context-item:hover {
  background: var(--bg-hover);
}

.context-item-danger {
  color: var(--color-primary-600);
}
</style>
