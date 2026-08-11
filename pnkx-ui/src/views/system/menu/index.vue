<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
            <el-form-item label="菜单名称" prop="menuName">
                <el-input
                    v-model="queryParams.menuName"
                    placeholder="请输入菜单名称"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="菜单状态" clearable size="small">
                    <el-option
                        v-for="dict in statusOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                    type="primary"
                                        icon="Plus"
                    size="small"
                    @click="handleAdd"
                    v-hasPermi="['system:menu:add']"
                >新增
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <div class="table-main-area">
            <el-table
                v-loading="loading"
                :data="menuList"
                row-key="menuId"
                :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
            >
                <el-table-column prop="menuName" label="菜单名称" :show-overflow-tooltip="true"
                                 width="160"></el-table-column>
                <el-table-column prop="icon" label="图标" align="center" width="100">
                    <template v-slot="scope">
                        <svg-icon :icon-class="scope.row.icon"/>
                    </template>
                </el-table-column>
                <el-table-column prop="orderNum" label="排序" width="60"></el-table-column>
                <el-table-column prop="perms" label="权限标识" :show-overflow-tooltip="true"></el-table-column>
                <el-table-column prop="component" label="组件路径" :show-overflow-tooltip="true"></el-table-column>
                <el-table-column prop="status" label="状态" :formatter="statusFormat" width="80"></el-table-column>
                <el-table-column label="创建时间" align="center" prop="createTime">
                    <template v-slot="scope">
                        <span>{{ parseTime(scope.row.createTime) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                    <template v-slot="scope">
                        <el-button size="small"
                                   type="text"
                                   icon="Edit"
                                   @click="handleUpdate(scope.row)"
                                   v-hasPermi="['system:menu:edit']"
                        >修改
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Plus"
                            @click="handleAdd(scope.row)"
                            v-hasPermi="['system:menu:add']"
                        >新增
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Delete"
                            @click="handleDelete(scope.row)"
                            v-hasPermi="['system:menu:remove']"
                        >删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 添加或修改菜单对话框 -->
        <el-dialog :title="title" v-model="open" width="600px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="80px">
                <el-row>
                    <el-col :span="24">
                        <el-form-item label="上级菜单">
                            <el-tree-select
                                v-model="form.parentId"
                                :data="menuOptions"
                                :render-after-expand="false"
                                :props="{value: 'menuId', label: 'menuName', children: 'children'}"
                                check-strictly
                                placeholder="选择上级菜单"
                                style="width: 100%"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="菜单类型" prop="menuType">
                            <el-radio-group v-model="form.menuType">
                                <el-radio label="M">目录</el-radio>
                                <el-radio label="C">菜单</el-radio>
                                <el-radio label="F">按钮</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item v-if="form.menuType != 'F'" label="菜单图标">
                            <el-popover
                                placement="bottom-start"
                                width="460"
                                trigger="click"
                                @show="$refs['iconSelect'].reset()"
                            >
                                <IconSelect ref="iconSelect" @selected="selected"/>
                                <template #reference>
                                    <el-input v-model="form.icon" placeholder="点击选择图标" readonly>
                                        <template #prefix>
                                            <svg-icon
                                                v-if="form.icon"
                                                :icon-class="form.icon"
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
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="菜单名称" prop="menuName">
                            <el-input v-model="form.menuName" placeholder="请输入菜单名称"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="显示排序" prop="orderNum">
                            <el-input-number v-model="form.orderNum" controls-position="right" :min="0"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item v-if="form.menuType != 'F'" label="是否外链">
                            <el-radio-group v-model="form.isFrame">
                                <el-radio label="0">是</el-radio>
                                <el-radio label="1">否</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item v-if="form.menuType != 'F'" label="路由地址" prop="path">
                            <el-input v-model="form.path" placeholder="请输入路由地址"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12" v-if="form.menuType == 'C'">
                        <el-form-item label="组件路径" prop="component">
                            <el-input v-model="form.component" placeholder="请输入组件路径"/>
                        </el-form-item>
                    </el-col>
                    <el-col v-if="form.menuType == 'C'" :span="12">
                        <el-form-item label="App菜单" prop="component">
                            <el-radio-group v-model="form.isApp">
                                <el-radio label="1">是</el-radio>
                                <el-radio label="0">否</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col v-if="form.menuType == 'C' && form.isApp == '1'" :span="12">
                        <el-form-item label="移动端路由" prop="appPath">
                            <el-input v-model="form.appPath" placeholder="如 /pages_life/reminder/index"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item v-if="form.menuType != 'M'" label="权限标识">
                            <el-input v-model="form.perms" placeholder="请权限标识" maxlength="50"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item v-if="form.menuType != 'F'" label="显示状态">
                            <el-radio-group v-model="form.visible">
                                <el-radio
                                    v-for="dict in visibleOptions"
                                    :key="dict.dictValue"
                                    :label="dict.dictValue"
                                >{{ dict.dictLabel }}
                                </el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item v-if="form.menuType != 'F'" label="菜单状态">
                            <el-radio-group v-model="form.status">
                                <el-radio
                                    v-for="dict in statusOptions"
                                    :key="dict.dictValue"
                                    :label="dict.dictValue"
                                >{{ dict.dictLabel }}
                                </el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item v-if="form.menuType == 'C'" label="是否缓存">
                            <el-radio-group v-model="form.isCache">
                                <el-radio label="0">缓存</el-radio>
                                <el-radio label="1">不缓存</el-radio>
                            </el-radio-group>
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
import {addMenu, delMenu, getMenu, listMenu, updateMenu} from "@/api/system/menu";
import Treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import IconSelect from "@/components/IconSelect";

export default {
    name: "Menu",
    components: {Treeselect, IconSelect},
    data() {
        return {
            // 遮罩层
            loading: true,
            // 显示搜索条件
            showSearch: true,
            // 菜单表格树数据
            menuList: [],
            // 菜单树选项
            menuOptions: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 显示状态数据字典
            visibleOptions: [],
            // 菜单状态数据字典
            statusOptions: [],
            // 查询参数
            queryParams: {
                menuName: undefined,
                visible: undefined
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                menuName: [
                    {required: true, message: "菜单名称不能为空", trigger: "blur"}
                ],
                orderNum: [
                    {required: true, message: "菜单顺序不能为空", trigger: "blur"}
                ],
                path: [
                    {required: true, message: "路由地址不能为空", trigger: "blur"}
                ]
            }
        };
    },
    created() {
        this.getList();
        this.getDicts("sys_show_hide").then(response => {
            this.visibleOptions = response.data;
        });
        this.getDicts("sys_normal_disable").then(response => {
            this.statusOptions = response.data;
        });
    },
    methods: {
        // 选择图标
        selected(name) {
            this.form.icon = name;
        },
        /** 查询菜单列表 */
        getList() {
            this.loading = true;
            listMenu(this.queryParams).then(response => {
                this.menuList = this.handleTree(response.data, "menuId");
                this.loading = false;
            });
        },
        /** 转换菜单数据结构 */
        normalizer(node) {
            if (node.children && !node.children.length) {
                delete node.children;
            }
            return {
                id: node.menuId,
                label: node.menuName,
                children: node.children
            };
        },
        /** 查询菜单下拉树结构 */
        getTreeselect() {
            listMenu().then(response => {
                this.menuOptions = [];
                const menu = {menuId: 0, menuName: '主类目', children: []};
                menu.children = this.handleTree(response.data, "menuId");
                this.menuOptions.push(menu);
            });
        },
        // 显示状态字典翻译
        visibleFormat(row, column) {
            if (row.menuType == "F") {
                return "";
            }
            return this.selectDictLabel(this.visibleOptions, row.visible);
        },
        // 菜单状态字典翻译
        statusFormat(row, column) {
            if (row.menuType == "F") {
                return "";
            }
            return this.selectDictLabel(this.statusOptions, row.status);
        },
        // 取消按钮
        cancel() {
            this.open = false;
            this.reset();
        },
        // 表单重置
        reset() {
            this.form = {
                menuId: undefined,
                parentId: 0,
                menuName: undefined,
                icon: undefined,
                menuType: "M",
                orderNum: undefined,
                isFrame: "1",
                isCache: "0",
                isApp: "0",
                appPath: undefined,
                visible: "0",
                status: "0"
            };
            this.resetForm("form");
        },
        /** 搜索按钮操作 */
        handleQuery() {
            this.getList();
        },
        /** 重置按钮操作 */
        resetQuery() {
            this.resetForm("queryForm");
            this.handleQuery();
        },
        /** 新增按钮操作 */
        handleAdd(row) {
            this.reset();
            this.getTreeselect();
            if (row != null && row.menuId) {
                this.form.parentId = row.menuId;
            } else {
                this.form.parentId = 0;
            }
            this.open = true;
            this.title = "添加菜单";
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            this.getTreeselect();
            getMenu(row.menuId).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改菜单";
            }).catch(err => {
                console.error('getMenu error:', err);
            });
        },
        /** 提交按钮 */
        submitForm: function () {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.menuId != undefined) {
                        updateMenu(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addMenu(this.form).then(response => {
                            this.msgSuccess("新增成功");
                            this.open = false;
                            this.getList();
                        });
                    }
                }
            });
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            this.$confirm('是否确认删除名称为"' + row.menuName + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delMenu(row.menuId);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        }
    }
};
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.app-container {
    @include adaptive-table-layout(130px);
    background: var(--bg-body);
}

