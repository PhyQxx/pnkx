<!--
 * @File: classification
 * @Author: PHY
 * @Date: 2021-11-05 21:06
 * @Description: 分类管理 - Modern UI Refactored
-->
<template>
    <div class="bookkeeping-classification-container">
        <!-- 左侧列表面板 -->
        <classification-sidebar
            v-model="searchCode"
            :type-difference="addTypeForm.typeDifference"
            :list="filteredList"
            :list-loading="listLoading"
            :active-id="active && active.id"
            @change-type="changeSelect"
            @select="handleSelect"
            @contextmenu="handleContextMenu"
            @add-child="openAddType"
        />

        <!-- 右侧详情面板 -->
        <classification-detail-panel
            :active="active"
            :type-difference="addTypeForm.typeDifference"
            :loading="loading"
            @edit="handleEdit"
            @delete="handleDeleteFromDetail"
        />

        <!-- 浮动新增按钮 -->
        <div class="fab-action" title="新增一级分类" @click="handleAddParent">
            <el-icon>
                <Plus/>
            </el-icon>
        </div>

        <!-- 新增/编辑分类对话框 -->
        <el-dialog
            :title="typeTitle"
            v-model="addType"
            width="520px"
            custom-class="modern-dialog"
            :modal-append-to-body="true"
            @closed="clearAddData"
        >
            <el-form
                ref="typeForm"
                :model="addTypeForm"
                :rules="typeRules"
                label-position="top"
                class="modern-form"
            >
                <el-form-item label="分类名称" prop="typeName">
                    <el-input v-model="addTypeForm.typeName" placeholder="请输入分类名称"/>
                </el-form-item>
                <el-form-item label="分类图标">
                    <el-popover
                        placement="bottom-start"
                        width="460"
                        trigger="click"
                        @show="$refs['iconSelect'].reset()"
                    >
                        <icon-select ref="iconSelect" prefix="c-" @selected="selected"/>
                        <template #reference>
                            <el-input
                                :value="addTypeForm.typeIcon ? addTypeForm.typeIcon.slice(2) : ''"
                                placeholder="点击选择分类图标"
                                readonly
                            >
                                <template #prefix>
                                    <svg-icon
                                        v-if="addTypeForm.typeIcon"
                                        :icon-class="addTypeForm.typeIcon"
                                        class="el-input__icon"
                                        style="height: 32px;width: 16px;"
                                    />
                                    <el-icon v-else>
                                        <Search/>
                                    </el-icon>
                                </template>
                            </el-input>
                        </template>
                    </el-popover>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="addTypeForm.remark" placeholder="请输入备注"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button class="btn-cancel" @click="addType = false">取消</el-button>
                    <el-button type="primary" class="btn-confirm" @click="addOrUpdateClassification">确定</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 右键菜单 -->
        <transition name="context-menu">
            <div
                v-if="contextMenuVisible"
                v-clickOutSide="closeContextMenu"
                class="context-menu"
                :style="contextMenuStyle"
            >
                <div
                    v-for="item in contextMenuItems"
                    :key="item.id"
                    class="menu-item"
                    @click="handleContextAction(item)"
                >
                    <svg-icon :icon-class="item.icon" class="menu-icon"/>
                    <span>{{ item.name }}</span>
                </div>
            </div>
        </transition>
    </div>
</template>

<script>
import IconSelect from '@/components/IconSelect/index.vue'
import {
    addClassification,
    delClassification,
    getClassification,
    listClassification,
    updateClassification
} from '@/api/px/life/bookkeeping/classification'
import ClassificationSidebar from './components/ClassificationSidebar.vue'
import ClassificationDetailPanel from './components/ClassificationDetailPanel.vue'

