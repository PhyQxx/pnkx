<!--
 * @File: index
 * @Author: PHY
 * @Description: 壁纸管理（左树右表，壁纸与文件夹合并为一个页面）
-->
<template>
    <div class="app-container wallpaper-page">
        <el-row :gutter="16">
            <!-- 左侧：文件夹树 -->
            <el-col :span="5" class="folder-panel">
                <el-card shadow="never" class="folder-card">
                    <template #header>
                        <div class="folder-header">
                            <span class="folder-title">壁纸文件夹</span>
                            <span class="folder-header-actions">
                                <el-button
                                    type="primary"
                                    icon="Setting"
                                    size="small"
                                    link
                                    title="文件夹管理（启用/停用等）"
                                    @click="goToFolderManage"
                                >文件夹管理
                                </el-button>
                                <el-button
                                    type="primary"
                                    icon="Plus"
                                    size="small"
                                    link
                                    @click="handleFolderAdd()"
                                    v-hasPermi="['wallpaper:folder:add']"
                                >新增
                                </el-button>
                            </span>
                        </div>
                    </template>
                    <el-input
                        v-model="folderKeyword"
                        placeholder="搜索文件夹"
                        clearable
                        size="small"
                        prefix-icon="Search"
                        class="folder-search"
                    />
                    <el-tree
                        ref="folderTreeRef"
                        :data="folderTreeData"
                        :props="treeProps"
                        node-key="key"
                        highlight-current
                        default-expand-all
                        :expand-on-click-node="false"
                        :filter-node-method="filterFolderNode"
                        @node-click="handleNodeClick"
                    >
                        <template #default="{ node, data }">
                        <span class="folder-node">
                            <span class="folder-node-label">
                                <el-icon v-if="data.isRoot"><FolderOpened/></el-icon>
                                <el-icon v-else><Folder/></el-icon>
                                {{ node.label }}
                                <span v-if="data.wallpaperCount != null" class="folder-count">
                                    ({{ data.wallpaperCount }})
                                </span>
                            </span>
                            <span v-if="!data.isRoot" class="folder-node-actions" @click.stop>
                                <el-button
                                    type="primary"
                                    icon="Plus"
                                    size="small"
                                    link
                                    title="新增子文件夹"
                                    @click="handleFolderAdd(data.id)"
                                    v-hasPermi="['wallpaper:folder:add']"
                                />
                                <el-button
                                    type="success"
                                    icon="Edit"
                                    size="small"
                                    link
                                    title="修改"
                                    @click="handleFolderUpdate(data)"
                                    v-hasPermi="['wallpaper:folder:edit']"
                                />
                                <el-button
                                    type="danger"
                                    icon="Delete"
                                    size="small"
                                    link
                                    title="删除"
                                    @click="handleFolderDelete(data)"
                                    v-hasPermi="['wallpaper:folder:remove']"
                                />
                            </span>
                        </span>
                        </template>
                    </el-tree>
                </el-card>
            </el-col>

            <!-- 右侧：壁纸表格 -->
            <el-col :span="19">
                <!-- 查询条件 -->
                <el-form
                    :model="queryParams"
                    ref="queryForm"
                    :inline="true"
                    v-show="showSearch"
                    label-width="68px"
                >
                    <el-form-item label="壁纸名称" prop="name">
                        <el-input
                            v-model="queryParams.name"
                            placeholder="请输入壁纸名称"
                            clearable
                            size="small"
                            @keyup.enter.native="handleQuery"
                        />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
                        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
                    </el-form-item>
                </el-form>

                <!-- 操作按钮 -->
                <el-row :gutter="10" class="mb8">
                    <el-col :span="1.5">
                        <el-button
                            type="primary"
                            icon="Plus"
                            size="small"
                            @click="handleAdd"
                            v-hasPermi="['wallpaper:add']"
                        >新增
                        </el-button>
                    </el-col>
                    <el-col :span="1.5">
                        <el-button
                            type="success"
                            icon="Edit"
                            size="small"
                            :disabled="single"
                            @click="handleUpdate"
                            v-hasPermi="['wallpaper:edit']"
                        >修改
                        </el-button>
                    </el-col>
                    <el-col :span="1.5">
                        <el-button
                            type="danger"
                            icon="Delete"
                            size="small"
                            :disabled="multiple"
                            @click="handleDelete"
                            v-hasPermi="['wallpaper:remove']"
                        >删除
                        </el-button>
                    </el-col>
                    <el-col :span="1.5">
                        <el-button
                            type="warning"
                            icon="Download"
                            size="small"
                            @click="handleExport"
                            v-hasPermi="['wallpaper:export']"
                        >导出
                        </el-button>
                    </el-col>
                    <el-col :span="1.5">
                        <el-button
                            type="info"
                            icon="DataLine"
                            size="small"
                            @click="goToRecord"
                        >操作记录
                        </el-button>
                    </el-col>
                    <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
                </el-row>

                <!-- 表格 -->
                <div class="table-main-area">
                    <el-table
                        v-loading="loading"
                        :data="wallpaperList"
                        @selection-change="handleSelectionChange"
                    >
                        <el-table-column type="selection" width="55" align="center"/>
                        <el-table-column label="预览" align="center" width="100">
                            <template v-slot="scope">
                                <el-image
                                    :src="scope.row.thumbnail || scope.row.url"
                                    :preview-src-list="[scope.row.url]"
                                    fit="cover"
                                    style="width: 60px; height: 100px; border-radius: 4px"
                                    preview-teleported
                                />
                            </template>
                        </el-table-column>
                        <el-table-column label="壁纸名称" align="center" prop="name" :show-overflow-tooltip="true"/>
                        <el-table-column label="点赞数" align="center" prop="likeCount" width="90">
                            <template v-slot="scope">
                                <span class="like-count">
                                    <el-icon><StarFilled/></el-icon>
                                    {{ scope.row.likeCount || 0 }}
                                </span>
                            </template>
                        </el-table-column>
                        <el-table-column label="所属文件夹" align="center" prop="folder">
                            <template v-slot="scope">
                                {{ scope.row.folder ? (folderMap[scope.row.folder] || scope.row.folder) : '未分类' }}
                            </template>
                        </el-table-column>
                        <el-table-column label="尺寸" align="center" width="140">
                            <template v-slot="scope">
                                <span v-if="scope.row.width && scope.row.height">
                                    {{ scope.row.width }} × {{ scope.row.height }}
                                </span>
                                <span v-else style="color: #999">-</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="排序" align="center" prop="order" width="80"/>
                        <el-table-column label="创建时间" align="center" prop="createTime" width="160">
                            <template v-slot="scope">
                                <span>{{ parseTime(scope.row.createTime) }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
                            <template v-slot="scope">
                                <el-button
                                    size="small"
                                    type="text"
                                    icon="Edit"
                                    @click="handleUpdate(scope.row)"
                                    v-hasPermi="['wallpaper:edit']"
                                >修改
                                </el-button>
                                <el-button
                                    size="small"
                                    type="text"
                                    icon="Delete"
                                    @click="handleDelete(scope.row)"
                                    v-hasPermi="['wallpaper:remove']"
                                >删除
                                </el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>

                <pagination
                    v-show="total > queryParams.pageSize"
                    :total="total"
                    v-model:page="queryParams.pageNum"
                    v-model:limit="queryParams.pageSize"
                    @pagination="getList"
                />
            </el-col>
        </el-row>

        <!-- 新增/修改壁纸对话框 -->
        <el-dialog :title="title" v-model="open" width="680px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="100px">
                <el-row>
                    <el-col :span="24">
                        <el-form-item label="壁纸名称" prop="name">
                            <el-input v-model="form.name" placeholder="请输入壁纸名称"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="所属文件夹" prop="folder">
                            <el-tree-select
                                v-model="form.folder"
                                :data="folderSelectData"
                                :props="{ value: 'id', label: 'name', children: 'children' }"
                                value-key="id"
                                placeholder="选择所属文件夹（不选为未分类）"
                                check-strictly
                                clearable
                                style="width: 100%"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="壁纸图片" prop="url">
                            <el-radio-group v-model="form.urlMode" size="small" class="image-mode-group">
                                <el-radio-button label="upload">上传</el-radio-button>
                                <el-radio-button label="url">URL</el-radio-button>
                            </el-radio-group>
                            <imageUpload
                                v-if="form.urlMode === 'upload'"
                                v-model="form.url"
                                image-type="wallpaper"
                            />
                            <el-input
                                v-else
                                v-model="form.url"
                                placeholder="请输入壁纸图片完整地址，如 https://example.com/a.jpg"
                                clearable
                            >
                                <template #prepend>
                                    <el-icon><Link/></el-icon>
                                </template>
                            </el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="缩略图">
                            <el-radio-group v-model="form.thumbnailMode" size="small" class="image-mode-group">
                                <el-radio-button label="upload">上传</el-radio-button>
                                <el-radio-button label="url">URL</el-radio-button>
                            </el-radio-group>
                            <imageUpload
                                v-if="form.thumbnailMode === 'upload'"
                                v-model="form.thumbnail"
                                image-type="wallpaper"
                            />
                            <el-input
                                v-else
                                v-model="form.thumbnail"
                                placeholder="请输入缩略图完整地址（可选）"
                                clearable
                            >
                                <template #prepend>
                                    <el-icon><Link/></el-icon>
                                </template>
                            </el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="宽度(px)" prop="width">
                            <el-input-number v-model="form.width" :min="0" controls-position="right" placeholder="自动"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="高度(px)" prop="height">
                            <el-input-number v-model="form.height" :min="0" controls-position="right" placeholder="自动"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="排序" prop="order">
                            <el-input-number v-model="form.order" :min="0" controls-position="right" placeholder="请输入排序"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="备注" prop="remark">
                            <el-input v-model="form.remark" type="textarea" placeholder="请输入备注"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 新增/修改文件夹对话框 -->
        <el-dialog :title="folderTitle" v-model="folderOpen" width="600px" append-to-body>
            <el-form ref="folderForm" :model="folderForm" :rules="folderRules" label-width="100px">
                <el-row>
                    <el-col :span="24">
                        <el-form-item label="文件夹名称" prop="name">
                            <el-input v-model="folderForm.name" placeholder="请输入文件夹名称"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="父级文件夹" prop="parentId">
                            <el-tree-select
                                v-model="folderForm.parentId"
                                :data="folderSelectData"
                                :props="{ value: 'id', label: 'name', children: 'children' }"
                                value-key="id"
                                placeholder="选择父级文件夹（不选为根目录）"
                                check-strictly
                                clearable
                                style="width: 100%"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="封面图片">
                            <imageUpload v-model="folderForm.cover" image-type="wallpaper"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="排序" prop="order">
                            <el-input-number v-model="folderForm.order" :min="0" controls-position="right" placeholder="请输入排序"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="备注" prop="remark">
                            <el-input v-model="folderForm.remark" type="textarea" placeholder="请输入备注"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitFolderForm">确 定</el-button>
                    <el-button @click="cancelFolder">取 消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {
    listWallpaper,
    getWallpaper,
    addWallpaper,
    updateWallpaper,
    delWallpaper,
    exportWallpaper,
    listWallpaperFolder,
    getWallpaperFolder,
    addWallpaperFolder,
    updateWallpaperFolder,
    delWallpaperFolder
} from '@/api/px/life/wallpaper'
import ImageUpload from '@/components/ImageUpload/index.vue'
import {Folder, FolderOpened, Link, DataLine} from '@element-plus/icons-vue'

export default {
    name: 'Wallpaper',
    components: {ImageUpload, Folder, FolderOpened, Link, DataLine},
    data() {
        return {
            // 壁纸表格遮罩
            loading: true,
            // 选中数组
            ids: [],
            // 非单个禁用
            single: true,
            // 非多个禁用
            multiple: true,
            // 显示搜索条件
            showSearch: true,
            // 总条数
            total: 0,
            // 壁纸列表
            wallpaperList: [],
            // 文件夹 id -> name 映射
            folderMap: {},
            // 文件夹树（左侧 el-tree 用，含根节点）
            folderTreeData: [],
            // 文件夹树（用于弹窗下拉选择，不含根节点）
            folderSelectData: [],
            // 文件夹树 props：label 由 label 字段读取（含数量）
            treeProps: {label: 'label', children: 'children'},
            // 文件夹搜索关键字
            folderKeyword: '',
            // 当前选中的文件夹节点 key（root 或 folder id），用于过滤右侧表格
            currentFolderKey: 'root',
            // 壁纸弹窗
            title: '',
            open: false,
            // 文件夹弹窗
            folderTitle: '',
            folderOpen: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                name: undefined,
                folder: undefined
            },
            // 壁纸表单
            form: {},
            // 文件夹表单
            folderForm: {},
            // 壁纸校验
            rules: {
                url: [
                    {required: true, message: '壁纸图片不能为空', trigger: 'blur'}
                ]
            },
            // 文件夹校验
            folderRules: {
                name: [
                    {required: true, message: '文件夹名称不能为空', trigger: 'blur'}
                ]
            }
        }
    },
    watch: {
        // 关键字变化时过滤左侧树
        folderKeyword(val) {
            this.$refs.folderTreeRef.filter(val)
        }
    },
    created() {
        this.getFolderTree()
        this.getList()
    },
    methods: {
        /** 跳转到壁纸文件夹管理页（启用/停用等） */
        goToFolderManage() {
            this.$router.push('/wallpaper-folder')
        },
        /** 跳转到壁纸操作记录页 */
        goToRecord() {
            this.$router.push('/wallpaper-record')
        },
        /** 查询壁纸列表 */
        getList() {
            this.loading = true
            const params = {...this.queryParams}
            // 选中具体文件夹节点时按 folder 过滤；选中根节点或未分类节点时不过滤
            if (this.currentFolderKey === 'root') {
                params.folder = undefined
            } else if (this.currentFolderKey === 'unclassified') {
                params.folder = undefined
            } else {
                params.folder = this.currentFolderKey
            }
            listWallpaper(params).then(response => {
                this.wallpaperList = response.rows
                this.total = response.total
                this.loading = false
            })
        },
        /** 获取文件夹树（左侧展示 + 弹窗下拉 + 名称映射） */
        getFolderTree() {
            // 左树仅展示启用文件夹；弹窗下拉/名称映射保留全量（壁纸可归属任意文件夹）
            return Promise.all([
                listWallpaperFolder({pageNum: 1, pageSize: 1000, enabled: 1}),
                listWallpaperFolder({pageNum: 1, pageSize: 1000})
            ]).then(([enabledRes, allRes]) => {
                const enabledList = enabledRes.rows || []
                const allList = allRes.rows || []
                // 组装弹窗下拉树 + 名称映射（全量）
                const map = {}
                allList.forEach(item => {
                    map[item.id] = {id: item.id, name: item.name, parentId: item.parentId, children: []}
                })
                const selectRoots = []
                allList.forEach(item => {
                    const node = map[item.id]
                    if (item.parentId && map[item.parentId]) {
                        map[item.parentId].children.push(node)
                    } else {
                        selectRoots.push(node)
                    }
                })
                this.folderSelectData = selectRoots
                const folderMap = {}
                allList.forEach(item => {
                    folderMap[item.id] = item.name
                })
                this.folderMap = folderMap
                // 组装左侧展示树（仅启用文件夹，含根节点）
                const treeNodes = enabledList.map(item => ({
                    key: item.id,
                    label: item.name,
                    id: item.id,
                    parentId: item.parentId,
                    wallpaperCount: item.wallpaperCount,
                    isRoot: false,
                    children: []
                }))
                const treeMap = {}
                treeNodes.forEach(n => {
                    treeMap[n.id] = n
                })
                const treeRoots = []
                treeNodes.forEach(n => {
                    if (n.parentId && treeMap[n.parentId]) {
                        treeMap[n.parentId].children.push(n)
                    } else {
                        treeRoots.push(n)
                    }
                })
                this.folderTreeData = [
                    {key: 'root', label: '全部壁纸', isRoot: true, children: treeRoots}
                ]
            })
        },
        /** 左侧树过滤 */
        filterFolderNode(value, data) {
            if (!value) return true
            return data.label && data.label.indexOf(value) !== -1
        },
        /** 点击左侧文件夹节点 */
        handleNodeClick(data) {
            this.currentFolderKey = data.key
            this.queryParams.pageNum = 1
            this.getList()
        },
        // ===== 壁纸表单 =====
        /** 判断是否为外链 URL（http/https 开头），用于自动识别图片填写方式 */
        isExternalUrl(url) {
            return typeof url === 'string' && /^https?:\/\//i.test(url.trim())
        },
        cancel() {
            this.open = false
            this.reset()
        },
        reset() {
            this.form = {
                id: undefined,
                name: undefined,
                url: undefined,
                thumbnail: undefined,
                // 图片填写方式：upload 上传 / url 链接
                urlMode: 'upload',
                thumbnailMode: 'upload',
                // 新增壁纸时默认归入当前选中的文件夹
                folder: (this.currentFolderKey !== 'root' && this.currentFolderKey !== 'unclassified')
                    ? this.currentFolderKey : undefined,
                width: undefined,
                height: undefined,
                order: 0,
                remark: undefined
            }
            this.resetForm('form')
        },
        handleQuery() {
            this.queryParams.pageNum = 1
            this.getList()
        },
        resetQuery() {
            this.resetForm('queryForm')
            this.handleQuery()
        },
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.id)
            this.single = selection.length !== 1
            this.multiple = !selection.length
        },
        handleAdd() {
            this.reset()
            this.open = true
            this.title = '添加壁纸'
        },
        handleUpdate(row) {
            this.reset()
            const id = row.id || this.ids[0]
            getWallpaper(id).then(response => {
                this.form = response.data
                // 根据已有图片地址自动判断填写方式：http(s):// 开头视为 URL 链接，否则视为本地上传
                this.form.urlMode = this.isExternalUrl(this.form.url) ? 'url' : 'upload'
                this.form.thumbnailMode = this.isExternalUrl(this.form.thumbnail) ? 'url' : 'upload'
                this.open = true
                this.title = '修改壁纸'
            })
        },
        submitForm() {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    if (this.form.id != null) {
                        updateWallpaper(this.form).then(() => {
                            this.$message.success('修改成功')
                            this.open = false
                            this.getList()
                        })
                    } else {
                        addWallpaper(this.form).then(() => {
                            this.$message.success('新增成功')
                            this.open = false
                            this.getList()
                            this.getFolderTree()
                        })
                    }
                }
            })
        },
        handleDelete(row) {
            const ids = row.id || this.ids
            this.$confirm('是否确认删除选中的壁纸？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                return delWallpaper(ids)
            }).then(() => {
                this.getList()
                this.getFolderTree()
                this.$message.success('删除成功')
            }).catch(() => {
            })
        },
        handleExport() {
            this.download('wallpaper/export', {
                ...this.queryParams
            }, `wallpaper_${new Date().getTime()}.xlsx`)
        },
        // ===== 文件夹表单 =====
        cancelFolder() {
            this.folderOpen = false
            this.resetFolderForm()
        },
        resetFolderForm() {
            this.folderForm = {
                id: undefined,
                name: undefined,
                cover: undefined,
                parentId: 0,
                order: 0,
                remark: undefined
            }
            this.resetForm('folderForm')
        },
        /** 新增文件夹（可指定父级 parentId） */
        handleFolderAdd(parentId) {
            this.resetFolderForm()
            if (parentId) {
                this.folderForm.parentId = parentId
            }
            this.folderOpen = true
            this.folderTitle = '添加壁纸文件夹'
        },
        /** 修改文件夹 */
        handleFolderUpdate(row) {
            this.resetFolderForm()
            const id = row.id
            getWallpaperFolder(id).then(response => {
                this.folderForm = response.data
                if (!this.folderForm.parentId) {
                    this.folderForm.parentId = 0
                }
                this.folderOpen = true
                this.folderTitle = '修改壁纸文件夹'
            })
        },
        submitFolderForm() {
            this.$refs['folderForm'].validate(valid => {
                if (valid) {
                    if (!this.folderForm.parentId) {
                        this.folderForm.parentId = 0
                    }
                    if (this.folderForm.id != null) {
                        updateWallpaperFolder(this.folderForm).then(() => {
                            this.$message.success('修改成功')
                            this.folderOpen = false
                            this.getFolderTree()
                        })
                    } else {
                        addWallpaperFolder(this.folderForm).then(() => {
                            this.$message.success('新增成功')
                            this.folderOpen = false
                            this.getFolderTree()
                        })
                    }
                }
            })
        },
        handleFolderDelete(row) {
            this.$confirm('是否确认删除该壁纸文件夹？删除后文件夹下的壁纸将变为未分类。', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                return delWallpaperFolder(row.id)
            }).then(() => {
                // 删除的若是当前选中节点，回到根节点
                if (this.currentFolderKey === row.id) {
                    this.currentFolderKey = 'root'
                    this.queryParams.pageNum = 1
                    this.getList()
                }
                this.getFolderTree()
                this.$message.success('删除成功')
            }).catch(() => {
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.wallpaper-page {
    .like-count {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        color: #f59e0b;
        font-weight: 600;
    }

    .folder-panel {
        min-width: 220px;
    }

    .image-mode-group {
        margin-bottom: 8px;
    }

    .folder-card {
        height: calc(100vh - 130px);
        display: flex;
        flex-direction: column;

        :deep(.el-card__body) {
            flex: 1;
            overflow: auto;
            padding: 8px;
        }
    }

    .folder-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .folder-title {
            font-weight: 600;
            font-size: 14px;
        }
    }

    .folder-search {
        margin-bottom: 8px;
    }

    .folder-node {
        flex: 1;
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-right: 4px;
        overflow: hidden;

        .folder-node-label {
            display: flex;
            align-items: center;
            gap: 4px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .folder-count {
            color: #909399;
            font-size: 12px;
        }

        .folder-node-actions {
            display: none;
            flex-shrink: 0;
        }

        &:hover .folder-node-actions {
            display: inline-flex;
        }
    }
}
</style>