::v-deep .el-form--inline .el-form-item {
    margin-right: var(--space-4);
}

::v-deep .el-table {
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    overflow: hidden;
    box-shadow: var(--shadow-sm);
    border: 1px solid var(--border-primary);

    th {
        background: var(--bg-hover);
        color: var(--text-primary);
        font-weight: var(--font-semibold);
        font-size: var(--text-sm);
    }

    td {
        color: var(--text-secondary);
        font-size: var(--text-sm);
    }

    .el-table__row:hover > td {
        background: var(--bg-hover);
        transition: background var(--duration-fast) var(--ease-default);
    }
}

::v-deep .el-table::before {
    display: none;
}

::v-deep .el-dialog {
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-lg);

    .el-dialog__header {
        border-bottom: 1px solid var(--border-primary);
        padding: var(--space-4) var(--space-6);
    }

    .el-dialog__body {
        padding: var(--space-6);
    }

    .el-dialog__footer {
        border-top: 1px solid var(--border-primary);
        padding: var(--space-4) var(--space-6);
    }
}

::v-deep .el-button {
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
    transition: all var(--duration-fast) var(--ease-default);

    &:hover {
        transform: translateY(-1px);
        box-shadow: var(--shadow-sm);
    }
}

::v-deep .el-input__inner,
::v-deep .el-textarea__inner {
    border-radius: var(--radius-md);
    border-color: var(--border-primary);
    transition: border-color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default);

    &:focus {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 3px rgba(var(--color-primary), 0.1);
    }
}

.mb8 {
    margin-bottom: var(--space-2);
}
</style>