export default {
    name: 'Classification',
    components: {IconSelect, ClassificationSidebar, ClassificationDetailPanel},
    data() {
        return {
            // 加载标志
            listLoading: false,
            loading: false,
            // 搜索关键字
            searchCode: '',
            // 分类树形数据
            classificationData: [],
            // 当前选中
            active: null,
            // 弹窗标志
            addType: false,
            // 弹窗标题
            typeTitle: '添加一级分类',
            // 表单数据
            addTypeForm: {
                typeName: '',
                typeParentId: '',
                typeDifference: '1',
                typeLevel: '0',
                remark: '',
                typeIcon: 'c-早午晚餐'
            },
            // 校验规则
            typeRules: {
                typeName: {required: true, message: '请输入分类名称', trigger: 'blur'}
            },
            // 右键菜单
            contextMenuVisible: false,
            contextMenuStyle: '',
            contextMenuItems: [],
            contextMenuTarget: null
        }
    },
    computed: {
        filteredList() {
            if (!this.searchCode) return this.classificationData
            const keyword = this.searchCode.toLowerCase()
            return this.classificationData.filter(parent => {
                const parentMatch = parent.typeName && parent.typeName.toLowerCase().includes(keyword)
                const childMatch = parent.children && parent.children.some(child =>
                    child.typeName && child.typeName.toLowerCase().includes(keyword)
                )
                return parentMatch || childMatch
            })
        }
    },
    mounted() {
        this.listClassification()
    },
    methods: {
        /**
         * 切换选中分类
         */
        changeSelect(index) {
            this.addTypeForm.typeDifference = index
            this.active = null
            this.listClassification()
        },
        /**
         * 获取分类数据
         */
        listClassification() {
            this.listLoading = true
            // 分类管理需要完整的一级+二级分类来构建树，分页接口默认只返回首页，
            // 这里一次性拉取全部数据，避免分类缺失。
            listClassification({
                typeDifference: this.addTypeForm.typeDifference,
                pageNum: 1,
                pageSize: 9999
            }).then(res => {
                this.setListToTree(res.rows)
                this.listLoading = false
            }).catch(() => {
                this.listLoading = false
            })
        },
        /**
         * 将获取的列表数据处理为树结构
         */
        setListToTree(list) {
            const tree = list.filter(item => {
                return item.typeLevel === '0'
            })
            list.forEach(item => {
                if (item.typeParentId) {
                    tree.forEach(one => {
                        if (one.id === item.typeParentId) {
                            if (!one.children) {
                                one.children = []
                            }
                            one.children.push(item)
                        }
                    })
                }
            })
            tree.forEach(item => {
                if (!item.children) {
                    item.children = []
                }
                item.children.push({
                    id: Math.random(),
                    type: 'menu',
                    typeParentId: item.id,
                    typeLevel: '1',
                    typeName: '+ 添加二级分类'
                })
            })
            this.classificationData = tree
        },
        /**
         * 选中分类
         */
        handleSelect(item) {
            this.active = item
        },
        /**
         * 选择图标
         */
        selected(name) {
            this.addTypeForm.typeIcon = name
        },
        /**
         * 打开新增一级分类
         */
        handleAddParent() {
            this.addTypeForm = {
                typeName: '',
                typeParentId: '',
                typeDifference: this.addTypeForm.typeDifference,
                typeLevel: '0',
                remark: '',
                typeIcon: 'c-早午晚餐'
            }
            this.typeTitle = '添加一级分类'
            this.addType = true
        },
        /**
         * 打开新增/修改分类弹窗
         */
        openAddType(row) {
            if (this.addTypeForm.id) {
                this.typeTitle = row.typeLevel === '0' ? '修改一级分类' : '修改二级分类'
            } else {
                this.addTypeForm.typeLevel = row.typeLevel
                this.typeTitle = row.typeLevel === '0' ? '添加一级分类' : '添加二级分类'
                this.addTypeForm.typeParentId = row.typeLevel === '0' ? '' : row.typeParentId
            }
            this.addType = true
        },
        /**
         * 编辑（从详情面板或右键菜单）
         */
        handleEdit() {
            if (!this.active) return
            this.loading = true
            getClassification(this.active.id).then(res => {
                if (res.code === 200) {
                    this.addTypeForm = res.data
                    this.typeTitle = res.data.typeLevel === '0' ? '修改一级分类' : '修改二级分类'
                    this.addType = true
                    this.loading = false
                }
            })
        },
        /**
         * 删除（从详情面板或右键菜单）
         */
        handleDeleteFromDetail() {
            if (!this.active) return
            this.$confirm('确认删除当前分类吗？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消'
            }).then(() => {
                return delClassification(this.active.id)
            }).then(() => {
                this.$notify.success('删除成功')
                this.active = null
                this.listClassification()
            }).catch(() => {
            })
        },
        /**
         * 关闭弹窗时清除数据
         */
        clearAddData() {
            this.addTypeForm = {
                typeName: '',
                typeParentId: '',
                typeDifference: this.addTypeForm.typeDifference,
                typeLevel: '0',
                remark: '',
                typeIcon: 'c-早午晚餐'
            }
            this.typeTitle = '添加一级分类'
        },
        /**
         * 新增/修改分类弹窗保存
         */
        addOrUpdateClassification() {
            this.$refs.typeForm.validate(valid => {
                if (valid) {
                    if (this.addTypeForm.id) {
                        this.updateClassification()
                    } else {
                        this.addClassification()
                    }
                }
            })
        },
        /**
         * 新增分类保存
         */
        addClassification() {
            addClassification(this.addTypeForm).then(res => {
                if (res.code === 200) {
                    this.addType = false
                    this.$notify.success('新增分类成功')
                    this.listClassification()
                }
            })
        },
        /**
         * 修改分类保存
         */
        updateClassification() {
            updateClassification(this.addTypeForm).then(res => {
                if (res.code === 200) {
                    this.addType = false
                    this.$notify.success('修改分类成功')
                    this.listClassification()
                }
            })
        },
        /**
         * 右键菜单
         */
        handleContextMenu(event, item) {
            if (item) {
                this.active = item
                this.contextMenuTarget = item
                this.contextMenuItems = [
                    {id: 1, name: '编辑', icon: '编辑'},
                    {id: 2, name: '删除', icon: '删除'}
                ]
            } else {
                this.contextMenuItems = [
                    {id: 3, name: '新增一级分类', icon: '编辑02'}
                ]
            }
            this.contextMenuVisible = true
            this.contextMenuStyle = `top: ${Math.min(event.y, window.innerHeight - this.contextMenuItems.length * 48)}px; left: ${Math.min(event.x - 180, window.innerWidth - 200)}px;`
        },
        /**
         * 关闭右键菜单
         */
        closeContextMenu() {
            this.contextMenuVisible = false
        },
        /**
         * 右键菜单操作
         */
        handleContextAction(item) {
            this.contextMenuVisible = false
            switch (item.id) {
                case 1:
                    this.handleEdit()
                    break
                case 2:
                    this.handleDeleteFromDetail()
                    break
                case 3:
                    this.handleAddParent()
                    break
            }
        }
    }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/design-tokens.scss';

