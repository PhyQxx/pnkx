<!--
 * @File: folder
 * @Author: PHY
 * @Description: 壁纸文件夹管理（独立页面，不在左侧菜单展示）
 *               从壁纸管理页左侧文件夹树顶部跳转进入。
 *               采用树形表格展示文件夹层级，停用的文件夹及其下属全部壁纸在移动端不展示。
-->
<template>
    <div class="app-container wallpaper-folder-page">
        <!-- 顶部：返回 + 标题 -->
        <div class="page-header">
            <el-button icon="Back" size="small" link @click="goBack">返回壁纸管理</el-button>
            <span class="page-title">壁纸文件夹管理</span>
            <span class="page-tip">停用的文件夹在移动端不会展示（含其下属全部壁纸）</span>
        </div>

        <!-- 查询条件 -->
        <el-form
            :model="queryParams"
            ref="queryForm"
            :inline="true"
            v-show="showSearch"
            label-width="88px"
        >
            <el-form-item label="文件夹名称" prop="name">
                <el-input
                    v-model="queryParams.name"
                    placeholder="请输入文件夹名称"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="启用状态" prop="enabled">
                <el-select v-model="queryParams.enabled" placeholder="全部" clearable size="small" style="width: 120px">
                    <el-option label="启用" :value="1"/>
                    <el-option label="未启用" :value="0"/>
                </el-select>
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
                    v-hasPermi="['wallpaper:folder:add']"
                >新增
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <!-- 树形表格 -->
        <div class="table-main-area">
            <el-table
                v-loading="loading"
                :data="folderList"
                row-key="id"
                height="100%"
                default-expand-all
                :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
            >
                <el-table-column label="文件夹名称" prop="name" :show-overflow-tooltip="true" width="240"/>
                <el-table-column label="封面" align="center" width="100">
                    <template v-slot="scope">
                        <el-image
                            v-if="scope.row.cover"
                            :src="scope.row.cover"
                            :preview-src-list="[scope.row.cover]"
                            fit="cover"
                            style="width: 60px; height: 40px; border-radius: 4px"
                            preview-teleported
                        />
                        <span v-else style="color: #999">-</span>
                    </template>
                </el-table-column>
                <el-table-column label="壁纸数量" align="center" prop="wallpaperCount" width="100"/>
                <el-table-column label="排序" align="center" prop="order" width="80"/>
                <el-table-column label="启用" align="center" width="100">
                    <template v-slot="scope">
                        <el-switch
                            v-model="scope.row.enabled"
                            :active-value="1"
                            :inactive-value="0"
                            @change="handleEnabledChange(scope.row)"
                            v-hasPermi="['wallpaper:folder:edit']"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="创建时间" align="center" prop="createTime" width="160">
                    <template v-slot="scope">
                        <span>{{ parseTime(scope.row.createTime) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                    <template v-slot="scope">
                        <el-button
                            size="small"
                            type="text"
                            icon="Edit"
                            @click="handleUpdate(scope.row)"
                            v-hasPermi="['wallpaper:folder:edit']"
                        >修改
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Plus"
                            @click="handleAdd(scope.row)"
                            v-hasPermi="['wallpaper:folder:add']"
                        >新增
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Delete"
                            @click="handleDelete(scope.row)"
                            v-hasPermi="['wallpaper:folder:remove']"
                        >删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 新增/修改文件夹对话框 -->
        <el-dialog :title="title" v-model="open" width="600px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="100px">
                <el-row>
                    <el-col :span="24">
                        <el-form-item label="文件夹名称" prop="name">
                            <el-input v-model="form.name" placeholder="请输入文件夹名称"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="父级文件夹" prop="parentId">
                            <el-tree-select
                                v-model="form.parentId"
                                :data="folderSelectTree"
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
                            <imageUpload v-model="form.cover" image-type="wallpaper"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="排序" prop="order">
                            <el-input-number v-model="form.order" :min="0" controls-position="right"
                                             placeholder="请输入排序"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="启用" prop="enabled">
                            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0"/>
                            <span class="enable-hint">{{ form.enabled === 1 ? '移动端可见' : '移动端隐藏' }}</span>
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
    </div>
</template>

<script>
import {
    listWallpaperFolder,
    getWallpaperFolder,
    addWallpaperFolder,
    updateWallpaperFolder,
    delWallpaperFolder
} from '@/api/px/life/wallpaper'
import ImageUpload from '@/components/ImageUpload/index.vue'

export default {
    name: 'WallpaperFolder',
    components: {ImageUpload},
    data() {
        return {
            // 遮罩
            loading: true,
            // 显示搜索条件
            showSearch: true,
            // 树形表格数据（由 handleTree 转换后的嵌套结构）
            folderList: [],
            // 全量扁平列表（构建父级下拉、排除自身及后代用）
            flatList: [],
            // 父级文件夹下拉树
            folderSelectTree: [],
            // 弹窗
            title: '',
            open: false,
            // 查询参数
            queryParams: {
                name: undefined,
                enabled: undefined
            },
            // 表单
            form: {},
            // 校验
            rules: {
                name: [
                    {required: true, message: '文件夹名称不能为空', trigger: 'blur'}
                ]
            }
        }
    },
    created() {
        this.getList()
    },
    methods: {
        /** 返回壁纸管理页 */
        goBack() {
            this.$router.back()
        },
        /** 查询文件夹列表（全量，转树形展示） */
        getList() {
            this.loading = true
            listWallpaperFolder(this.queryParams).then(response => {
                // /wallpaper/folder/list 返回 TableDataInfo，数据在 rows（非 AjaxResult.data）
                this.flatList = response.rows || []
                this.folderList = this.handleTree(this.flatList, 'id')
                this.loading = false
            }).catch(() => {
                this.loading = false
            })
        },
        /**
         * 基于全量扁平列表构建父级下拉树。
         * @param excludeId 需要排除的文件夹 id（编辑时传当前 id），
         *                  连同其全部后代一并排除，避免把父级设成自己的后代而形成环。
         */
        buildSelectTree(excludeId) {
            if (!excludeId) {
                this.folderSelectTree = this.handleTree(this.flatList, 'id')
                return
            }
            // 递归收集需排除的 id（自身 + 所有后代）
            const excludeSet = new Set()
            const collect = id => {
                excludeSet.add(id)
                this.flatList.forEach(item => {
                    if (item.parentId === id) {
                        collect(item.id)
                    }
                })
            }
            collect(excludeId)
            const filtered = this.flatList.filter(item => !excludeSet.has(item.id))
            this.folderSelectTree = this.handleTree(filtered, 'id')
        },
        handleQuery() {
            this.getList()
        },
        resetQuery() {
            this.resetForm('queryForm')
            this.handleQuery()
        },
        cancel() {
            this.open = false
            this.reset()
        },
        reset() {
            this.form = {
                id: undefined,
                name: undefined,
                cover: undefined,
                parentId: 0,
                order: 0,
                enabled: 1,
                remark: undefined
            }
            this.resetForm('form')
        },
        /** 新增文件夹（行内触发时以当前行为父级） */
        handleAdd(row) {
            this.reset()
            if (row != undefined) {
                this.form.parentId = row.id
            }
            this.buildSelectTree(null)
            this.open = true
            this.title = '添加壁纸文件夹'
        },
        handleUpdate(row) {
            this.reset()
            getWallpaperFolder(row.id).then(response => {
                this.form = response.data
                if (!this.form.parentId) {
                    this.form.parentId = 0
                }
                if (this.form.enabled == null) {
                    this.form.enabled = 1
                }
                // 排除自身及后代，避免形成环
                this.buildSelectTree(row.id)
                this.open = true
                this.title = '修改壁纸文件夹'
            })
        },
        submitForm() {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    if (!this.form.parentId) {
                        this.form.parentId = 0
                    }
                    if (this.form.id != null) {
                        updateWallpaperFolder(this.form).then(() => {
                            this.$message.success('修改成功')
                            this.open = false
                            this.getList()
                        })
                    } else {
                        addWallpaperFolder(this.form).then(() => {
                            this.$message.success('新增成功')
                            this.open = false
                            this.getList()
                        })
                    }
                }
            })
        },
        /** 启用/停用切换 */
        handleEnabledChange(row) {
            const text = row.enabled === 1 ? '启用' : '停用'
            updateWallpaperFolder({
                id: row.id,
                enabled: row.enabled,
                // 保留排序/父级等关键字段，避免 update 动态 SQL 误置空
                name: row.name,
                parentId: row.parentId,
                order: row.order,
                cover: row.cover
            }).then(() => {
                this.$message.success(`${text}成功`)
            }).catch(() => {
                // 失败回滚开关状态
                row.enabled = row.enabled === 1 ? 0 : 1
            })
        },
        handleDelete(row) {
            this.$confirm('是否确认删除选中的壁纸文件夹？删除后文件夹下的壁纸将变为未分类。', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                return delWallpaperFolder(row.id)
            }).then(() => {
                this.getList()
                this.$message.success('删除成功')
            }).catch(() => {
            })
        }
    }
}
</script>

<style lang="scss" scoped>
// 弹性铺满整个可视区域：页面无滚动条，表格区域弹性填充并内部滚动
// 高度 = 视口 - 顶部导航栏(86px) - app-container 上下内边距(48px)
.wallpaper-folder-page {
    display: flex;
    flex-direction: column;
    height: calc(100vh - 86px - 48px);
    overflow: hidden;

    .page-header {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        .page-title {
            font-size: 16px;
            font-weight: 600;
            color: #303133;
        }

        .page-tip {
            font-size: 12px;
            color: #909399;
        }
    }

    // 查询条件 / 操作按钮：固定高度，不参与伸缩
    > .el-form,
    > .mb8 {
        flex-shrink: 0;
    }

    // 表格区域：弹性填充剩余空间
    .table-main-area {
        flex: 1;
        min-height: 0;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border);
        border-radius: var(--pnkx-radius-md);
        box-shadow: var(--pnkx-shadow-1);
        overflow: hidden;

        // el-table 撑满容器并内部滚动
        :deep(.el-table) {
            height: 100%;
            margin-bottom: 0;
            border: none;
        }
    }

    .enable-hint {
        margin-left: 10px;
        font-size: 12px;
        color: #909399;
    }
}
</style>
