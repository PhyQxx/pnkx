<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="群组名称" prop="groupName">
                <el-input
                    v-model="queryParams.groupName"
                    placeholder="请输入群组名称"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="群组状态" clearable size="small">
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
                    v-hasPermi="['system:dataGroup:add']"
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
                    v-hasPermi="['system:dataGroup:edit']"
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
                    v-hasPermi="['system:dataGroup:remove']"
                >删除
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="dataGroupList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center"/>
            <el-table-column label="群组编号" align="center" prop="id"/>
            <el-table-column label="群组名称" align="center" prop="groupName"/>
            <el-table-column label="群组编码" align="center" prop="groupCode"/>
            <el-table-column label="状态" align="center" prop="status" :formatter="statusFormat"/>
            <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true"/>
            <el-table-column label="创建时间" align="center" prop="createTime" width="180">
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
                        v-hasPermi="['system:dataGroup:edit']"
                    >修改
                    </el-button>
                    <el-button
                        size="small"
                        type="text"
                        icon="Delete"
                        @click="handleDelete(scope.row)"
                        v-hasPermi="['system:dataGroup:remove']"
                    >删除
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total>0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <!-- 添加或修改数据权限群组对话框 -->
        <el-dialog :title="title" v-model="open" width="600px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="群组名称" prop="groupName">
                    <el-input v-model="form.groupName" placeholder="请输入群组名称"/>
                </el-form-item>
                <el-form-item label="群组编码" prop="groupCode">
                    <el-input v-model="form.groupCode" placeholder="请输入群组编码"/>
                </el-form-item>
                <el-form-item label="群组成员" prop="userIds">
                    <el-select
                        v-model="form.userIds"
                        multiple
                        filterable
                        placeholder="请选择群组成员（成员间数据互见）"
                        style="width: 100%"
                    >
                        <el-option
                            v-for="u in userOptions"
                            :key="u.userId"
                            :label="u.userName + '（' + (u.nickName || u.userName) + '）'"
                            :value="u.userId"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio
                            v-for="dict in statusOptions"
                            :key="dict.dictValue"
                            :label="dict.dictValue"
                        >{{ dict.dictLabel }}
                        </el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
                </el-form-item>
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
    listDataGroup,
    getDataGroup,
    addDataGroup,
    updateDataGroup,
    delDataGroup
} from "@/api/system/dataGroup";
import {listUser} from "@/api/system/user";

export default {
    name: "DataGroup",
    data() {
        return {
            // 遮罩层
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
            // 群组表格数据
            dataGroupList: [],
            // 可选用户列表
            userOptions: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 状态数据字典
            statusOptions: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                groupName: undefined,
                status: undefined
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                groupName: [
                    {required: true, message: "群组名称不能为空", trigger: "blur"}
                ]
            }
        };
    },
    created() {
        this.getList();
        this.getDicts("sys_normal_disable").then(response => {
            this.statusOptions = response.data;
        });
        this.getUserOptions();
    },
    methods: {
        /** 查询群组列表 */
        getList() {
            this.loading = true;
            listDataGroup(this.queryParams).then(response => {
                this.dataGroupList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        /** 查询可选用户列表 */
        getUserOptions() {
            listUser({pageNum: 1, pageSize: 1000}).then(response => {
                this.userOptions = response.rows;
            });
        },
        // 状态字典翻译
        statusFormat(row, column) {
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
                id: undefined,
                groupName: undefined,
                groupCode: undefined,
                userIds: [],
                status: "0",
                remark: undefined
            };
            this.resetForm("form");
        },
        /** 搜索按钮操作 */
        handleQuery() {
            this.queryParams.pageNum = 1;
            this.getList();
        },
        /** 重置按钮操作 */
        resetQuery() {
            this.resetForm("queryForm");
            this.handleQuery();
        },
        // 多选框选中数据
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.id)
            this.single = selection.length != 1
            this.multiple = !selection.length
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset();
            this.open = true;
            this.title = "添加数据权限群组";
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids
            getDataGroup(id).then(response => {
                this.form = response.data;
                if (!this.form.userIds) {
                    this.form.userIds = [];
                }
                this.open = true;
                this.title = "修改数据权限群组";
            });
        },
        /** 提交按钮 */
        submitForm: function () {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.id != undefined) {
                        updateDataGroup(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addDataGroup(this.form).then(response => {
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
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除数据权限群组编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delDataGroup(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        }
    }
};
</script>

<style lang="scss" scoped>
.app-container {
    padding: var(--space-6);
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-2);
}

::v-deep .el-table {
    border-radius: var(--radius-lg);
    overflow: hidden;

    th {
        background: var(--bg-body);
        color: var(--text-primary);
        font-weight: var(--font-semibold);
        font-size: var(--text-sm);
    }

    td {
        color: var(--text-secondary);
        font-size: var(--text-sm);
    }

    .el-button--text {
        transition: color var(--duration-fast) var(--ease-default);
    }
}

::v-deep .el-dialog {
    border-radius: var(--radius-lg);

    .el-dialog__header {
        border-bottom: 1px solid var(--border-primary);
        padding: var(--space-4) var(--space-6);
    }

    .el-dialog__body {
        padding: var(--space-6);
    }
}
</style>