$bk-red: $theme-bookkeeping-red;
$bk-green: $theme-bookkeeping-green;

.bookkeeping-classification-container {
    display: flex;
    height: 100%;
    background: var(--bg-body);
    font-family: var(--font-family-base);
}

// FAB
.fab-action {
    position: fixed;
    right: var(--space-8);
    bottom: var(--space-8);
    height: 56px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 20px rgba(14, 165, 233, 0.4);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    z-index: 100;

    i {
        font-size: var(--text-3xl);
    }

    &:hover {
        transform: scale(1.1) rotate(90deg);
        box-shadow: 0 6px 28px rgba(14, 165, 233, 0.5);
    }

    &:active {
        transform: scale(0.95);
    }
}

// 右键菜单
.context-menu {
    position: fixed;
    background: var(--bg-card);
    border-radius: var(--radius-lg);
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
        font-size: var(--text-base);
        color: var(--text-primary);
        cursor: pointer;
        transition: all var(--duration-fast) var(--ease-default);

        .menu-icon {
            font-size: var(--text-lg);
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

.context-menu-enter-active,
.context-menu-leave-active {
    transition: all 0.2s ease;
}

.context-menu-enter,
.context-menu-leave-to {
    opacity: 0;
    transform: scale(0.95) translateY(-8px);
}

// 对话框样式
::v-deep .modern-dialog {
    border-radius: var(--radius-xl) !important;
    overflow: hidden;
    box-shadow: var(--shadow-xl) !important;

    .el-dialog__header {
        padding: var(--space-5) var(--space-6) var(--space-4);
        border-bottom: 1px solid var(--border-primary);
        background: var(--bg-card);

        .el-dialog__title {
            font-size: var(--text-xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
        }
    }

    .el-dialog__body {
        padding: var(--space-6);
    }

    .el-dialog__footer {
        padding: var(--space-4) var(--space-6) var(--space-5);
        border-top: 1px solid var(--border-primary);
        background: var(--bg-body);
    }
}

.modern-form {
    ::v-deep .el-form-item {
        margin-bottom: var(--space-5);

        .el-form-item__label {
            font-size: var(--text-sm);
            font-weight: var(--font-medium);
            color: var(--text-secondary);
            padding-bottom: var(--space-2);
        }

        .el-input__inner {
            border-radius: var(--radius-sm);
            border-color: var(--border-primary);
            transition: all var(--duration-normal) var(--ease-default);

            &:focus {
                border-color: var(--color-primary);
                box-shadow: 0 0 0 3px var(--color-primary-100);
            }
        }
    }
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);

    .btn-cancel {
        border-radius: var(--radius-sm);
        padding: 10px var(--space-5);
        transition: all var(--duration-normal) var(--ease-default);

        &:hover {
            background: var(--bg-hover);
        }
    }

    .btn-confirm {
        border-radius: var(--radius-sm);
        padding: 10px var(--space-6);
        background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
        border: none;
        transition: all var(--duration-normal) var(--ease-default);

        &:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }
    }
}

// Loading 美化
::v-deep .el-loading-mask {
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(4px);
}

// 通知美化
::v-deep .el-notification {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    border: none;
}
</style>
